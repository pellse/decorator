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
package net.pellse.decorator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.pellse.decorator.Decorator;
import net.pellse.decorator.collection.BoundedList;
import net.pellse.decorator.collection.BoundedList2;
import net.pellse.decorator.collection.DirtyList;
import net.pellse.decorator.collection.DirtyListInvocationHandler;
import net.pellse.decorator.collection.ForwarderInvocationHandler;
import net.pellse.decorator.collection.IDirtyList;
import net.pellse.decorator.collection.InitializedBoundedList;
import net.pellse.decorator.collection.SafeList;
import net.pellse.decorator.util.EmptyClass;

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

		assertTrue(dirtyList.isDirty());
		assertFalse(dirtyList.removeIf(s -> s.equals(emptyClass)));
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

		assertTrue(dirtyList.isDirty());
		assertTrue(removed);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithOneArgumentConstructor() {

		BoundedList<String> boundedList = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(DirtyList.class)
				.with(BoundedList.class, 50)
				.with(BoundedList.class, new Integer(100))
				.make();

		boundedList.add("aaa");
		boolean removed = boundedList.removeIf(s -> s.equals("aaa"));

		assertTrue(removed);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalStateException.class)
	public void testDecoratorWithBoundedList() {

		BoundedList<String> boundedList = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(DirtyList.class)
				.with(BoundedList.class, 2)
				.make();

		boundedList.add("aaa");
		boundedList.add("bbb");
		boundedList.add("ccc");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithInjectionOfDelegateInPrivateField() {

		BoundedList2<String> boundedList = Decorator.of(new ArrayList<>(), List.class)
				.with(BoundedList2.class, 2)
				.make();

		boundedList.add("aaa");

		assertNull(boundedList.getUselessList());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorWithInvocationHandler() {

		IDirtyList<String> list = Decorator.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(new ForwarderInvocationHandler())
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(InitializedBoundedList.class)
				.with(new DirtyListInvocationHandler(), IDirtyList.class)
				.make();

		list.stream().filter(s -> true).map(s -> s);

		list.add("aaa");
		boolean removed = list.removeIf(s -> s.equals("aaa"));

		assertTrue(removed);
		assertTrue(list.isDirty());
	}

	@Test
	public void testExistingInputStreamDelegate() throws Exception {

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		new DataOutputStream(bOut).writeInt(100);

		DataInputStream in = Decorator.of(new ByteArrayInputStream(bOut.toByteArray()), InputStream.class)
				.with(BufferedInputStream.class, 50)
				.with(new ForwarderInvocationHandler())
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(DataInputStream.class)
				.make();

		int value = in.readInt();
		assertEquals(100, value);

		assertEquals(DataInputStream.class, in.getClass());
	}

	@Test
	public void testNoDelegateMixin() {

		ArrayList<Integer> inputList = new ArrayList<>();
		ArrayList<Integer> outputList = Decorator.of(inputList, List.class).make();

		assertTrue(inputList == outputList);
	}
}
