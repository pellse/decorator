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

public abstract class AbstractDecorator<I, T extends I> implements Decorator<I, T> {

	private final Decorator<I, ? extends I> next;
	
	public AbstractDecorator(Decorator<I, ? extends I> next) {
		this.next = next;
	}

	@Override
	public Optional<Decorator<I, ? extends I>> next() {
		return Optional.ofNullable(next);
	}
}