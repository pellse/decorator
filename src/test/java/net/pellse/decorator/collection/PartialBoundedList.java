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
package net.pellse.decorator.collection;

import java.util.List;

public abstract class PartialBoundedList<E> implements List<E> {

	private final int maxNbItems;

	protected abstract List<E> getDelegate();

	public PartialBoundedList(int maxNbItems) {
		this.maxNbItems = maxNbItems;
	}

	@Override
	public boolean add(E e) {
		checkSize(1);
		return getDelegate().add(e);
	}

	@Override
	public void add(int index, E element) {
		checkSize(1);
		getDelegate().add(index, element);
	}

	protected void checkSize(int addCount) {
		if (getDelegate().size() + addCount >= maxNbItems)
			throw new IllegalStateException("Size of list (" + getDelegate().size() + ") greater than maxNbItems (" + maxNbItems + ")");
	}
}
