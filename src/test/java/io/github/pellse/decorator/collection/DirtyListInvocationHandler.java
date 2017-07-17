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
package io.github.pellse.decorator.collection;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import io.github.pellse.decorator.aop.DelegateInvocationHandler;

public class DirtyListInvocationHandler<T> implements DelegateInvocationHandler<T> {

	private boolean isDirty;

	@Override
	public Object invoke(T delegate, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("isDirty"))
			return isDirty;

		if (Stream.of("add", "remove", "set", "clear", "retain").anyMatch(s -> method.getName().startsWith(s)))
			isDirty = true;

		return method.invoke(delegate, args);
	}
}