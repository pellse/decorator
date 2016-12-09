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

import java.util.Collection;
import java.util.List;

public abstract class BoundedList2<E> extends PartialBoundedList2<E> {

	private List<E> uselessListThatShouldntBeInjected;

	public BoundedList2(int maxNbItems) {
		super(maxNbItems);
	}

	public List<E> getUselessList() {
		return this.uselessListThatShouldntBeInjected;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		checkSize(c.size());
		return delegate.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		checkSize(c.size());
		return delegate.addAll(index, c);
	}
}
