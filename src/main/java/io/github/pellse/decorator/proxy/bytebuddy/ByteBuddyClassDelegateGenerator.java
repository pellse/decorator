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

import static io.github.pellse.decorator.util.reflection.ReflectionUtils.isNestedClass;
import static io.github.pellse.decorator.util.reflection.ReflectionUtils.newInstance;
import static io.github.pellse.decorator.util.reflection.ReflectionUtils.setField;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isGetter;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.reflect.Modifier;
import java.util.function.BiFunction;
import java.util.function.Function;

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
import net.bytebuddy.implementation.Forwarding;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

public class ByteBuddyClassDelegateGenerator<I> implements DelegateGenerator<I> {

	private static final String DELEGATE_FIELD_NAME = "delegate";

	@Override
	public <D extends I, T extends I> D generateDelegate(T delegateTarget,
			Class<D> generatedType,
			Class<I> commonDelegateType,
			BiFunction<Class<D>, T, D> instanceCreator,
			ClassLoader classLoader) {
		return generateDelegate(delegateTarget,
				generatedType,
				commonDelegateType,
				builder -> builder.method(isAbstract().and(not(isGetter(commonDelegateType))))
					.intercept(isNestedClass(delegateTarget.getClass())
							? InvocationHandlerAdapter.of((proxy, method, args) -> method.invoke(delegateTarget, args))
							: Forwarding.to(delegateTarget)),
				instanceCreator,
				classLoader);
	}

	@Override
	public <D extends I, T extends I> D generateDelegate(T delegateTarget,
			DelegateInvocationHandler handler,
			Class<D> generatedType,
			Class<I> commonDelegateType,
			ClassLoader classLoader) {
		return generateDelegate(delegateTarget,
				generatedType,
				commonDelegateType,
				builder -> builder.method(not(isDeclaredBy(Object.class)))
					.intercept(InvocationHandlerAdapter.of((proxy, method, args) -> handler.invoke(delegateTarget, method, args))),
				null,
				classLoader);
	}

	@SuppressWarnings("unchecked")
	static <I, D extends I, T extends I> D generateDelegate(T delegateTarget,
			Class<D> generatedType,
			Class<I> commonDelegateType,
			Function<Builder<?>, ReceiverTypeDefinition<?>> interceptStrategy,
			BiFunction<Class<D>, T, D> instanceCreator,
			ClassLoader classLoader) {

		Function<ByteBuddy, Builder<?>> builderFactory = byteBuddy -> generatedType.isInterface() ?
				byteBuddy.subclass(Object.class).implement(generatedType) :
				byteBuddy.subclass(generatedType);

		return Try.of(() -> {
			Class<D> delegateClass = (Class<D>) builderFactory.andThen(interceptStrategy).apply(new ByteBuddy())
				.implement(DelegateProvider.class)
					.intercept(FieldAccessor.ofField(DELEGATE_FIELD_NAME))
				.defineField(DELEGATE_FIELD_NAME, commonDelegateType, Modifier.PRIVATE)
				.method(isAbstract().and(isGetter(commonDelegateType)))
					.intercept(FieldAccessor.ofField(DELEGATE_FIELD_NAME))
				.make()
				.load(Option.of(classLoader).getOrElse(ByteBuddyClassDelegateGenerator.class.getClassLoader()), ClassLoadingStrategy.Default.INJECTION)
				.getLoaded();

			D delegate = Option.of(instanceCreator).getOrElse((clazz, target) -> newInstance(clazz)).apply(delegateClass, delegateTarget);
			setField(delegate, delegate.getClass().getDeclaredField(DELEGATE_FIELD_NAME), delegateTarget);

			return delegate;
		}).get();
	}
}
