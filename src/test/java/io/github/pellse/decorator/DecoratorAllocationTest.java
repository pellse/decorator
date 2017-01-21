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
package io.github.pellse.decorator;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.base.Stopwatch;

import io.github.pellse.decorator.util.DelegateList;

/**
 * @author Sebastien Pelletier
 *
 */
public class DecoratorAllocationTest {

	@Rule public TestName testName = new TestName();

	@SuppressWarnings("unchecked")
	@Test
	public void testAllocationGeneratedDelegate() throws Exception {

		invoke(list -> Decorator.of(list, List.class).with(InnerDelegateList.class).make(),
				list-> new DelegateList<>(list),
				10_000_000);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAllocationDelegateInvocationHandlerVsDynamicProxy() throws Exception {

		invoke(list -> Decorator.of(list, List.class)
					.with((delegate, method, args) -> method.invoke(delegate, args))
					.make(),
				list -> (List<Object>)Proxy.newProxyInstance(
						Decorator.class.getClassLoader(),
						new Class<?>[] {List.class},
						(proxy, method, args) -> method.invoke(list, args)),
				10_000_000);
	}

	private void invoke(Function<List<Object>, List<Object>> decoratorFunction,
			Function<List<Object>, List<Object>> controlFunction,
			long loopSize) throws Exception {

		List<Object> list = new ArrayList<>();

		Stopwatch watch1 = Stopwatch.createStarted();
		for (int i = 0; i < loopSize; i++) {
			List<Object> decoratorList = decoratorFunction.apply(list);
			decoratorList.add("aaa");
			decoratorList.remove("aaa");
		}
		long timeElapsed1 = watch1.elapsed(TimeUnit.MILLISECONDS);

		Stopwatch watch2 = Stopwatch.createStarted();
		for (int i = 0; i < loopSize; i++) {
			List<Object> controlList = controlFunction.apply(list);
			controlList.add("aaa");
			controlList.remove("aaa");
		}
		long timeElapsed2 = watch2.elapsed(TimeUnit.MILLISECONDS);

		System.out.println("Test name = " + testName.getMethodName() +
				": For " + loopSize + " entries, Time elapsed decorator list = " + timeElapsed1 + "ms, time elapsed control list = " + timeElapsed2 + "ms");
	}

	public static interface InnerDelegateList<E> extends List<E> {

		List<E> getDelegate();

		@Override
		default boolean add(E e) {
			return getDelegate().add(e);
		}
	}
}
