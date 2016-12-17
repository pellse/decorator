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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

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
import io.github.pellse.decorator.util.DelegateList;
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
		assertThat(dirtyList.isDirty(), is(true));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDecoratorBuilderWithDynamicInvocationHandler() {

		IDirtyList<EmptyClass> dirtyList = DecoratorBuilder.of(new ArrayList<>(), List.class)
				.with(delegate -> new DelegateList<>(delegate))
				.with(SafeList.class)
				.with(delegate -> new DelegateList<>((SafeList<EmptyClass>)delegate)) // With Eclipse Compiler no need to cast, it knows it's a SafeList
																						// even if we lose the generic type of the SafeList
																						// with javac the type of the delegate param is Object
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.with(delegate -> synchronizedList((List<EmptyClass>)delegate))
				.with(BoundedList.class)
					.params(50)
				.with(delegate -> new DelegateList<>((List<EmptyClass>)delegate))
				.with(new DirtyListInvocationHandler())
					.as(IDirtyList.class)
				.make();

		EmptyClass emptyClass = new EmptyClass();
		dirtyList.add(emptyClass);

		assertThat(dirtyList.isDirty(), is(true));
		assertThat(dirtyList.removeIf(s -> s.equals(emptyClass)), is(false));
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

		assertThat(value, is(equalTo(100)));
		assertThat(in, isA(DataInputStream.class));
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

		assertThat(value, is(equalTo(100)));
		assertThat(in, isA(DataInputStream.class));
	}

	@Test
	public void testNoDelegateMixin() {

		ArrayList<Integer> inputList = new ArrayList<>();
		ArrayList<Integer> outputList = DecoratorBuilder.of(inputList, List.class).make();

		assertThat(inputList, sameInstance(outputList));
	}
}
