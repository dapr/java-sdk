package io.dapr.client;

import io.dapr.serializer.DaprObjectSerializer;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DaprClientBuilderTest {

  @Test
  public void build() {
    DaprObjectSerializer objectSerializer = mock(DaprObjectSerializer.class);
    DaprObjectSerializer stateSerializer = mock(DaprObjectSerializer.class);
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder(objectSerializer, stateSerializer);
    DaprClient daprClient = daprClientBuilder.build();
    assertNotNull(daprClient);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildException() {
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder(null,null);
    DaprClient daprClient = daprClientBuilder.build();
    assertNotNull(daprClient);
  }

  @Test
  public void getEnvHttpPortOrDefault() throws Exception {
    DaprObjectSerializer objectSerializer = mock(DaprObjectSerializer.class);
    DaprObjectSerializer stateSerializer = mock(DaprObjectSerializer.class);
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder(objectSerializer, stateSerializer);
    Method method = daprClientBuilder.getClass().getDeclaredMethod("getEnvHttpPortOrDefault", String.class, int.class);
    method.setAccessible(true);
    method.invoke(daprClientBuilder, "PATH", 3000);
  }

  @Test
  public void buildDaprClientHttpTest() throws Exception {
    DaprObjectSerializer objectSerializer = mock(DaprObjectSerializer.class);
    DaprObjectSerializer stateSerializer = mock(DaprObjectSerializer.class);
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder(objectSerializer, stateSerializer);
    setFinalStaticField(DaprClientBuilder.class, "HTTP_PORT", 0);
    assertThrows(IllegalStateException.class, () -> {
      daprClientBuilder.build();
    });
  }

  @Test
  public void buildDaprClientGrpcTest() throws Exception {
    DaprObjectSerializer objectSerializer = mock(DaprObjectSerializer.class);
    DaprObjectSerializer stateSerializer = mock(DaprObjectSerializer.class);
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder(objectSerializer, stateSerializer);
    setFinalStaticField(DaprClientBuilder.class, "GRPC_PORT", 0);
    Method method = daprClientBuilder.getClass().getDeclaredMethod("buildDaprClientGrpc");
    method.setAccessible(true);
    assertThrows(InvocationTargetException.class, () -> {
      method.invoke(daprClientBuilder);
    });
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