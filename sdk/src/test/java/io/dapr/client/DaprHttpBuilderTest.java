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
  public void build() throws Exception {
    DaprHttpBuilder daprHttpBuilder = new DaprHttpBuilder();
    Duration duration = mock(Duration.class);
    Method method = daprHttpBuilder.getClass().getDeclaredMethod("withReadTimeout", Duration.class);
    method.setAccessible(true);
    method.invoke(daprHttpBuilder, duration);
    daprHttpBuilder.build();
  }

  @Test
  public void getEnvPortOrDefaultTest() throws Exception {
    DaprHttpBuilder daprHttpBuilder = new DaprHttpBuilder();
    setFinalStaticField(Constants.class,"ENV_DAPR_HTTP_PORT","PATH");
    Method method = daprHttpBuilder.getClass().getDeclaredMethod("getEnvPortOrDefault");
    method.setAccessible(true);
    method.invoke(daprHttpBuilder);
    daprHttpBuilder.build();
  }

  private static void setFinalStaticField(Class<?> clazz, String fieldName, Object value)
      throws ReflectiveOperationException {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, value);
  }

}