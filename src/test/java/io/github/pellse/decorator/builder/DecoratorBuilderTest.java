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
package io.github.pellse.decorator.builder;

import static java.util.Collections.synchronizedList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import io.github.pellse.decorator.collection.BoundedList;
import io.github.pellse.decorator.collection.DirtyList;
import io.github.pellse.decorator.collection.DirtyListInvocationHandler;
import io.github.pellse.decorator.collection.ForwarderInvocationHandler;
import io.github.pellse.decorator.collection.IDirtyList;
import io.github.pellse.decorator.collection.SafeList;
import io.github.pellse.decorator.util.EmptyClass;

public class DecoratorBuilderTest {

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
	public void testDecoratorBuilderWithParams() {

		DirtyList<String> dirtyList = DecoratorBuilder.of(new ArrayList<>(), List.class)
				.with(SafeList.class)
				.with(BoundedList.class)
					.params(50)
					.paramTypes(int.class)
				.with(DirtyList.class)
				.make();

		dirtyList.add("aaa");
		assertTrue(dirtyList.isDirty());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorBuilderWithDynamicInvocationHandler() {

		IDirtyList<EmptyClass> dirtyList = DecoratorBuilder.of(new ArrayList<>(), List.class)
				.with(delegate -> synchronizedList(delegate))
				.with(SafeList.class)
				.with(delegate -> synchronizedList(delegate))
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(delegate -> synchronizedList(delegate))
				.with(BoundedList.class)
					.params(50)
				.with(delegate -> synchronizedList(delegate))
				.with(new DirtyListInvocationHandler())
					.as(IDirtyList.class)
				.make();

		EmptyClass emptyClass = new EmptyClass();
		dirtyList.add(emptyClass);

		assertTrue(dirtyList.isDirty());
		assertFalse(dirtyList.removeIf(s -> s.equals(emptyClass)));
	}

	@Test
	public void testExistingInputStreamDelegate() throws Exception {

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		new DataOutputStream(bOut).writeInt(100);

		DataInputStream in = DecoratorBuilder.of(new ByteArrayInputStream(bOut.toByteArray()), InputStream.class)
				.with(BufferedInputStream.class)
					.params(50)
				.with(new ForwarderInvocationHandler())
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(DataInputStream.class)
				.make();

		int value = in.readInt();
		assertEquals(100, value);

		assertEquals(DataInputStream.class, in.getClass());
	}

	@Test
	public void testExistingInputStreamDelegateDirectInvocation() throws Exception {

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		new DataOutputStream(bOut).writeInt(100);

		DataInputStream in = DecoratorBuilder.of(new ByteArrayInputStream(bOut.toByteArray()), InputStream.class)
				.with(delegate -> new BufferedInputStream(delegate, 50))
				.with(new ForwarderInvocationHandler())
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(delegate -> new DataInputStream(delegate))
				.make();

		int value = in.readInt();
		assertEquals(100, value);

		assertEquals(DataInputStream.class, in.getClass());
	}

	@Test
	public void testNoDelegateMixin() {

		ArrayList<Integer> inputList = new ArrayList<>();
		ArrayList<Integer> outputList = DecoratorBuilder.of(inputList, List.class).make();

		assertTrue(inputList == outputList);
	}
}
