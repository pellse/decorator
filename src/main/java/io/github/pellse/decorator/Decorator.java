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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.pellse.decorator.aop.DelegateInvocationHandler;
import io.github.pellse.decorator.proxy.DelegateGenerator;
import io.github.pellse.decorator.proxy.bytebuddy.ByteBuddyClassDelegateGenerator;

public interface Decorator<I, T extends I> {

	Optional<Decorator<I, ? extends I>> next();

	<D extends I> Decorator<I, D> with(DelegateInvocationHandler delegateHandler);
	<D extends I> Decorator<I, D> with(DelegateInvocationHandler delegateHandler, Class<D> generatedType);
	<D extends I> Decorator<I, D> with(Function<? super T, ? extends D> delegateFactory);
	<D extends I> Decorator<I, D> with(Class<D> generatedType, Object... constructorArgs);
	<D extends I> Decorator<I, D> with(Class<D> generatedType, Object[] constructorArgs, Class<?>[] constructorArgTypes);
	<D extends I> Decorator<I, D> with(Supplier<D> delegateSupplier);

	T make();

	static <I, T extends I> Decorator<I, T> of(T rootObject, Class<I> delegateInterface) {
		return of(rootObject, delegateInterface, new ByteBuddyClassDelegateGenerator<>());
	}

	static <I, T extends I> Decorator<I, T> of(T rootObject, Class<I> delegateInterface, DelegateGenerator<I> generator) {
		return new GeneratedDecorator<>(rootObject, delegateInterface, generator);
	}
}
