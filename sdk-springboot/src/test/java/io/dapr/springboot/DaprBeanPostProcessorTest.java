/*
 * Copyright 2022 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package io.dapr.springboot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class DaprBeanPostProcessorTest {
  private static final String TOPIC_NAME = "topicName1";

  public static Stream<Arguments> routesTester() {
    return Stream.of(
        Arguments.of(MockController.class, "testMethod1", new String[] {"v1", "v2", "v1/page1", "v2/page1", "v1/page2", "v2/page2"},
            true),
      Arguments.of(MockController.class, "testMethod2", new String[] {"v1", "v2", "v1/page3", "v2/page3", "v1/page4", "v2/page4"},
            true),
        Arguments.of(MockController.class, "testMethod3", new String[] {"v1/foo", "v2/foo"}, true),
        Arguments.of(MockController.class, "testMethod4", new String[] {"v1/foo1", "v2/foo1", "v1/foo2", "v2/foo2"}, true),
        Arguments.of(MockController.class, "testMethod5", new String[] {"v1/" + TOPIC_NAME, "v2/" + TOPIC_NAME}, true),
        Arguments.of(MockControllerNoClazzAnnotation.class, "testMethod1", new String[] {"", "page1", "page2"}, true),
        Arguments.of(MockControllerNoClazzAnnotation.class, "testMethod2", new String[] {"", "page3", "page4"}, true),
        Arguments.of(MockControllerNoClazzAnnotation.class, "testMethod3", new String[] {"foo"}, true),
        Arguments.of(MockControllerNoClazzAnnotation.class, "testMethod4", new String[] {"foo1", "foo2"}, true),
        Arguments.of(MockControllerNoClazzAnnotation.class, "testMethod5", new String[] {TOPIC_NAME}, true)
    );
  }

  @ParameterizedTest
  @MethodSource("routesTester")
  public void testAllPostRoutesGeneration(Class<?> clazzToBeTested, String methodToBeTested, String[] expected,
                                          boolean expectedResult) throws NoSuchMethodException {
    Method allPostRoutesMethod = DaprBeanPostProcessor.class.
        getDeclaredMethod("getAllCompleteRoutesForPost", Class.class, Method.class, String.class);
    allPostRoutesMethod.setAccessible(true);
    List<String> routesArrayTestMethod1 = null;
    try {
      routesArrayTestMethod1 = (List<String>) allPostRoutesMethod.invoke(DaprBeanPostProcessor.class, clazzToBeTested,
          clazzToBeTested.getMethod(methodToBeTested), TOPIC_NAME);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    Assertions.assertEquals(expectedResult,
        testingListForOrderAgnosticEquality(Arrays.asList(expected), routesArrayTestMethod1));
  }

  private boolean testingListForOrderAgnosticEquality(List<?> first, List<?> second) {
    return (first.size() == second.size() && first.containsAll(second) && second.containsAll(first));
  }
}
