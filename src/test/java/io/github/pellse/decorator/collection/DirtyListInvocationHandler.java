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
import java.util.Arrays;

import io.github.pellse.decorator.aop.DelegateInvocationHandler;

public class DirtyListInvocationHandler implements DelegateInvocationHandler {
	
	private boolean isDirty;
	
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public Object invoke(Object delegate, Method method, Object[] args) throws Throwable {
		if (Arrays.asList("add", "remove", "set", "clear", "retain").stream()
				.anyMatch(s -> method.getName().contains(s)))
			isDirty = true;
		
		if (method.getName().equals("isDirty"))
			return isDirty;
		
		return method.invoke(delegate, args);
	}
}