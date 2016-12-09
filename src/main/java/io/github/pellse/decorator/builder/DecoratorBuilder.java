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
package io.github.pellse.decorator.builder;

import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

import io.github.pellse.decorator.Decorator;
import io.github.pellse.decorator.aop.DelegateInvocationFilter;
import io.github.pellse.decorator.aop.DelegateInvocationHandler;
import javaslang.control.Option;

/*BoundedList<String> boundedList = Decorator.of(new ArrayList<>(), List.class)
.with(SafeList.class)
.with(DirtyList.class)
.with(BoundedList.class)
	.parameters(50)
	.parameterTypes(Integer.class)
	.when((method, args) -> method.getName().contains("add"))
.with(new DirtyListInvocationHandler())
	.when(methodNameContains("add", "remove").and(...))
.with((delegate, method, args) -> method.invoke(delegate, args))
	.when(...)
.makeAs(IDirtyList.class);*/


public interface DecoratorBuilder {

	@SuppressWarnings("unchecked")
	public static <I, T extends I> RootDelegateBuilder<I, T> of(T rootObject, Class<I> delegateInterface) {
		return new RootDelegateBuilder<>(Decorator.of(rootObject, delegateInterface), (Class<T>) rootObject.getClass(), delegateInterface);
	}

	public static abstract class DelegateBuilder<I, D extends I, T extends I> {

		private final Decorator<I, T> decorator;
		private final Class<D> typeToGenerate;
		private final Class<I> commonDelegateType;

		DelegateBuilder(Decorator<I, T> decorator, Class<D> typeToGenerate, Class<I> commonDelegateType) {
			this.decorator = decorator;
			this.typeToGenerate = typeToGenerate;
			this.commonDelegateType = commonDelegateType;
		}

		Decorator<I, T> getDecorator() {
			return decorator;
		}

		Class<D> getTypeToGenerate() {
			return typeToGenerate;
		}

		Class<I> getCommonDelegateType() {
			return commonDelegateType;
		}

		public <F extends I> ExistingDelegateBuilder<I, F, D> with(Function<D, F> delegateFactory) {
			return new ExistingDelegateBuilder<>(generateDecorator(), getCommonDelegateType(), delegateFactory);
		}

		public <F extends I> ClassDelegateBuilderWithParam<I, F, D> with(Class<F> typeToGenerate) {
			return new ClassDelegateBuilderWithParam<>(generateDecorator(), typeToGenerate, getCommonDelegateType());
		}

		@SuppressWarnings("unchecked")
		public DynamicDelegateBuilder<I, I> with(DelegateInvocationHandler delegateHandler) {
			return new DynamicDelegateBuilder<>((Decorator<I, I>) generateDecorator(), delegateHandler, getCommonDelegateType(), getCommonDelegateType());
		}

		public D make() {
			return generateDecorator().make();
		}

		abstract Decorator<I, D> generateDecorator();
	}

	public static class RootDelegateBuilder<I, T extends I> extends DelegateBuilder<I, T, T> {

		RootDelegateBuilder(Decorator<I, T> decorator, Class<T> typeToGenerate, Class<I> commonDelegateType) {
			super(decorator, typeToGenerate, commonDelegateType);
		}

		@Override
		Decorator<I, T> generateDecorator() {
			return getDecorator();
		}
	}

	public static class ExistingDelegateBuilder<I, D extends I, T extends I> extends DelegateBuilder<I, D, T> {

		private final Function<T, D> delegateFactory;

		@SuppressWarnings("unchecked")
		ExistingDelegateBuilder(Decorator<I, T> decorator, Class<I> commonDelegateType, Function<T, D> delegateFactory) {
			super(decorator, (Class<D>)commonDelegateType, commonDelegateType);
			this.delegateFactory = delegateFactory;
		}

		@Override
		Decorator<I, D> generateDecorator() {
			return getDecorator().with(delegateFactory);
		}
	}

	public static class ClassDelegateBuilderWithParam<I, D extends I, T extends I> extends DelegateBuilder<I, D, T> {

		private Object[] parameters = ArrayUtils.EMPTY_OBJECT_ARRAY;
		private Class<?>[] parameterTypes;

		ClassDelegateBuilderWithParam(Decorator<I, T> decorator, Class<D> typeToGenerate, Class<I> commonDelegateType) {
			super(decorator, typeToGenerate, commonDelegateType);
		}

		public ClassDelegateBuilderWithParam<I, D, T> params(Object... parameters) {
			this.parameters = parameters;
			return this;
		}

		public ClassDelegateBuilderWithParam<I, D, T> paramTypes(Class<?>... parameterTypes) {
			this.parameterTypes = parameterTypes;
			return this;
		}

		@Override
		Decorator<I, D> generateDecorator() {
			return getDecorator().with(getTypeToGenerate(), parameters, Option.of(parameterTypes).getOrElse(() -> ClassUtils.toClass(parameters)));
		}
	}

	public static class DynamicDelegateBuilder<I, D extends I> extends DelegateBuilder<I, D, I> {

		private final DelegateInvocationHandler delegateHandler;
		private Class<? extends D> newTypeToGenerate;

		DynamicDelegateBuilder(Decorator<I, I> decorator, DelegateInvocationHandler delegateHandler, Class<D> typeToGenerate, Class<I> commonDelegateType) {
			super(decorator, typeToGenerate, commonDelegateType);
			this.delegateHandler = delegateHandler;
		}

		public DynamicDelegateBuilder<I, D> when(DelegateInvocationFilter filter) {
			return this;
		}

		@SuppressWarnings("unchecked")
		public <F extends D> DynamicDelegateBuilder<I, F> as(Class<F> typeToGenerate) {
			newTypeToGenerate = typeToGenerate;
			return (DynamicDelegateBuilder<I, F>) this;
		}

		@SuppressWarnings("unchecked")
		@Override
		Decorator<I, D> generateDecorator() {
			return getDecorator().with(delegateHandler, newTypeToGenerate != null ? (Class<D>) newTypeToGenerate : getTypeToGenerate());
		}
	}
}
