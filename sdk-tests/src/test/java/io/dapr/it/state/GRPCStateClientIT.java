/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.actors.services.EmptyService;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.Assert.*;

/**
 * Test State GRPC DAPR capabilities using a DAPR instance with an empty service running
 */
public class GRPCStateClientIT extends BaseIT {

  private static DaprRun daprRun;

  private static DaprClient daprClient;

  @BeforeClass
  public static void init() throws Exception {
    daprRun = startDaprApp(
      "BUILD SUCCESS",
      EmptyService.class,
      false,
      5000
    );
    daprRun.switchToGRPC();
    daprClient = new DaprClientBuilder(new DefaultObjectSerializer(), new DefaultObjectSerializer()).build();
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
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the save action
    saveResponse.block();

    //create of the deferred call to DAPR to get the state
    Mono<State<MyData>> response = daprClient.getState(new State(stateKey, null, null), MyData.class);

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
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute save action to DAPR
    saveResponse.block();

    //change data properties
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B2");
    //create deferred action to update the sate without any etag or options
    saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the update action to DAPR
    saveResponse.block();

    //Create deferred action to retrieve the action
    Mono<State<MyData>> response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
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
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
    //execute the retrieve of the state
    State<MyData> myDataResponse = response.block();

    //review that the state was saved correctly
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //create deferred action to delete the state
    Mono<Void> deleteResponse = daprClient.deleteState(stateKey, null, null);
    //execute the delete action
    deleteResponse.block();

    //Create deferred action to retrieve the state
    response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
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
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
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
    saveResponse = daprClient.saveState(stateKey, myDataResponse.getEtag(), data, null);
    saveResponse.block();


    response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
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

  @Ignore("This test case is ignored because DAPR  ignore the ETag is wrong when is sent from GRPC protocol, the execution continues and the state is updated.")
  @Test(expected = RuntimeException.class)
  public void saveUpdateAndGetStateWithWrongEtag() {
    final String stateKey = "keyToBeUpdatedWithWrongEtag";

    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
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
    saveResponse = daprClient.saveState(stateKey, "99999999999999", data, null);
    saveResponse.block();


    response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
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
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to get the state with the etag
    Mono<State<MyData>> response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
    //execute the get state
    State<MyData> myDataResponse = response.block();

    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //Create deferred action to delete an state sending the etag
    Mono<Void> deleteResponse = daprClient.deleteState(stateKey, myDataResponse.getEtag(), null);
    //execute the delete of the state
    deleteResponse.block();

    //Create deferred action to get the sate without an etag
    response = daprClient.getState(new State(stateKey, null, null), MyData.class);
    myDataResponse = response.block();

    //Review that the response is null, because the state was deleted
    assertNull(myDataResponse.getValue());
  }

  @Ignore("This test case is ignored because DAPR  ignore if the ETag is wrong when is sent from GRPC protocol, the execution continues and the state is deleted.")
  @Test(expected = RuntimeException.class)
  public void saveAndDeleteStateWithWrongEtag() {
    final String stateKey = "myeKeyToBeDeletedWithWrongEtag";


    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");
    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to get the state with the etag
    Mono<State<MyData>> response = daprClient.getState(new State<MyData>(stateKey, null, null), MyData.class);
    //execute the get state
    State<MyData> myDataResponse = response.block();

    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //Create deferred action to delete an state sending the incorrect etag
    Mono<Void> deleteResponse = daprClient.deleteState(stateKey, "99999999999", null);
    //execute the delete of the state, this should trhow an exception
    deleteResponse.block();

    //Create deferred action to get the sate without an etag
    response = daprClient.getState(new State(stateKey, null, null), MyData.class);
    myDataResponse = response.block();

    //Review that the response is null, because the state was deleted
    assertNull(myDataResponse.getValue());
  }

  @Ignore("This test case is  ignored because it seems that DAPR using GRPC is ignoring the state options for consistency and concurrency.")
  @Test(expected = RuntimeException.class)
  public void saveUpdateAndGetStateWithEtagAndStateOptionsFirstWrite() {
    final String stateKey = "keyToBeUpdatedWithEtagAndOptions";

    //create option with concurrency with first writte and consistency of strong
    StateOptions stateOptions = new StateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.FIRST_WRITE, null);


    //create Dummy data
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //create state using stateOptions
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, stateOptions);
    //execute the save state
    saveResponse.block();


    //crate deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(new State(stateKey, null, stateOptions), MyData.class);
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
    saveResponse = daprClient.saveState(stateKey, myDataResponse.getEtag(), data, stateOptions);
    //update the state
    saveResponse.block();


    data.setPropertyA("last write");
    data.setPropertyB("data in property B2");
    //create deferred action to update the action with the same etag
    saveResponse = daprClient.saveState(stateKey, myDataResponse.getEtag(), data, stateOptions);
    //throws an exception, the state was already udpated
    saveResponse.block();

    response = daprClient.getState(new State(stateKey, null, stateOptions), MyData.class);
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
    StateOptions stateOptions = new StateOptions(StateOptions.Consistency.STRONG, StateOptions.Concurrency.LAST_WRITE, null);


    //create Dummy data
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //create state using stateOptions
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, stateOptions);
    //execute the save state
    saveResponse.block();


    //crate deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(new State(stateKey, null, stateOptions), MyData.class);
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
    saveResponse = daprClient.saveState(stateKey, myDataResponse.getEtag(), data, stateOptions);
    //update the state
    saveResponse.block();


    data.setPropertyA("last write");
    data.setPropertyB("data in property B2");
    //create deferred action to update the action with the same etag
    saveResponse = daprClient.saveState(stateKey, myDataResponse.getEtag(), data, stateOptions);
    //update the state without an error
    saveResponse.block();

    response = daprClient.getState(new State(stateKey, null, stateOptions), MyData.class);
    State<MyData> myLastDataResponse = response.block();

    assertNotNull(myLastDataResponse.getEtag());
    assertNotNull(myLastDataResponse.getKey());
    assertNotNull(myLastDataResponse.getValue());
    assertNotNull(myDataResponse.getEtag(), myLastDataResponse.getEtag());
    assertEquals("last write", myLastDataResponse.getValue().getPropertyA());
    assertEquals("data in property B2", myLastDataResponse.getValue().getPropertyB());
  }

  @Test(timeout = 13000)
  public void saveDeleteWithRetry() {
    final String stateKey = "keyToBeDeleteWithWrongEtagAndRetry";
    StateOptions.RetryPolicy retryPolicy = new StateOptions.RetryPolicy(Duration.ofSeconds(3), 3, StateOptions.RetryPolicy.Pattern.LINEAR);
    StateOptions stateOptions = new StateOptions(null, null, retryPolicy);


    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(new State(stateKey, null, null), MyData.class);
    //execute the action for retrieve the state and the etag
    State<MyData> myDataResponse = response.block();

    //review that the etag is not empty
    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());


    Mono<Void> deleteResponse = daprClient.deleteState(stateKey, "99999999", stateOptions);

    long start = System.currentTimeMillis();
    try {
      //delete action
      deleteResponse.block();
    } catch (RuntimeException ex) {
      assertTrue(ex.getMessage().contains("failed to set value after 3 retries"));
    }
    long end = System.currentTimeMillis();
    System.out.println("DEBUG: Logic A took " + (end - start) + " MilliSeconds");
    long elapsedTime = end - start;
    assertTrue(elapsedTime > 9000 && elapsedTime < 9200);

  }

  @Ignore("Ignored as an issue on DAPR")
  @Test(timeout = 13000)
  public void saveUpdateWithRetry() {
    final String stateKey = "keyToBeDeleteWithWrongEtagAndRetry";
    StateOptions.RetryPolicy retryPolicy = new StateOptions.RetryPolicy(Duration.ofSeconds(4), 3, StateOptions.RetryPolicy.Pattern.LINEAR);
    StateOptions stateOptions = new StateOptions(null, null, retryPolicy);


    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(new State(stateKey, null, null), MyData.class);
    //execute the action for retrieve the state and the etag
    State<MyData> myDataResponse = response.block();

    //review that the etag is not empty
    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B", myDataResponse.getValue().getPropertyB());

    //Create deferred action to save the sate
    saveResponse = daprClient.saveState(stateKey, "9999999", data, stateOptions);
    //execute the save state action
    long start = System.currentTimeMillis();


    try {
      saveResponse.block();
    } catch (RuntimeException ex) {
      assertTrue(ex.getMessage().contains("failed to set value after 3 retries"));
    }
    long end = System.currentTimeMillis();
    System.out.println("DEBUG: Logic A took " + (end - start) + " MilliSeconds");
    long elapsedTime = end - start;
    assertTrue(elapsedTime > 9000 && elapsedTime < 9200);

  }

}
