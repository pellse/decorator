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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;

public interface SafeList<E> extends List<E> {

	static Kryo kryo = new Kryo();

	List<E> getDelegate();

	@Override
	default boolean add(E e) {
		return getDelegate().add(clone(e));
	}

	@Override
	default void add(int index, E e) {
		getDelegate().add(index, clone(e));
	}

	@Override
	default boolean addAll(Collection<? extends E> c) {
		return getDelegate().addAll(c.stream().map(SafeList::clone).collect(toList()));
	}

	@Override
	default boolean addAll(int index, Collection<? extends E> c) {
		return getDelegate().addAll(index, c.stream().map(SafeList::clone).collect(toList()));
	}

	@Override
	default E set(int index, E e) {
		return getDelegate().set(index, clone(e));
	}

	static <E> E clone(E obj) {
		kryo.register(obj.getClass());
		return kryo.copy(obj);
	}
}
