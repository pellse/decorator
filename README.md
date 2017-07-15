# Decorator

A library that emulates in Java the Scala's Stackable Trait Pattern

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.pellse/decorator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.pellse/decorator)

## Usage Examples

This example shows how to create a component by extending an interface and only implement the necessary methods, the framework will  automatically generate the unimplemented pass through delegate methods. The framework will use the provided delegate method signature as an injection point for the delegate class instance to which forward methods:
```java
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

	public static <E> E clone(E obj) {
		return kryo.copy(obj);
	}
}

SafeList<String> decoratorList = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.make();
```

It is also possible to create a partial component by providing an abstract class that implement only the methods that needs to be overriden (some methods were removed for brevity), the framework will use the provided constructor to inject the appropriate delegate:
```java
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
}

DirtyList<String> dirtyList = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(DirtyList.class)
				.make();
```

The `@Inject` annotation is also supported:
```java
public abstract class DirtyList<E> implements List<E> {

	@Inject
	private List<E> delegate;

	private boolean isDirty = false;

	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean add(E e) {
		isDirty = true;
		return delegate.add(e);
	}

	...
}
```

We can also chain partial components with existing components that fully implement the interface:
```java
DirtyList<String> dirtyList = Decorator.of(new ArrayList<>(), List.class)
				.with(delegate -> Collections.synchronizedList(delegate))
				.with(SafeList.class)
				.with(DirtyList.class)
				.make();
```

## License

Copyright 2017 Sebastien Pelletier

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
