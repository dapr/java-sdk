/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprClientGrpc;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test State GRPC DAPR capabilities using a DAPR instance with an empty service running
 */
public class GRPCStateClientIT extends BaseIT {

  private static DaprRun daprRun;

  private static DaprClient daprClient;

  @BeforeClass
  public static void init() throws Exception {
    daprRun = startDaprApp(GRPCStateClientIT.class.getSimpleName(), 5000);
    daprRun.switchToGRPC();
    daprClient = new DaprClientBuilder().build();

    assertTrue(daprClient instanceof DaprClientGrpc);
  }

  @AfterClass
  public static void tearDown() throws IOException {
    daprClient.close();
  }


  @Test
  public void saveVerifyAndDeleteTransactionalStateString() {

    //The key use to store the state
    final String stateKey = "myTKey";

    //creation of a dummy data
    String data = "my state 3";

    TransactionalStateOperation<String> operation = createStringTransactionalStateOperation(
        TransactionalStateOperation.OperationType.UPSERT,
        createState(stateKey, null, null, data));

    //create of the deferred call to DAPR to execute the transaction
    Mono<Void> saveResponse = daprClient.executeTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    //execute the save action
    saveResponse.block();

    //create of the deferred call to DAPR to get the state
    Mono<State<String>> response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, null), String.class);

    //retrieve the state
    State<String> myDataResponse = response.block();

    //Assert that the response is the correct one
    Assert.assertNotNull(myDataResponse.getEtag());
    Assert.assertNotNull(myDataResponse.getKey());
    Assert.assertNotNull(myDataResponse.getValue());
    Assert.assertEquals("my state 3", myDataResponse.getValue());
    operation = createStringTransactionalStateOperation(
        TransactionalStateOperation.OperationType.DELETE,
        createState(stateKey, null, null, data));
    //create of the deferred call to DAPR to execute the transaction
    Mono<Void> deleteResponse = daprClient.executeTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    //execute the delete action
    deleteResponse.block();

    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, null), String.class);
    State<String> deletedData = response.block();

    //Review that the response is null, because the state was deleted
    Assert.assertNull(deletedData.getValue());
  }

  @Test
  public void saveVerifyAndDeleteTransactionalState() {

    //The key use to store the state
    final String stateKey = "myTKey";

    //creation of a dummy data
    MyData data = new MyData();
    data.setPropertyA("data in property AA");
    data.setPropertyB("data in property BA");

    TransactionalStateOperation<MyData> operation = createTransactionalStateOperation(
        TransactionalStateOperation.OperationType.UPSERT,
        createState(stateKey, null, null, data));

    Assert.assertNotNull(daprClient);
    //create of the deferred call to DAPR to execute the transaction
    Mono<Void> saveResponse = daprClient.executeTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    //execute the save action
    saveResponse.block();

    //create of the deferred call to DAPR to get the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, null), MyData.class);

    //retrieve the state
    State<MyData> myDataResponse = response.block();

    //Assert that the response is the correct one
    Assert.assertNotNull(myDataResponse.getEtag());
    Assert.assertNotNull(myDataResponse.getKey());
    Assert.assertNotNull(myDataResponse.getValue());
    Assert.assertEquals("data in property AA", myDataResponse.getValue().getPropertyA());
    Assert.assertEquals("data in property BA", myDataResponse.getValue().getPropertyB());

    operation = createTransactionalStateOperation(
        TransactionalStateOperation.OperationType.DELETE,
        createState(stateKey, null, null, data));
    //create of the deferred call to DAPR to execute the transaction
    Mono<Void> deleteResponse = daprClient.executeTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    //execute the delete action
    deleteResponse.block();

    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, null), MyData.class);
    State<MyData> deletedData = response.block();

    //Review that the response is null, because the state was deleted
    Assert.assertNull(deletedData.getValue());
  }

  @Test
  public void saveAndGetState() {

    //The key use to store the state
    final String stateKey = "myKey";

    //create the http client


    //creation of a dummy data
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //create of the deferred call to DAPR to store the state
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save action
    saveResponse.block();

    //create of the deferred call to DAPR to get the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, null), MyData.class);

    //retrieve the state
    State<MyData> myDataResponse = response.block();

    //Assert that the response is the correct one
    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());
  }

  @Test
  public void saveUpdateAndGetState() {

    //The key use to store the state and be updated
    final String stateKey = "keyToBeUpdated";

    //create http DAPR client

    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute save action to DAPR
    saveResponse.block();

    //change data properties
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B2");
    //create deferred action to update the sate without any etag or options
    saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the update action to DAPR
    saveResponse.block();

    //Create deferred action to retrieve the action
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null),
        MyData.class);
    //execute the retrieve of the state
    State<MyData> myDataResponse = response.block();

    //review that the update was success action
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B2", myDataResponse.getValue().getPropertyB());
  }

  @Test
  public void saveAndDeleteState() {
    //The key use to store the state and be deleted
    final String stateKey = "myeKeyToBeDeleted";

    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");
    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null),
        MyData.class);
    //execute the retrieve of the state
    State<MyData> myDataResponse = response.block();

    //review that the state was saved correctly
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //create deferred action to delete the state
    Mono<Void> deleteResponse = daprClient.deleteState(STATE_STORE_NAME, stateKey, null, null);
    //execute the delete action
    deleteResponse.block();

    //Create deferred action to retrieve the state
    response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null), MyData.class);
    //execute the retrieve of the state
    myDataResponse = response.block();

    //review that the action does not return any value, because the state was deleted
    assertNull(myDataResponse.getValue());
  }


  @Test
  public void saveUpdateAndGetStateWithEtag() {
    //The key use to store the state and be updated using etags
    final String stateKey = "keyToBeUpdatedWithEtag";


    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null),
        MyData.class);
    //execute the action for retrieve the state and the etag
    State<MyData> myDataResponse = response.block();

    //review that the etag is not empty
    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    String firstETag = myDataResponse.getEtag();

    //change the data in order to update the state
    data.setPropertyA("data in property A2");
    data.setPropertyB("data in property B2");
    //Create deferred action to update the data using the correct etag
    saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, myDataResponse.getEtag(), data, null);
    saveResponse.block();


    response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null), MyData.class);
    //retrive the data wihout any etag
    myDataResponse = response.block();

    //review that state value changes
    assertNotNull(myDataResponse.getEtag());
    //review that the etag changes after an update
    assertNotEquals(firstETag, myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A2", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B2", myDataResponse.getValue().getPropertyB());
  }

  @Ignore("This test case is ignored because DAPR  ignore the ETag is wrong when is sent from GRPC protocol, the " +
      "execution continues and the state is updated.")
  @Test(expected = RuntimeException.class)
  public void saveUpdateAndGetStateWithWrongEtag() {
    final String stateKey = "keyToBeUpdatedWithWrongEtag";

    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null),
        MyData.class);
    //execute the action for retrieve the state and the etag
    State<MyData> myDataResponse = response.block();

    //review that the etag is not empty
    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    String firstETag = myDataResponse.getEtag();

    //change the data in order to update the state
    data.setPropertyA("data in property A2");
    data.setPropertyB("data in property B2");
    //Create deferred action to update the data using the incorrect etag
    saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, "99999999999999", data, null);
    saveResponse.block();


    response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null), MyData.class);
    //retrive the data wihout any etag
    myDataResponse = response.block();

    //review that state value changes
    assertNotNull(myDataResponse.getEtag());
    //review that the etag changes after an update
    assertNotEquals(firstETag, myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A2", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B2", myDataResponse.getValue().getPropertyB());
  }

  @Test
  public void saveAndDeleteStateWithEtag() {
    final String stateKey = "myeKeyToBeDeletedWithEtag";


    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");
    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to get the state with the etag
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null),
        MyData.class);
    //execute the get state
    State<MyData> myDataResponse = response.block();

    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //Create deferred action to delete an state sending the etag
    Mono<Void> deleteResponse = daprClient.deleteState(STATE_STORE_NAME, stateKey, myDataResponse.getEtag(), null);
    //execute the delete of the state
    deleteResponse.block();

    //Create deferred action to get the sate without an etag
    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, null), MyData.class);
    myDataResponse = response.block();

    //Review that the response is null, because the state was deleted
    assertNull(myDataResponse.getValue());
  }

  @Ignore("This test case is ignored because DAPR  ignore if the ETag is wrong when is sent from GRPC protocol, the " +
      "execution continues and the state is deleted.")
  @Test(expected = RuntimeException.class)
  public void saveAndDeleteStateWithWrongEtag() {
    final String stateKey = "myeKeyToBeDeletedWithWrongEtag";


    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");
    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to get the state with the etag
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<MyData>(stateKey, null, null),
        MyData.class);
    //execute the get state
    State<MyData> myDataResponse = response.block();

    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //Create deferred action to delete an state sending the incorrect etag
    Mono<Void> deleteResponse = daprClient.deleteState(STATE_STORE_NAME, stateKey, "99999999999", null);
    //execute the delete of the state, this should trhow an exception
    deleteResponse.block();

    //Create deferred action to get the sate without an etag
    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, null), MyData.class);
    myDataResponse = response.block();

    //Review that the response is null, because the state was deleted
    assertNull(myDataResponse.getValue());
  }

  @Ignore("This test case is  ignored because it seems that DAPR using GRPC is ignoring the state options for " +
      "consistency and concurrency.")
  @Test(expected = RuntimeException.class)
  public void saveUpdateAndGetStateWithEtagAndStateOptionsFirstWrite() {
    final String stateKey = "keyToBeUpdatedWithEtagAndOptions";

    //create option with concurrency with first writte and consistency of strong
    StateOptions stateOptions = new StateOptions(StateOptions.Consistency.STRONG,
        StateOptions.Concurrency.FIRST_WRITE);


    //create Dummy data
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //create state using stateOptions
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, stateOptions);
    //execute the save state
    saveResponse.block();


    //crate deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, stateOptions),
        MyData.class);
    //execute the retrieve of the state using options
    State<MyData> myDataResponse = response.block();

    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //change data to be udpated
    data.setPropertyA("data in property A2");
    data.setPropertyB("data in property B2");
    //create deferred action to update the action with options
    saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, myDataResponse.getEtag(), data, stateOptions);
    //update the state
    saveResponse.block();


    data.setPropertyA("last write");
    data.setPropertyB("data in property B2");
    //create deferred action to update the action with the same etag
    saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, myDataResponse.getEtag(), data, stateOptions);
    //throws an exception, the state was already udpated
    saveResponse.block();

    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, stateOptions), MyData.class);
    State<MyData> myLastDataResponse = response.block();

    assertNotNull(myLastDataResponse.getEtag());
    assertNotNull(myLastDataResponse.getKey());
    assertNotNull(myLastDataResponse.getValue());
    assertNotNull(myDataResponse.getEtag(), myLastDataResponse.getEtag());
    assertEquals("data in property A2", myLastDataResponse.getValue().getPropertyA());
    assertEquals("data in property B2", myLastDataResponse.getValue().getPropertyB());
  }

  @Test()
  public void saveUpdateAndGetStateWithEtagAndStateOptionsLastWrite() {
    final String stateKey = "keyToBeUpdatedWithEtagAndOptions";

    //create option with concurrency with first writte and consistency of strong
    StateOptions stateOptions = new StateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.LAST_WRITE);


    //create Dummy data
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //create state using stateOptions
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, stateOptions);
    //execute the save state
    saveResponse.block();


    //crate deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, stateOptions),
        MyData.class);
    //execute the retrieve of the state using options
    State<MyData> myDataResponse = response.block();

    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //change data to be udpated
    data.setPropertyA("data in property A2");
    data.setPropertyB("data in property B2");
    //create deferred action to update the action with options
    saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, myDataResponse.getEtag(), data, stateOptions);
    //update the state
    saveResponse.block();


    data.setPropertyA("last write");
    data.setPropertyB("data in property B2");
    //create deferred action to update the action with the same etag
    saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, myDataResponse.getEtag(), data, stateOptions);
    //update the state without an error
    saveResponse.block();

    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey, null, stateOptions), MyData.class);
    State<MyData> myLastDataResponse = response.block();

    assertNotNull(myLastDataResponse.getEtag());
    assertNotNull(myLastDataResponse.getKey());
    assertNotNull(myLastDataResponse.getValue());
    assertNotNull(myDataResponse.getEtag(), myLastDataResponse.getEtag());
    assertEquals("last write", myLastDataResponse.getValue().getPropertyA());
    assertEquals("data in property B2", myLastDataResponse.getValue().getPropertyB());
  }

  private TransactionalStateOperation<MyData> createTransactionalStateOperation(
      TransactionalStateOperation.OperationType type,
      State<MyData> state) {
    return new TransactionalStateOperation<>(type, state);
  }

  private TransactionalStateOperation<String> createStringTransactionalStateOperation(
      TransactionalStateOperation.OperationType type,
      State<String> state) {
    return new TransactionalStateOperation<>(type, state);
  }

  private State<String> createState(String stateKey, String etag, StateOptions options, String data) {
    return new State<>(data, stateKey, etag, options);
  }

  private State<MyData> createState(String stateKey, String etag, StateOptions options, MyData data) {
    return new State<>(data, stateKey, etag, options);
  }


}
