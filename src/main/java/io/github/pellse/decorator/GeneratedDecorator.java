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

import static io.github.pellse.decorator.util.reflection.ReflectionUtils.newInstance;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.ClassUtils;

import io.github.pellse.decorator.aop.DelegateInvocationHandler;
import io.github.pellse.decorator.proxy.DelegateGenerator;
import io.github.pellse.decorator.proxy.bytebuddy.ByteBuddyClassDelegateGenerator;
import io.github.pellse.decorator.util.reflection.ReflectionUtils;
import javaslang.control.Option;

public class GeneratedDecorator<I, T extends I> extends AbstractDecorator<I, T> {

	private final T delegateTarget;
	private final Class<I> commonDelegateType;
	private final DelegateGenerator<I> generator;

	public GeneratedDecorator(T delegate,
			Class<I> commonDelegateType,
			DelegateGenerator<I> delegateGeneratorFactory) {
		this(null, delegate, commonDelegateType, delegateGeneratorFactory);
	}

	public GeneratedDecorator(Decorator<I, ? extends I> next,
			T delegateTarget,
			Class<I> commonDelegateType,
			DelegateGenerator<I> generator) {
		super(next);
		this.delegateTarget = delegateTarget;
		this.commonDelegateType = commonDelegateType;
		this.generator = Option.of(generator).getOrElse(ByteBuddyClassDelegateGenerator<I>::new);
	}

	@Override
	public Decorator<I, I> with(DelegateInvocationHandler delegateHandler) {
		return with(delegateHandler, commonDelegateType);
	}

	@Override
	public <D extends I> Decorator<I, D> with(DelegateInvocationHandler delegateHandler, Class<D> generatedType) {
		return with(() -> generator.generateDelegate(delegateTarget, delegateHandler, generatedType, commonDelegateType));
	}

	@Override
	public <D extends I> Decorator<I, D> with(Function<? super T, ? extends D> delegateFactory) {
		return new GeneratedDecorator<>(this, delegateFactory.apply(delegateTarget), commonDelegateType, generator);
	}

	@Override
	public <D extends I> Decorator<I, D> with(Class<D> generatedType, Object... constructorArgs) {
		return with(generatedType, constructorArgs, ClassUtils.toClass(constructorArgs));
	}

	@Override
	public <D extends I> Decorator<I, D> with(Class<D> generatedType, Object[] constructorArgs, Class<?>[] constructorArgTypes) {

		BiFunction<Class<D>, T, D> instanceCreator = (type, delegateTarget) -> newInstance(type, delegateTarget, constructorArgs, constructorArgTypes);
		Supplier<D> delegateSupplier = ReflectionUtils.isAbstract(generatedType)
				? () -> generator.generateDelegate(delegateTarget, generatedType, commonDelegateType, instanceCreator)
				: () -> instanceCreator.apply(generatedType, delegateTarget);

		return with(delegateSupplier);
	}

	@Override
	public <D extends I> Decorator<I, D> with(Supplier<D> delegateSupplier) {
		return new GeneratedDecorator<>(this, delegateSupplier.get(), commonDelegateType, generator);
	}

	@Override
	public T make() {
		return delegateTarget;
	}
}
