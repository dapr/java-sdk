/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.actors.ActorId;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the state store facade.
 */
public class DaprStateAsyncProviderTest {

  private static final DaprObjectSerializer SERIALIZER = new DefaultObjectSerializer();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final double EPSILON = 1e-10;

  /**
   * Class used to test JSON serialization.
   */
  public static final class Customer {

    private int id;

    private String name;

    public int getId() {
      return id;
    }

    public Customer setId(int id) {
      this.id = id;
      return this;
    }

    public String getName() {
      return name;
    }

    public Customer setName(String name) {
      this.name = name;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Customer customer = (Customer) o;
      return id == customer.id &&
          Objects.equals(name, customer.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name);
    }

  }

  @Test
  public void happyCaseApply() {
    DaprClient daprClient = mock(DaprClient.class);
    when(daprClient
        .saveStateTransactionally(
            eq("MyActor"),
            eq("123"),
            argThat(operations -> {
              if (operations == null) {
                return false;
              }

              if (operations.size() != 4) {
                return false;
              }

              boolean foundInsertName = false;
              boolean foundUpdateZipcode = false;
              boolean foundDeleteFlag = false;
              boolean foundUpdateBytes = false;
              for (ActorStateOperation operation : operations) {
                if (operation.getOperationType() == null) {
                  return false;
                }
                if (operation.getKey() == null) {
                  return false;
                }

                String opName = operation.getOperationType();
                String key = operation.getKey();
                Object value = operation.getValue();

                foundInsertName |= "upsert".equals(opName) &&
                    "name".equals(key) &&
                    "\"Jon Doe\"".equals(value);
                foundUpdateZipcode |= "upsert".equals(opName) &&
                    "zipcode".equals(key) &&
                    "98011".equals(value);
                foundDeleteFlag |= "delete".equals(opName) &&
                    "flag".equals(key) &&
                    (value == null);
                foundUpdateBytes |= "upsert".equals(opName) &&
                    "bytes".equals(key) &&
                    Arrays.equals(new byte[]{0x1}, (byte[]) value);
              }

              return foundInsertName && foundUpdateZipcode && foundDeleteFlag && foundUpdateBytes;
            })))
        .thenReturn(Mono.empty());

    DaprStateAsyncProvider provider = new DaprStateAsyncProvider(daprClient, SERIALIZER);
    provider.apply("MyActor",
        new ActorId("123"),
        createInsertChange("name", "Jon Doe"),
        createUpdateChange("zipcode", 98011),
        createDeleteChange("flag"),
        createUpdateChange("bytes", new byte[]{0x1}))
        .block();

    verify(daprClient).saveStateTransactionally(eq("MyActor"), eq("123"), any());
  }

  @Test
  public void happyCaseLoad() throws Exception {
    DaprClient daprClient = mock(DaprClient.class);
    when(daprClient
        .getState(any(), any(), eq("name")))
        .thenReturn(Mono.just(SERIALIZER.serialize("Jon Doe")));
    when(daprClient
        .getState(any(), any(), eq("zipcode")))
        .thenReturn(Mono.just(SERIALIZER.serialize(98021)));
    when(daprClient
        .getState(any(), any(), eq("goals")))
        .thenReturn(Mono.just(SERIALIZER.serialize(98)));
    when(daprClient
        .getState(any(), any(), eq("balance")))
        .thenReturn(Mono.just(SERIALIZER.serialize(46.55)));
    when(daprClient
        .getState(any(), any(), eq("active")))
        .thenReturn(Mono.just(SERIALIZER.serialize(true)));
    when(daprClient
        .getState(any(), any(), eq("customer")))
        .thenReturn(Mono.just("{ \"id\": 1000, \"name\": \"Roxane\"}".getBytes()));
    when(daprClient
        .getState(any(), any(), eq("anotherCustomer")))
        .thenReturn(Mono.just("{ \"id\": 2000, \"name\": \"Max\"}".getBytes()));
    when(daprClient
        .getState(any(), any(), eq("nullCustomer")))
        .thenReturn(Mono.empty());
    when(daprClient
        .getState(any(), any(), eq("bytes")))
        .thenReturn(Mono.just("\"QQ==\"".getBytes()));
    when(daprClient
        .getState(any(), any(), eq("emptyBytes")))
        .thenReturn(Mono.just(new byte[0]));

    DaprStateAsyncProvider provider = new DaprStateAsyncProvider(daprClient, SERIALIZER);

    Assert.assertEquals("Jon Doe",
        provider.load("MyActor", new ActorId("123"), "name", TypeRef.STRING).block());
    Assert.assertEquals(98021,
        (int) provider.load("MyActor", new ActorId("123"), "zipcode", TypeRef.INT).block());
    Assert.assertEquals(98,
        (int) provider.load("MyActor", new ActorId("123"), "goals", TypeRef.INT).block());
    Assert.assertEquals(98,
        (int) provider.load("MyActor", new ActorId("123"), "goals", TypeRef.INT).block());
    Assert.assertEquals(46.55,
        (double) provider.load("MyActor", new ActorId("123"), "balance", TypeRef.DOUBLE).block(),
        EPSILON);
    Assert.assertEquals(true,
        (boolean) provider.load("MyActor", new ActorId("123"), "active", TypeRef.BOOLEAN).block());
    Assert.assertEquals(new Customer().setId(1000).setName("Roxane"),
        provider.load("MyActor", new ActorId("123"), "customer", TypeRef.get(Customer.class)).block());
    Assert.assertNotEquals(new Customer().setId(1000).setName("Roxane"),
        provider.load("MyActor", new ActorId("123"), "anotherCustomer", TypeRef.get(Customer.class)).block());
    Assert.assertNull(
        provider.load("MyActor", new ActorId("123"), "nullCustomer", TypeRef.get(Customer.class)).block());
    Assert.assertArrayEquals("A".getBytes(),
        provider.load("MyActor", new ActorId("123"), "bytes", TypeRef.get(byte[].class)).block());
    Assert.assertNull(
        provider.load("MyActor", new ActorId("123"), "emptyBytes", TypeRef.get(byte[].class)).block());
  }

  @Test
  public void happyCaseContains() {
    DaprClient daprClient = mock(DaprClient.class);

    // Keys that exists.
    when(daprClient
        .getState(any(), any(), eq("name")))
        .thenReturn(Mono.just("Jon Doe".getBytes()));
    when(daprClient
        .getState(any(), any(), eq("zipcode")))
        .thenReturn(Mono.just("98021".getBytes()));
    when(daprClient
        .getState(any(), any(), eq("goals")))
        .thenReturn(Mono.just("98".getBytes()));
    when(daprClient
        .getState(any(), any(), eq("balance")))
        .thenReturn(Mono.just("46.55".getBytes()));
    when(daprClient
        .getState(any(), any(), eq("active")))
        .thenReturn(Mono.just("true".getBytes()));
    when(daprClient
        .getState(any(), any(), eq("customer")))
        .thenReturn(Mono.just("{ \"id\": \"3000\", \"name\": \"Ely\" }".getBytes()));

    // Keys that do not exist.
    when(daprClient
        .getState(any(), any(), eq("Does not exist")))
        .thenReturn(Mono.empty());
    when(daprClient
        .getState(any(), any(), eq("NAME")))
        .thenReturn(Mono.empty());
    when(daprClient
        .getState(any(), any(), eq(null)))
        .thenReturn(Mono.empty());

    DaprStateAsyncProvider provider = new DaprStateAsyncProvider(daprClient, SERIALIZER);

    Assert.assertTrue(provider.contains("MyActor", new ActorId("123"), "name").block());
    Assert.assertFalse(provider.contains("MyActor", new ActorId("123"), "NAME").block());
    Assert.assertTrue(provider.contains("MyActor", new ActorId("123"), "zipcode").block());
    Assert.assertTrue(provider.contains("MyActor", new ActorId("123"), "goals").block());
    Assert.assertTrue(provider.contains("MyActor", new ActorId("123"), "balance").block());
    Assert.assertTrue(provider.contains("MyActor", new ActorId("123"), "active").block());
    Assert.assertTrue(provider.contains("MyActor", new ActorId("123"), "customer").block());
    Assert.assertFalse(provider.contains("MyActor", new ActorId("123"), "Does not exist").block());
    Assert.assertFalse(provider.contains("MyActor", new ActorId("123"), null).block());
  }

  private final <T> ActorStateChange createInsertChange(String name, T value) {
    return new ActorStateChange(name, value, ActorStateChangeKind.ADD);
  }

  private final <T> ActorStateChange createUpdateChange(String name, T value) {
    return new ActorStateChange(name, value, ActorStateChangeKind.UPDATE);
  }

  private final ActorStateChange createDeleteChange(String name) {
    return new ActorStateChange(name, null, ActorStateChangeKind.REMOVE);
  }
}
