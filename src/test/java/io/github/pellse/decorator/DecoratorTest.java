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

import static java.lang.Runtime.getRuntime;
import static java.util.Collections.synchronizedList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.github.pellse.decorator.collection.BoundedList;
import io.github.pellse.decorator.collection.BoundedList2;
import io.github.pellse.decorator.collection.DirtyList;
import io.github.pellse.decorator.collection.DirtyListInvocationHandler;
import io.github.pellse.decorator.collection.ForwarderInvocationHandler;
import io.github.pellse.decorator.collection.IDirtyList;
import io.github.pellse.decorator.collection.InitializedBoundedList;
import io.github.pellse.decorator.collection.SafeList;
import io.github.pellse.decorator.util.DelegateList;
import io.github.pellse.decorator.util.EmptyClass;

public class DecoratorTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithSafeListOfInstancesNotImplementingEquals() {

		DirtyList<EmptyClass> dirtyList = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(DirtyList.class)
				.make();

		EmptyClass emptyClass = new EmptyClass();
		dirtyList.add(emptyClass);

		assertThat(dirtyList.isDirty(), is(true));
		assertThat(dirtyList.removeIf(s -> s.equals(emptyClass)), is(false));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithSafeListOfInstancesImplementingEquals() {

		DirtyList<String> dirtyList = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(DirtyList.class)
				.make();

		dirtyList.add("aaa");
		boolean removed = dirtyList.removeIf(s -> s.equals("aaa"));

		assertThat(dirtyList.isDirty(), is(true));
		assertThat(removed, is(true));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorIsImplementingDelegateProvider() {

		List<String> list = new ArrayList<>();
		List<String> decoratorList = Decorator.of(list, List.class)
				.with(SafeList.class)
				.make();

		assertThat(((DelegateProvider<List<String>>)decoratorList).getDelegate(), equalTo(list));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorCache() {

		List<Integer> listInteger = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.make();

		listInteger.add(10);
		listInteger.removeIf(i -> i == 10);

		List<String> listString = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.make();

		listString.add("aaa");
		listString.remove("aaa");

		assertThat(listInteger, empty());
		assertThat(listString, empty());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorCache2() {

		DirtyList<Integer> listInteger = Decorator.of(new ArrayList<>(), List.class)
				.with(DirtyList.class)
				.make();

		DirtyList<String> listString = Decorator.of(new ArrayList<>(), List.class)
				.with(DirtyList.class)
				.make();

		DirtyList<String> listEmpty = Decorator.of(new ArrayList<>(), List.class)
				.with(DirtyList.class)
				.make();

		listInteger.add(10);
		listString.add("aaa");

		assertThat(listInteger.isDirty(), is(true));
		assertThat(listString.isDirty(), is(true));
		assertThat(listEmpty.isDirty(), is(false));

		assertThat(listInteger, hasItem(10));
		assertThat(listString, hasItem("aaa"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorCache3() {

		List<Integer> listInteger = Decorator.of(new ArrayList<>(), List.class)
				.with(BoundedList.class, 50)
				.make();

		List<String> listString = Decorator.of(new ArrayList<>(), List.class)
				.with(BoundedList.class, 50)
				.make();

		listInteger.add(10);
		listString.add("aaa");

		assertThat(listInteger, hasItem(10));
		assertThat(listString, hasItem("aaa"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorCache4() {

		List<Integer> listInteger = Decorator.of(new ArrayList<>(), List.class)
				.with(BoundedList2.class, 50)
				.make();

		List<String> listString = Decorator.of(new ArrayList<>(), List.class)
				.with(BoundedList2.class, 50)
				.make();

		listInteger.add(10);
		listString.add("aaa");

		assertThat(listInteger, hasItem(10));
		assertThat(listString, hasItem("aaa"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithOneArgumentConstructor() {

		List<String> boundedList = Decorator.of(new ArrayList<>(), List.class)
				.with(delegate -> synchronizedList(delegate))
				.with(SafeList.class)
				.with(DirtyList.class)
				.with(BoundedList.class, 50)
				.with(delegate -> synchronizedList((List<String>)delegate))
				.with(BoundedList.class, new Integer(100))
				.make();

		boundedList.add("bbb");
		boundedList.set(0, "aaa");
		boolean removed = boundedList.removeIf(s -> s.equals("aaa"));

		assertThat(removed, is(true));
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalStateException.class)
	public void testDecoratorWithBoundedList() {

		List<String> list = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(DirtyList.class)
				.with(BoundedList.class, 2)
				.make();

		list.add("aaa");
		list.add("bbb");
		list.add("ccc");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithInjectionOfDelegateInPrivateField() {

		BoundedList2<String> boundedList = Decorator.of(new ArrayList<>(), List.class)
				.with(BoundedList2.class, 2)
				.make();

		boundedList.add("aaa");

		assertThat(boundedList.getUselessList(), nullValue());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithInvocationHandler() {

		IDirtyList<String> list = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(new ForwarderInvocationHandler<>())
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(InitializedBoundedList.class)
				.with(delegate -> synchronizedList((List<String>)delegate))
				.with(new DirtyListInvocationHandler<>(), IDirtyList.class)
				.make();

		list.add("aaa");
		list.remove("bbb");

		boolean removed = list.removeIf(s -> s.equals("aaa"));

		assertThat(list, empty());
		assertThat(removed, is(true));
		assertThat(list.isDirty(), is(true));
	}

	@Test
	public void testDecoratorWithDirectInvocation() {

		List<Object> list = Decorator.of(new ArrayList<>(), List.class)
				.with(delegate -> synchronizedList(delegate))
				.with(delegate -> synchronizedList(delegate))
				.with(delegate -> synchronizedList(delegate))
				.with(delegate -> synchronizedList(delegate))
				.with(delegate -> synchronizedList(delegate))
				.make();

		list.stream().filter(s -> true).map(s -> s);

		list.add("aaa");
		boolean removed = list.removeIf(s -> s.equals("aaa"));

		assertThat(removed, is(true));
	}

	@Test
	public void testExistingInputStreamDelegate() throws Exception {

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		new DataOutputStream(bOut).writeInt(100);

		DataInputStream in = Decorator.of(new ByteArrayInputStream(bOut.toByteArray()), InputStream.class)
				.with(BufferedInputStream.class, 50)
				.with(new ForwarderInvocationHandler<>())
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(DataInputStream.class)
				.make();

		int value = in.readInt();

		assertThat(value, equalTo(100));
		assertThat(in, isA(DataInputStream.class));
	}

	@Test
	public void testExistingInputStreamDelegateDirectInvocation() throws Exception {

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		new DataOutputStream(bOut).writeInt(100);

		DataInputStream in = Decorator.of(new ByteArrayInputStream(bOut.toByteArray()), InputStream.class)
				.with(delegate -> new BufferedInputStream(delegate, 50))
				.with(new ForwarderInvocationHandler<>())
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(delegate -> new DataInputStream(delegate))
				.make();

		int value = in.readInt();

		assertThat(value, equalTo(100));
		assertThat(in, isA(DataInputStream.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDirectInvocationWithList() {

		DelegateList<Integer> outputList = Decorator.of(new ArrayList<>(), List.class)
				.with(delegate -> new DelegateList<>(delegate))
				.with(SafeList.class)
				.with(delegate -> new DelegateList<>((SafeList<Integer>)delegate))
				.make();

		outputList.add(1);
		assertThat(outputList.get(0), equalTo(1));
	}

	@Test
	public void testDirectInvocationWithNonGenericDelegate() throws Exception {

		DataInputStream in = Decorator.of(new ByteArrayInputStream(new byte[]{9, 99}), InputStream.class)
				.with(delegate -> new BufferedInputStream(delegate))
				.with(DelegateInputStream.class)
				.with(delegate -> new DataInputStream(delegate))
				.make();

		byte[] buffer = new byte[2];
		in.read(buffer);

		assertThat(buffer, equalTo(new byte[]{10, 100}));
	}

	@Test
	public void testDirectInvocationWithNonGenericExistingDelegate() throws Exception {

		DataInputStream in = Decorator.of(new ByteArrayInputStream(new byte[]{9, 99}), InputStream.class)
				.with(delegate -> new BufferedInputStream(delegate))
				.with(AbstractDelegateInputStream.class)
				.with(delegate -> new DataInputStream(delegate))
				.make();

		byte[] buffer = new byte[2];
		in.read(buffer);

		assertThat(buffer, equalTo(new byte[]{10, 100}));
	}

	@Test
	public void testNoDelegateMixin() {

		List<Integer> inputList = new ArrayList<>();
		List<Integer> outputList = Decorator.of(inputList, List.class).make();

		assertThat(inputList, sameInstance(outputList));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithDelegateFromStaticInnerClass() {

		List<CharSequence> list = Decorator.of(new ArrayList<>(), List.class)
				.with(ListStaticSubclass.class)
				.make();

		list.add("aaa");
		assertThat(list.get(0), equalTo("aaa"));
	}

	@SuppressWarnings("unchecked")
	@Test
	@Ignore
	public void testDecoratorGC() throws Exception {

		System.gc();
		Thread.sleep(1000);
		System.gc();

		long usedMemoryBefore = getRuntime().totalMemory() - getRuntime().freeMemory();

		for (int i = 0; i < 10000; i++) {
			List<String> list = Decorator.of(new ArrayList<>(), List.class)
					.with(SafeList.class)
					.with(BoundedList.class, 50)
					.with(InitializedBoundedList.class)
					.with(DirtyList.class)
					.make();

			list.add("aaa");
		}

		System.gc();
		Thread.sleep(1000);
		System.gc();

		long usedMemoryAfter = getRuntime().totalMemory() - getRuntime().freeMemory();

		assertThat(usedMemoryAfter, lessThanOrEqualTo(usedMemoryBefore));
	}

	public static abstract class ListStaticSubclass<E> implements List<E> {

		@Inject
		List<E> delegate;

		@Override
		public boolean add(E e) {
			return delegate.add(e);
		}
	}

	public static class DelegateInputStream extends InputStream {

		@Inject
		InputStream delegate;

		@Override
		public int read() throws IOException {
			return delegate.read() + 1;
		}
	}

	public static abstract class AbstractDelegateInputStream extends InputStream {

		@Inject
		InputStream delegate;

		@Override
		public int read() throws IOException {
			return delegate.read() + 1;
		}
	}
}
