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

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import com.google.common.base.Stopwatch;

import io.github.pellse.decorator.DecoratorTest.ListStaticSubclass;
import io.github.pellse.decorator.util.DelegateList;

/**
 * @author Sebastien Pelletier
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DecoratorPerformanceTest {

	@Rule public TestName testName = new TestName();

	@SuppressWarnings("unchecked")
	@Test
	public void testPerformanceGeneratedDelegate() throws Exception {

		List<Object> delegateList1 = Decorator.of(new ArrayList<>(), List.class).with(ListStaticSubclass.class).make();
		List<Object> delegateList2 = new DelegateList<>(new ArrayList<>());

		invoke(delegateList1, delegateList2, 1_000_000_000, 100_000);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPerformanceDelegateInvocationHandlerVsDynamicProxy() throws Exception {
		List<Object> delegateList1 = Decorator.of(new ArrayList<>(), List.class)
				.with((delegate, method, args) -> method.invoke(delegate, args))
				.make();

		List<Object> delegate = new ArrayList<>();
		List<Object> delegateList2 = (List<Object>)Proxy.newProxyInstance(
	            Decorator.class.getClassLoader(),
	            new Class<?>[] {List.class},
	            (proxy, method, args) -> method.invoke(delegate, args));

		invoke(delegateList1, delegateList2, 1_000_000_000, 100_000);
	}

	private void invoke(List<Object> decoratorList, List<Object> controlList, long loopSize, long warmupLoopSize) throws Exception {
		for (int i = 0; i < warmupLoopSize; i++) {
			decoratorList.add("aaa");
			decoratorList.remove("aaa");
		}

		Thread.sleep(2000);

		for (int i = 0; i < warmupLoopSize; i++) {
			controlList.add("aaa");
			controlList.remove("aaa");
		}

		Thread.sleep(2000);

		Stopwatch watch1 = Stopwatch.createStarted();
		for (int i = 0; i < loopSize; i++) {
			decoratorList.add("aaa");
			decoratorList.remove("aaa");
		}
		long timeElapsed1 = watch1.elapsed(TimeUnit.MILLISECONDS);

		Thread.sleep(2000);

		Stopwatch watch2 = Stopwatch.createStarted();
		for (int i = 0; i < loopSize; i++) {
			controlList.add("aaa");
			controlList.remove("aaa");
		}
		long timeElapsed2 = watch2.elapsed(TimeUnit.MILLISECONDS);

		System.out.println("Test name = " + testName.getMethodName() +
			": For " + loopSize + " entries, Time elapsed decorator list = " + timeElapsed1 + "ms, time elapsed control list = " + timeElapsed2 + "ms");
	}
}
