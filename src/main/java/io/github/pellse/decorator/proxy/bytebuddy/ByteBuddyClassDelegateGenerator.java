/**
 * Copyright 2016 Sebastien Pelletier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pellse.decorator.proxy.bytebuddy;

import static io.github.pellse.decorator.util.reflection.ReflectionUtils.isAbstract;
import static io.github.pellse.decorator.util.reflection.ReflectionUtils.newInstance;
import static io.github.pellse.decorator.util.reflection.ReflectionUtils.setField;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isGetter;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jctools.maps.NonBlockingHashMap;

import io.github.pellse.decorator.DelegateProvider;
import io.github.pellse.decorator.aop.DelegateInvocationHandler;
import io.github.pellse.decorator.proxy.DelegateGenerator;
import javaslang.control.Option;
import javaslang.control.Try;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodCall;

public class ByteBuddyClassDelegateGenerator<I> implements DelegateGenerator<I> {

	private static final String DELEGATE_FIELD_NAME = "delegate";

	// TODO: Composite key ClassLoader/GeneratedType
	private static final Map<Class<?>, Class<?>> CACHE = new NonBlockingHashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public <D extends I, T extends I> D generateDelegate(T delegateTarget,
			Class<D> generatedType,
			Class<I> commonDelegateType,
			BiFunction<Class<D>, T, D> instanceCreator,
			ClassLoader classLoader) {

		Function<? super Class<?>, ? extends Class<?>> classGenerator = clazz -> generateDelegate(delegateTarget,
					(Class<D>) clazz,
					commonDelegateType,
					builder -> builder.method(isAbstract().and(not(isDeclaredBy(DelegateProvider.class))))
						.intercept(MethodCall.invokeSelf().onField(DELEGATE_FIELD_NAME).withAllArguments()),
					classLoader);

		D generatedInstance = instanceCreator.apply(
			isAbstract(generatedType) ? (Class<D>) CACHE.computeIfAbsent(generatedType, classGenerator) : generatedType,
			delegateTarget);

		if (generatedInstance.getClass() != generatedType)
			setField(generatedInstance, Try.of(() -> generatedInstance.getClass().getDeclaredField(DELEGATE_FIELD_NAME)).get(), delegateTarget);

		return generatedInstance;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <D extends I, T extends I> D generateDelegate(T delegateTarget,
			DelegateInvocationHandler<I> handler,
			Class<D> generatedType,
			Class<I> commonDelegateType,
			ClassLoader classLoader) {

		if (generatedType.isInterface())
			return (D)Proxy.newProxyInstance(
				classLoader,
				new Class<?>[] {generatedType},
				(proxy, method, args) -> handler.invoke(delegateTarget, method, args));

		// TODO: Implement caching of generated proxy when proxying a class instead of an interface
		// This should be a very infrequent case, so not urgent to fix
		Class<D> delegateClass = generateDelegate(delegateTarget,
				generatedType,
				commonDelegateType,
				builder -> builder.method(not(isDeclaredBy(Object.class)))
					.intercept(InvocationHandlerAdapter.of((proxy, method, args) -> handler.invoke(delegateTarget, method, args))),
				classLoader);

		return newInstance(delegateClass);
	}

	@SuppressWarnings("unchecked")
	static <I, D extends I, T extends I> Class<D> generateDelegate(T delegateTarget,
			Class<D> generatedType,
			Class<I> commonDelegateType,
			Function<Builder<?>, ReceiverTypeDefinition<?>> interceptStrategy,
			ClassLoader classLoader) {

		Function<ByteBuddy, Builder<?>> builderFactory = byteBuddy -> generatedType.isInterface() ?
				byteBuddy.subclass(Object.class).implement(generatedType) :
				byteBuddy.subclass(generatedType);

		return Try.of(() -> {
			return (Class<D>) builderFactory.andThen(interceptStrategy).apply(new ByteBuddy())
				.defineField(DELEGATE_FIELD_NAME, commonDelegateType, Modifier.PRIVATE)
				.implement(DelegateProvider.class)
				.method(isAbstract().and(isGetter(commonDelegateType).or(isDeclaredBy(DelegateProvider.class))))
					.intercept(FieldAccessor.ofField(DELEGATE_FIELD_NAME))
				.make()
				.load(Option.of(classLoader).getOrElse(ByteBuddyClassDelegateGenerator.class.getClassLoader()), ClassLoadingStrategy.Default.INJECTION)
				.getLoaded();
		}).get();
	}
}
