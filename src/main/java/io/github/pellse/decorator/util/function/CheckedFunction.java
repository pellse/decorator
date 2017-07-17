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
package io.github.pellse.decorator.util.function;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Sebastien Pelletier
 *
 */
@FunctionalInterface
public interface CheckedFunction<T, R, E extends Throwable> extends Function<T, R> {

	R checkedApply(T t) throws E;

	@Override
	default R apply(T t) {
		try {
			return checkedApply(t);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	default <V, E1 extends Throwable> CheckedFunction<V, R, E1> compose(CheckedFunction<? super V, ? extends T, E1> before) {
		 Objects.requireNonNull(before);
	     return (V v) -> apply(before.apply(v));
	}

	default <V, E1 extends Throwable> CheckedFunction<T, V, E1> andThen(CheckedFunction<? super R, ? extends V, E1> after) {
		Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
	}

	default Optional<R> toOptional(T t) {
		try {
			return Optional.ofNullable(checkedApply(t));
		} catch (Throwable e) {
			return Optional.empty();
		}
	}

	static <T1, R1, E1 extends Throwable> CheckedFunction<T1, R1, E1> of(CheckedFunction<T1, R1, E1> function) {
		return function;
	}
}
