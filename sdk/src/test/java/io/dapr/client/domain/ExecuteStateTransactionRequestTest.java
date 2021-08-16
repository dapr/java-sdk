package io.dapr.client.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

public class ExecuteStateTransactionRequestTest {

  private String STORE_NAME = "STORE";

  @Test
  public void testSetMetadata(){
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequest(STORE_NAME);
    // Null check
    request.setMetadata(null);
    assertNull(request.getMetadata());
    // Modifiability check
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "testval");
    request.setMetadata(metadata);
    Map<String, String> initial = request.getMetadata();
    request.setMetadata(metadata);
    assertNotSame("Should not be same map", request.getMetadata(), initial);
  }

  @Test
  public void testSetOperations(){
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequest(STORE_NAME);
    // Null check
    request.setOperations(null);
    assertNull(request.getOperations());
    // Modifiability check
    List<TransactionalStateOperation<?>> operations = new ArrayList<>();
    operations.add(new TransactionalStateOperation<>(TransactionalStateOperation.OperationType.DELETE, new State<>("test")));
    request.setOperations(operations);
    List<TransactionalStateOperation<?>> initial = request.getOperations();
    request.setOperations(operations);
    assertNotSame("Should not be same list", request.getOperations(), initial);
  }
}
