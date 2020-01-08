/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.actors.ActorId;
import io.dapr.client.DaprClient;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the state store facade.
 */
public class DaprStateAsyncProviderTest {

  private static final ActorStateSerializer SERIALIZER = new ActorStateSerializer();

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
      .saveActorStateTransactionally(
        eq("MyActor"),
        eq("123"),
        argThat(s -> {
          try {
            JsonNode node = OBJECT_MAPPER.readTree(s);
            if (node == null) {
              return false;
            }

            if (node.size() != 3) {
              return false;
            }

            boolean foundInsertName = false;
            boolean foundUpdateZipcode = false;
            boolean foundDeleteFlag = false;
            for (JsonNode operation : node) {
              if (operation.get("operation") == null) {
                return false;
              }
              if (operation.get("request") == null) {
                return false;
              }

              String opName = operation.get("operation").asText();
              String key = operation.get("request").get("key").asText();
              JsonNode valueNode = operation.get("request").get("value");

              foundInsertName |= "upsert".equals(opName) &&
                "name".equals(key) &&
                "Jon Doe".equals(valueNode.asText());
              foundUpdateZipcode |= "upsert".equals(opName) &&
                "zipcode".equals(key) &&
                "98011".equals(valueNode.asText());
              foundDeleteFlag |= "delete".equals(opName) &&
                "flag".equals(key) &&
                (valueNode == null);
            }

            return foundInsertName && foundUpdateZipcode && foundDeleteFlag;
          } catch (IOException e) {
            e.printStackTrace();
            return false;
          }
        })))
      .thenReturn(Mono.empty());

    DaprStateAsyncProvider provider = new DaprStateAsyncProvider(daprClient, SERIALIZER);
    provider.apply("MyActor",
      new ActorId("123"),
      createInsertChange("name", "Jon Doe"),
      createUpdateChange("zipcode", "98011"),
      createDeleteChange("flag"))
      .block();

    verify(daprClient).saveActorStateTransactionally(eq("MyActor"), eq("123"), any());
  }

  @Test
  public void happyCaseLoad() {
    DaprClient daprClient = mock(DaprClient.class);
    when(daprClient
      .getActorState(any(), any(), eq("name")))
      .thenReturn(Mono.just("Jon Doe"));
    when(daprClient
      .getActorState(any(), any(), eq("zipcode")))
      .thenReturn(Mono.just("98021"));
    when(daprClient
      .getActorState(any(), any(), eq("goals")))
      .thenReturn(Mono.just("98"));
    when(daprClient
      .getActorState(any(), any(), eq("balance")))
      .thenReturn(Mono.just("46.55"));
    when(daprClient
      .getActorState(any(), any(), eq("active")))
      .thenReturn(Mono.just("true"));
    when(daprClient
      .getActorState(any(), any(), eq("customer")))
      .thenReturn(Mono.just("{ \"id\": 1000, \"name\": \"Roxane\"}"));
    when(daprClient
      .getActorState(any(), any(), eq("anotherCustomer")))
      .thenReturn(Mono.just("{ \"id\": 2000, \"name\": \"Max\"}"));
    when(daprClient
      .getActorState(any(), any(), eq("nullCustomer")))
      .thenReturn(Mono.just(""));

    DaprStateAsyncProvider provider = new DaprStateAsyncProvider(daprClient, SERIALIZER);

    Assert.assertEquals("Jon Doe",
      provider.load("MyActor", new ActorId("123"), "name", String.class).block());
    Assert.assertEquals("98021",
      provider.load("MyActor", new ActorId("123"), "zipcode", String.class).block());
    Assert.assertEquals(98,
      (int) provider.load("MyActor", new ActorId("123"), "goals", int.class).block());
    Assert.assertEquals(98,
      (int) provider.load("MyActor", new ActorId("123"), "goals", int.class).block());
    Assert.assertEquals(46.55,
      (double) provider.load("MyActor", new ActorId("123"), "balance", double.class).block(),
      EPSILON);
    Assert.assertEquals(true,
      (boolean) provider.load("MyActor", new ActorId("123"), "active", boolean.class).block());
    Assert.assertEquals(new Customer().setId(1000).setName("Roxane"),
      provider.load("MyActor", new ActorId("123"), "customer", Customer.class).block());
    Assert.assertNotEquals(new Customer().setId(1000).setName("Roxane"),
      provider.load("MyActor", new ActorId("123"), "anotherCustomer", Customer.class).block());
    Assert.assertNull(provider.load("MyActor", new ActorId("123"), "nullCustomer", Customer.class).block());
  }

  @Test
  public void happyCaseContains() {
    DaprClient daprClient = mock(DaprClient.class);

    // Keys that exists.
    when(daprClient
      .getActorState(any(), any(), eq("name")))
      .thenReturn(Mono.just("Jon Doe"));
    when(daprClient
      .getActorState(any(), any(), eq("zipcode")))
      .thenReturn(Mono.just("98021"));
    when(daprClient
      .getActorState(any(), any(), eq("goals")))
      .thenReturn(Mono.just("98"));
    when(daprClient
      .getActorState(any(), any(), eq("balance")))
      .thenReturn(Mono.just("46.55"));
    when(daprClient
      .getActorState(any(), any(), eq("active")))
      .thenReturn(Mono.just("true"));
    when(daprClient
      .getActorState(any(), any(), eq("customer")))
      .thenReturn(Mono.just("{ \"id\": \"3000\", \"name\": \"Ely\" }"));

    // Keys that do not exist.
    when(daprClient
      .getActorState(any(), any(), eq("Does not exist")))
      .thenReturn(Mono.just(""));
    when(daprClient
      .getActorState(any(), any(), eq("NAME")))
      .thenReturn(Mono.just(""));
    when(daprClient
      .getActorState(any(), any(), eq(null)))
      .thenReturn(Mono.just(""));

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
