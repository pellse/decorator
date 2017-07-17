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
package io.github.pellse.decorator.util.reflection;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.ClassUtils.toClass;
import static org.apache.commons.lang3.reflect.ConstructorUtils.getMatchingAccessibleConstructor;
import static org.apache.commons.lang3.reflect.ConstructorUtils.invokeConstructor;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withTypeAssignableTo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.ClassUtils;

import com.google.common.base.Predicate;

import io.github.pellse.decorator.util.function.CheckedRunnable;
import io.github.pellse.decorator.util.function.CheckedSupplier;

public class ReflectionUtils {

	private ReflectionUtils() {
	}

	@SuppressWarnings("unchecked")
	public static Set<Field> findFields(Class<?> targetClass, Class<?> fieldType) {
		return findFields(targetClass, withTypeAssignableTo(fieldType));
	}

	@SuppressWarnings("unchecked")
	public static Set<Field> findFields(Class<?> targetClass, Class<?> fieldType, Class<? extends Annotation> annotationType) {
		return findFields(targetClass, withTypeAssignableTo(fieldType), withAnnotation(annotationType));
	}

	@SuppressWarnings("unchecked")
	private static Set<Field> findFields(Class<?> targetClass, Predicate<? super Field>... filters) {
		return getAllFields(targetClass, filters);
	}

	public static void copyFields(Object src, Object target) {

		Class<?> targetClass = target.getClass();
		do {
			stream(targetClass.getDeclaredFields()).forEach(field -> CheckedRunnable.of(() -> field.set(target, field.get(src))).run());
			targetClass = targetClass.getSuperclass();
		} while (targetClass != null && targetClass != Object.class);
	}

	public static <T> T newInstance(Class<T> clazz) {
		return CheckedSupplier.of(clazz::newInstance).get();
	}

	public static <T> T newInstance(Class<T> clazz, Object... args) {
		return CheckedSupplier.of(() -> invokeConstructor(clazz, args)).get();
	}

	public static <T, U> T newInstance(Class<T> clazz, U argToInsert, Object... args) {
		return newInstance(clazz, argToInsert, args, toClass(args));
	}

	public static DelegateInstantiationInfo findDelegateInstantiationInfo(Class<?> clazz, Class<?> delegateType, Class<?>[] otherConstructorParameterTypes) {
		return IntStream.rangeClosed(0, otherConstructorParameterTypes.length)
				.mapToObj(i -> new DelegateInstantiationInfo(
						getMatchingAccessibleConstructor(clazz, insert(otherConstructorParameterTypes, i, delegateType)), i, findFields(clazz, delegateType, Inject.class)))
				.filter(dii -> dii.getConstructor() != null)
				.findFirst()
				.orElseGet(() -> new DelegateInstantiationInfo(
						getMatchingAccessibleConstructor(clazz, otherConstructorParameterTypes), -1, findFields(clazz, delegateType, Inject.class)));
	}

	public static <T, U> T newInstance(Class<T> clazz, U argToInsert, Object[] args, Class<?>[] argTypes) {
		return IntStream.rangeClosed(0, args.length)
				.mapToObj(i -> CheckedSupplier.of(() -> newInstance(clazz, argToInsert, i, args, argTypes)).toOptional())
				.flatMap(opt -> opt.map(Stream::of).orElseGet(Stream::empty))
				.findFirst()
				.orElseGet(CheckedSupplier.of(() -> invokeConstructor(clazz, args, argTypes)));
	}

	public static <T, U> T newInstance(Class<T> clazz, U argToInsert, int insertIndex, Object[] args, Class<?>[] argTypes) throws Exception {
		boolean shouldInsert = insertIndex > -1;
		return invokeConstructor(clazz, shouldInsert ? insert(args, insertIndex, argToInsert) : args,
				shouldInsert ? insert(argTypes, insertIndex, argToInsert.getClass()) : argTypes);
	}

	public static Class<?>[] wrapperToPrimitives(Class<?>... wrapperClasses) {
		return stream(wrapperClasses).map(ReflectionUtils::wrapperToPrimitive).toArray(Class<?>[]::new);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> wrapperToPrimitive(Class<T> wrapperClass) {
		return Optional.ofNullable((Class<T>) ClassUtils.wrapperToPrimitive(wrapperClass)).orElse(wrapperClass);
	}

	public static <T> T setField(T obj, Field field, Object value) {
		return setField(obj, field, value, true);
	}

	public static <T> T setField(T obj, Field field, Object value, boolean override) {
		return CheckedSupplier.of(() -> privateSetField(obj, field, value, override)).get();
	}

	public static <T> void setFields(T obj, Collection<Field> fields, Object value, boolean override) {
		fields.forEach(field -> CheckedRunnable.of(() -> privateSetField(obj, field, value, override)).run());
	}

	private static <T> T privateSetField(T obj, Field field, Object value, boolean override) throws IllegalArgumentException, IllegalAccessException {
		field.setAccessible(true);
		if (field.get(obj) == null || override)
			field.set(obj, value);

		return obj;
	}

	public static Optional<Object> getField(Object obj, Field field) {
		return Optional.ofNullable(CheckedSupplier.of(() -> {
			field.setAccessible(true);
			return field.get(obj);
		}).get());
	}

	public static Object invoke(Object obj, Method method, Object... args) {
		return CheckedSupplier.of(() -> {
			method.setAccessible(true);
			return method.invoke(obj, args);
		}).get();
	}

	public static boolean isAbstract(Class<?> clazz) {
		return clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers());
	}

	public static boolean isNestedClass(Class<?> clazz) {
		return clazz.isMemberClass() || clazz.isLocalClass() || clazz.isAnonymousClass();
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] insert(T[] objArray, int index, T obj) {
		if (index > -1) {
			List<T> list = new ArrayList<>(Arrays.asList(objArray));
			list.add(index, obj);
			return list.toArray((T[]) Array.newInstance(objArray.getClass().getComponentType(), 0));
		}
		return objArray;
	}
}
