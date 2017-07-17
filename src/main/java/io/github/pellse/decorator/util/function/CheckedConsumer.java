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
import java.util.function.Consumer;

/**
 * @author Sebastien Pelletier
 *
 */
@FunctionalInterface
public interface CheckedConsumer<T, E extends Throwable> extends Consumer<T> {

	void checkedAccept(T t) throws E;

	@Override
	default void accept(T t) {
		try {
			checkedAccept(t);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	default <E1 extends Throwable> CheckedConsumer<T, E1> andThen(CheckedConsumer<? super T, E1> after) {
		Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
	}

	static <T1, E1 extends Throwable> CheckedConsumer<T1, E1> of(CheckedConsumer<T1, E1> consumer) {
		return consumer;
	}
}
