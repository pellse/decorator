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
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public abstract class DirtyList<E> implements List<E> {

	private List<E> delegate;

	private boolean isDirty = false;

	public DirtyList(List<E> delegate) {
		this.delegate = delegate;
	}

	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		isDirty = true;
		return delegate.removeIf(filter);
	}

	@Override
	public boolean add(E e) {
		isDirty = true;
		return delegate.add(e);
	}

	@Override
	public boolean remove(Object o) {
		isDirty = true;
		return delegate.remove(o);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		isDirty = true;
		return delegate.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		isDirty = true;
		return delegate.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		isDirty = true;
		return delegate.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		isDirty = true;
		return delegate.retainAll(c);
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		isDirty = true;
		delegate.replaceAll(operator);
	}

	@Override
	public void clear() {
		isDirty = true;
		delegate.clear();
	}

	@Override
	public E set(int index, E element) {
		isDirty = true;
		return delegate.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		isDirty = true;
		delegate.add(index, element);
	}

	@Override
	public E remove(int index) {
		isDirty = true;
		return delegate.remove(index);
	}
}
