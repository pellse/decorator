/**
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Sebastien Pelletier
 *
 */
public final class DelegateInstantiationInfo {

	private final Constructor<?> constructor;
	private final int parameterToInsertIndex;

	private final Collection<Field> injectableFields;

	public DelegateInstantiationInfo(Constructor<?> constructor, int parameterToInsertIndex, Collection<Field> injectableFields) {
		this.constructor = constructor;
		this.parameterToInsertIndex = parameterToInsertIndex;
		this.injectableFields = injectableFields != null ? injectableFields : Collections.emptyList();
	}

	public Constructor<?> getConstructor() {
		return constructor;
	}

	public int getParameterToInsertIndex() {
		return parameterToInsertIndex;
	}

	public Collection<Field> getInjectableFields() {
		return injectableFields;
	}
}
