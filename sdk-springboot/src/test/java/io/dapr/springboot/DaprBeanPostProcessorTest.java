package io.dapr.springboot;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class DaprBeanPostProcessorTest {

  private final Class<?> clazzToBeTested;
  private final String methodToBeTested;
  private final String[] expected;
  private final boolean expectedResult;
  private static final String TOPIC_NAME = "topicName1";

  public DaprBeanPostProcessorTest(Class<?> clazzToBeTested, String methodToBeTested, String[] expected,
                                   boolean expectedResult) {
    this.clazzToBeTested = clazzToBeTested;
    this.methodToBeTested = methodToBeTested;
    this.expected = expected;
    this.expectedResult = expectedResult;
  }

  @Parameterized.Parameters
  public static Collection<?> routesTester() {
    return Arrays.asList(new Object[][] {
        {MockController.class, "testMethod1", new String[] {"v1", "v2", "v1/page1", "v2/page1", "v1/page2", "v2/page2"},
            true},
        {MockController.class, "testMethod2", new String[] {"v1", "v2", "v1/page3", "v2/page3", "v1/page4", "v2/page4"},
            true},
        {MockController.class, "testMethod3", new String[] {"v1/foo", "v2/foo"}, true},
        {MockController.class, "testMethod4", new String[] {"v1/foo1", "v2/foo1", "v1/foo2", "v2/foo2"}, true},
        {MockController.class, "testMethod5", new String[] {"v1/" + TOPIC_NAME, "v2/" + TOPIC_NAME}, true},
        {MockControllerNoClazzAnnotation.class, "testMethod1", new String[] {"", "page1", "page2"}, true},
        {MockControllerNoClazzAnnotation.class, "testMethod2", new String[] {"", "page3", "page4"}, true},
        {MockControllerNoClazzAnnotation.class, "testMethod3", new String[] {"foo"}, true},
        {MockControllerNoClazzAnnotation.class, "testMethod4", new String[] {"foo1", "foo2"}, true},
        {MockControllerNoClazzAnnotation.class, "testMethod5", new String[] {TOPIC_NAME}, true}
    });
  }

  @Test
  public void testAllPostRoutesGeneration() throws NoSuchMethodException {
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
    Assert.assertEquals(expectedResult,
        testingListForOrderAgnosticEquality(Arrays.asList(expected), routesArrayTestMethod1));
  }

  private boolean testingListForOrderAgnosticEquality(List<?> first, List<?> second) {
    return (first.size() == second.size() && first.containsAll(second) && second.containsAll(first));
  }

}
