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
package io.github.pellse.decorator;

import static io.github.pellse.decorator.util.reflection.ReflectionUtils.findDelegateInstantiationInfo;
import static io.github.pellse.decorator.util.reflection.ReflectionUtils.setFields;
import static org.apache.commons.lang3.ClassUtils.toClass;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jctools.maps.NonBlockingHashMap;

import io.github.pellse.decorator.aop.DelegateInvocationHandler;
import io.github.pellse.decorator.proxy.DelegateGenerator;
import io.github.pellse.decorator.proxy.bytebuddy.ByteBuddyClassDelegateGenerator;
import io.github.pellse.decorator.util.function.CheckedBiFunction;
import io.github.pellse.decorator.util.function.CheckedSupplier;
import io.github.pellse.decorator.util.reflection.DelegateInstantiationInfo;
import io.github.pellse.decorator.util.reflection.ReflectionUtils;

public class GeneratedDecorator<I, T extends I> extends AbstractDecorator<I, T> {

	private static final Map<Class<?>, DelegateInstantiationInfo> CACHE = new NonBlockingHashMap<>();

	private final T delegateTarget;
	private final Class<I> commonDelegateType;
	private final DelegateGenerator<I> generator;
	private final ClassLoader classLoader;

	public GeneratedDecorator(T delegate, Class<I> commonDelegateType, DelegateGenerator<I> delegateGeneratorFactory) {
		this(delegate, commonDelegateType, delegateGeneratorFactory, GeneratedDecorator.class.getClassLoader());
	}

	public GeneratedDecorator(T delegate,
			Class<I> commonDelegateType,
			DelegateGenerator<I> delegateGeneratorFactory,
			ClassLoader classLoader) {
		this(null, delegate, commonDelegateType, delegateGeneratorFactory, classLoader);
	}

	public GeneratedDecorator(Decorator<I, ? extends I> next,
			T delegateTarget,
			Class<I> commonDelegateType,
			DelegateGenerator<I> generator,
			ClassLoader classLoader) {
		super(next);
		this.delegateTarget = delegateTarget;
		this.commonDelegateType = commonDelegateType;
		this.generator = Optional.ofNullable(generator).orElseGet(ByteBuddyClassDelegateGenerator<I>::new);
		this.classLoader = classLoader;
	}

	@Override
	public Decorator<I, I> with(DelegateInvocationHandler<I> delegateHandler) {
		return with(delegateHandler, commonDelegateType);
	}

	@Override
	public <D extends I> Decorator<I, D> with(DelegateInvocationHandler<I> delegateHandler, Class<D> generatedType) {
		return with(CheckedSupplier.of(() -> generator.generateDelegate(delegateTarget, delegateHandler, generatedType, commonDelegateType, classLoader)));
	}

	@Override
	public <D extends I> Decorator<I, D> with(Function<? super T, ? extends D> delegateFactory) {
		return new GeneratedDecorator<>(this, delegateFactory.apply(delegateTarget), commonDelegateType, generator, classLoader);
	}

	@Override
	public <D extends I> Decorator<I, D> with(Class<D> generatedType, Object... constructorArgs) {
		return with(generatedType, constructorArgs, toClass(constructorArgs));
	}

	@Override
	public <D extends I> Decorator<I, D> with(Class<D> generatedType, Object[] constructorArgs, Class<?>[] constructorArgTypes) {
		BiFunction<Class<D>, T, D> delegateSupplier = CheckedBiFunction.of(
				(type, delegateTarget) -> createInstance(type, delegateTarget, constructorArgs, constructorArgTypes));

		return with(CheckedSupplier.of(() -> generator.generateDelegate(delegateTarget,
				generatedType,
				commonDelegateType,
				delegateSupplier,
				classLoader)));
	}

	@Override
	public <D extends I> Decorator<I, D> with(Supplier<D> delegateSupplier) {
		return new GeneratedDecorator<>(this, delegateSupplier.get(), commonDelegateType, generator, classLoader);
	}

	@Override
	public T make() {
		return delegateTarget;
	}

	@SuppressWarnings("unchecked")
	private <D extends I> D createInstance(Class<D> type, T delegateTarget, Object[] constructorArgs, Class<?>[] constructorArgTypes) throws Exception {

		// TODO: create DelegateInstantiator interface
		// that will encapsulate the creation of the decorator
		DelegateInstantiationInfo delegateInstantiationInfo = CACHE.computeIfAbsent(type,
				clazz -> findDelegateInstantiationInfo(clazz, commonDelegateType, constructorArgTypes));

		Object[] args = ReflectionUtils.insert(constructorArgs, delegateInstantiationInfo.getParameterToInsertIndex(), delegateTarget);

		D instance = (D) delegateInstantiationInfo.getConstructor().newInstance(args);
		setFields(instance, delegateInstantiationInfo.getInjectableFields(), delegateTarget, false);

		return instance;
	}
}
