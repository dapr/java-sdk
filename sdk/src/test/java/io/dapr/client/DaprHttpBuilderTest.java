package io.dapr.client;

import io.dapr.utils.Constants;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;

import static org.mockito.Mockito.mock;

public class DaprHttpBuilderTest {


  @Test
  public void withReadTimeout() throws Exception {
    DaprHttpBuilder daprHttpBuilder = new DaprHttpBuilder();
    Duration duration = mock(Duration.class);
    Method method = daprHttpBuilder.getClass().getDeclaredMethod("withReadTimeout", Duration.class);
    method.setAccessible(true);
    method.invoke(daprHttpBuilder, duration);
    daprHttpBuilder.build();

  }

}