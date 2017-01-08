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
import static javaslang.collection.Stream.rangeClosed;
import static org.apache.commons.lang3.ClassUtils.toClass;
import static org.apache.commons.lang3.reflect.ConstructorUtils.invokeConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;

import javaslang.collection.List;
import javaslang.control.Option;
import javaslang.control.Try;

public class ReflectionUtils {

	private ReflectionUtils() {}

	public static void copyFields(Object src, Object target) {

		Class<?> targetClass = target.getClass();
		do {
			stream(targetClass.getDeclaredFields()).forEach(field -> Try.run(() -> field.set(target, field.get(src))).get());
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}

	public static <T> T newInstance(Class<T> clazz) {
		return Try.of(clazz::newInstance).get();
	}

	public static <T> T newInstance(Class<T> clazz, Object... args) {
		return Try.of(() -> invokeConstructor(clazz, args)).get();
	}

	public static <T, U> T newInstance(Class<T> clazz, U argToInsert, Object... args) {
		return newInstance(clazz, argToInsert, args, toClass(args));
	}

	public static <T, U> T newInstance(Class<T> clazz, U argToInsert, Object[] args, Class<?>[] argTypes) {
		return rangeClosed(0, args.length)
			.map(i -> Try.of(() -> invokeConstructor(clazz,
					List.of(args).insert(i, argToInsert).toJavaArray(),
					List.of(argTypes).insert(i, argToInsert.getClass()).toJavaList().stream().toArray(Class<?>[]::new))))
			.find(Try::isSuccess)
			.getOrElse(() -> Try.of(() -> invokeConstructor(clazz, args, argTypes)))
			.get();
	}

	public static Class<?>[] wrapperToPrimitives(Class<?>... wrapperClasses) {
		return stream(wrapperClasses)
			.map(ReflectionUtils::wrapperToPrimitive)
			.toArray(Class<?>[]::new);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> wrapperToPrimitive(Class<T> wrapperClass) {
		return Option.of((Class<T>)ClassUtils.wrapperToPrimitive(wrapperClass)).getOrElse(wrapperClass);
	}

	public static <T> T setField(T obj, Field field, Object value) {
		Try.run(() -> {
			field.setAccessible(true);
			field.set(obj, value);
		});
		return obj;
	}

	public static Optional<Object> getField(Object obj, Field field) {
		return Optional.ofNullable(
				Try.of(() -> {
					field.setAccessible(true);
					return field.get(obj);
				}).get());
	}

	public static Object invoke(Object obj, String methodName, Object... args) {
		return Try.of(() -> {
			Method method = obj.getClass().getMethod(methodName, toClass(args));
			method.setAccessible(true);
			return method.invoke(obj, args);
		}).get();
	}

	public static Object invoke(Object obj, Method method, Object... args) {
		return Try.of(() -> {
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
}
