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

import static io.github.pellse.decorator.util.reflection.ReflectionUtils.getField;
import static io.github.pellse.decorator.util.reflection.ReflectionUtils.invoke;
import static io.github.pellse.decorator.util.reflection.ReflectionUtils.setField;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withParametersAssignableTo;
import static org.reflections.ReflectionUtils.withParametersCount;
import static org.reflections.ReflectionUtils.withPrefix;
import static org.reflections.ReflectionUtils.withTypeAssignableTo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import com.google.common.base.Predicate;

public class Injector {

	private Injector() {}

	@SuppressWarnings("unchecked")
	public static <T> boolean injectField(Object target, T fieldValue, Class<? super T> fieldType, boolean override) {
		return injectField(target, fieldValue, override, withTypeAssignableTo(fieldType));
	}

	@SuppressWarnings("unchecked")
	public static <T> boolean injectField(Object target, T fieldValue, Class<? super T> fieldType, Class<? extends Annotation> annotationType, boolean override) {
		return injectField(target, fieldValue, override, withTypeAssignableTo(fieldType), withAnnotation(annotationType));
	}

	@SuppressWarnings("unchecked")
	private static <T> boolean injectField(Object target, T fieldValue, boolean override, Predicate<? super Field>... filters) {

		Set<Field> fields = getAllFields(target.getClass(), filters);
		fields.stream()
			.filter(field -> !getField(target, field).isPresent() || override)
			.forEach(field -> setField(target, field, fieldValue));
		return !fields.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public static <T> boolean injectSetterMethod(Object target, T setterValue, Class<? super T> setterType) {
		return injectSetterMethod(target, setterValue, withPrefix("set"), withParametersCount(1), withParametersAssignableTo(setterType));
	}

	@SuppressWarnings("unchecked")
	public static <T> boolean injectSetterMethod(Object target, T setterValue, Class<? super T> setterType, Class<? extends Annotation> annotationType) {
		return injectSetterMethod(target, setterValue, withPrefix("set"), withParametersCount(1), withParametersAssignableTo(setterType), withAnnotation(annotationType));
	}

	@SuppressWarnings("unchecked")
	private static <T> boolean injectSetterMethod(Object target, T setterValue, Predicate<? super Method>... filters) {
		Set<Method> methods = getAllMethods(target.getClass(), filters);
		methods.stream()
			.forEach(method -> invoke(target, method, setterValue));
		return !methods.isEmpty();
	}
}
