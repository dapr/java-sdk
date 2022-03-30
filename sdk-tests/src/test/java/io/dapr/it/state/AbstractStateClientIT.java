/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.query.Query;
import io.dapr.client.domain.query.Sorting;
import io.dapr.client.domain.query.filters.EqFilter;
import io.dapr.exceptions.DaprException;
import io.dapr.it.BaseIT;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Common test cases for Dapr client (GRPC and HTTP).
 */
public abstract class AbstractStateClientIT extends BaseIT {
  private static final Logger logger = Logger.getLogger(AbstractStateClientIT.class.getName());

  @Test
  public void saveAndGetState() {

    //The key use to store the state
    final String stateKey = "myKey";

    //create the http client
    DaprClient daprClient = buildDaprClient();

    //creation of a dummy data
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //create of the deferred call to DAPR to store the state
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save action
    saveResponse.block();

    //create of the deferred call to DAPR to get the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State(stateKey), MyData.class);

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
  public void getStateKeyNotFound() {
    final String stateKey = "unknownKey";

    DaprClient daprClient = buildDaprClient();

    State<String> state = (State<String>)
        daprClient.getState(STATE_STORE_NAME, new State(stateKey), String.class).block();
    assertNotNull(state);
    assertEquals("unknownKey", state.getKey());
    assertNull(state.getValue());
    assertNull(state.getEtag());
  }

  @Test
  public void saveAndGetBulkState() {
    final String stateKeyOne = UUID.randomUUID().toString();
    final String stateKeyTwo = UUID.randomUUID().toString();
    final String stateKeyThree = "NotFound";

    DaprClient daprClient = buildDaprClient();

    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //saves the states.
    daprClient.saveState(STATE_STORE_NAME, stateKeyOne, "1", data, null).block();
    daprClient.saveState(STATE_STORE_NAME, stateKeyTwo, null, null, null).block();

    //retrieves states in bulk.
    Mono<List<State<MyData>>> response =
        daprClient.getBulkState(STATE_STORE_NAME, Arrays.asList(stateKeyOne, stateKeyTwo, stateKeyThree), MyData.class);
    List<State<MyData>> result = response.block();

    //Assert that the response is the correct one
    assertEquals(3, result.size());
    assertEquals(stateKeyOne, result.stream().findFirst().get().getKey());
    assertEquals(data, result.stream().findFirst().get().getValue());
    assertEquals("1", result.stream().findFirst().get().getEtag());
    assertNull(result.stream().findFirst().get().getError());

    assertEquals(stateKeyTwo, result.stream().skip(1).findFirst().get().getKey());
    assertNull(result.stream().skip(1).findFirst().get().getValue());
    assertEquals("1", result.stream().skip(1).findFirst().get().getEtag());
    assertNull(result.stream().skip(1).findFirst().get().getError());

    assertEquals(stateKeyThree, result.stream().skip(2).findFirst().get().getKey());
    assertNull(result.stream().skip(2).findFirst().get().getValue());
    assertNull(result.stream().skip(2).findFirst().get().getEtag());
    assertNull(result.stream().skip(2).findFirst().get().getError());
  }

  @Test
  public void saveAndQueryAndDeleteState() throws JsonProcessingException {
    final String stateKeyOne = UUID.randomUUID().toString();
    final String stateKeyTwo = UUID.randomUUID().toString();
    final String stateKeyThree = UUID.randomUUID().toString();
    final String commonSearchValue = UUID.randomUUID().toString();
    Map<String,String> meta = new HashMap<>();
    meta.put("contentType", "application/json");

    DaprClient daprClient = buildDaprClient();
    DaprPreviewClient previewApiClient = (DaprPreviewClient) daprClient;

    //saves the states.
    MyData data = new MyData();
    data.setPropertyA(commonSearchValue);
    data.setPropertyB("query");
    State<MyData> state = new State<>(stateKeyOne, data, null, meta, null );
    SaveStateRequest request = new SaveStateRequest(QUERY_STATE_STORE).setStates(state);
    daprClient.saveBulkState(request).block();
    data = new MyData();
    data.setPropertyA(commonSearchValue);
    data.setPropertyB("query");
    state = new State<>(stateKeyTwo, data, null, meta, null );
    request = new SaveStateRequest(QUERY_STATE_STORE).setStates(state);
    daprClient.saveBulkState(request).block();
    data = new MyData();
    data.setPropertyA("CA");
    data.setPropertyB("no query");
    state = new State<>(stateKeyThree, data, null, meta, null );
    request = new SaveStateRequest(QUERY_STATE_STORE).setStates(state);
    daprClient.saveBulkState(request).block();


    QueryStateRequest queryStateRequest = new QueryStateRequest(QUERY_STATE_STORE);
    Query query = new Query().setFilter(new EqFilter<>("propertyA", commonSearchValue))
            .setSort(Arrays.asList(new Sorting("propertyB", Sorting.Order.ASC)));
    queryStateRequest.setQuery(query).setMetadata(meta);

    Mono<QueryStateResponse<MyData>> response = previewApiClient.queryState(queryStateRequest, MyData.class);
    QueryStateResponse<MyData>  result = response.block();

    // Assert that the response is not null
    assertNotNull(result);
    List<QueryStateItem<MyData>> items = result.getResults();
    assertNotNull(items);

    QueryStateItem<MyData> item;
    //Assert that the response is the correct one
    assertEquals(2, items.size());
    assertTrue(items.stream().anyMatch(f -> f.getKey().equals(stateKeyOne)));
    item = items.stream().filter(f -> f.getKey().equals(stateKeyOne)).findFirst().get();
    assertNotNull(item);
    assertEquals(commonSearchValue, item.getValue().getPropertyA());
    assertEquals("query", item.getValue().getPropertyB());
    assertNull(item.getError());

    assertTrue(items.stream().anyMatch(f -> f.getKey().equals(stateKeyTwo)));
    item = items.stream().filter(f -> f.getKey().equals(stateKeyTwo)).findFirst().get();
    assertEquals(commonSearchValue, item.getValue().getPropertyA());
    assertEquals("query", item.getValue().getPropertyB());
    assertNull(item.getError());

    assertFalse(items.stream().anyMatch(f -> f.getKey().equals(stateKeyThree)));

    assertEquals(2L, items.stream().filter(f -> f.getValue().getPropertyB().equals("query")).count());

    //delete all states
    daprClient.deleteState(QUERY_STATE_STORE, stateKeyOne).block();
    daprClient.deleteState(QUERY_STATE_STORE, stateKeyTwo).block();
    daprClient.deleteState(QUERY_STATE_STORE, stateKeyThree).block();
  }

  @Test
  public void saveUpdateAndGetState() {

    //The key use to store the state and be updated
    final String stateKey = "keyToBeUpdated";

    //create http DAPR client
    DaprClient daprClient = buildDaprClient();
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
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null),
        MyData.class);
    //execute the retrieve of the state
    State<MyData> myDataResponse = response.block();

    //review that the update was success action
    assertNotNull("expected non null response", myDataResponse);
    assertEquals("data in property A", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property B2", myDataResponse.getValue().getPropertyB());
  }

  @Test
  public void saveAndDeleteState() {
    //The key use to store the state and be deleted
    final String stateKey = "myeKeyToBeDeleted";

    //create DAPR client
    DaprClient daprClient = buildDaprClient();

    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");
    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null),
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
    response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null), MyData.class);
    //execute the retrieve of the state
    myDataResponse = response.block();

    //review that the action does not return any value, because the state was deleted
    assertNull(myDataResponse.getValue());
  }


  @Test
  public void saveUpdateAndGetStateWithEtag() {
    //The key use to store the state and be updated using etags
    final String stateKey = "keyToBeUpdatedWithEtag";
    //create DAPR client
    DaprClient daprClient = buildDaprClient();
    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null),
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


    response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null), MyData.class);
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
  public void saveUpdateAndGetNullStateWithEtag() {
    // The key use to store the state and be updated using etags
    final String stateKey = UUID.randomUUID().toString();
    DaprClient daprClient = buildDaprClient();
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    // Get state to validate case for key not found.
    State<MyData> stateNotFound = daprClient.getState(STATE_STORE_NAME, stateKey, MyData.class).block();
    assertEquals(stateKey, stateNotFound.getKey());
    assertNull(stateNotFound.getValue());
    assertNull(stateNotFound.getEtag());
    assertNull(stateNotFound.getOptions());
    assertNull(stateNotFound.getError());
    assertEquals(0, stateNotFound.getMetadata().size());

    // Set non null value
    daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null).block();

    // Get state to validate case for key with value.
    State<MyData> stateFound = daprClient.getState(STATE_STORE_NAME, stateKey, MyData.class).block();
    assertEquals(stateKey, stateFound.getKey());
    assertNotNull(stateFound.getValue());
    assertEquals("data in property A", stateFound.getValue().getPropertyA());
    assertEquals("data in property B", stateFound.getValue().getPropertyB());
    assertNotNull(stateFound.getEtag());
    assertFalse(stateFound.getEtag().isEmpty());
    assertNull(stateFound.getOptions());
    assertNull(stateFound.getError());
    assertEquals(0, stateFound.getMetadata().size());

    // Set to null value
    daprClient.saveState(STATE_STORE_NAME, stateKey, null, null, null).block();

    // Get state to validate case for key not found.
    State<MyData> stateNullValue = daprClient.getState(STATE_STORE_NAME, stateKey, MyData.class).block();
    assertEquals(stateKey, stateNullValue.getKey());
    assertNull(stateNullValue.getValue());
    assertNotNull(stateNullValue.getEtag());
    assertFalse(stateNullValue.getEtag().isEmpty());
    assertNull(stateNullValue.getOptions());
    assertNull(stateNullValue.getError());
    assertEquals(0, stateNullValue.getMetadata().size());
  }

  @Test(expected = RuntimeException.class)
  public void saveUpdateAndGetStateWithWrongEtag() {
    final String stateKey = "keyToBeUpdatedWithWrongEtag";

    //create DAPR client
    DaprClient daprClient = buildDaprClient();
    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");

    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to retrieve the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null),
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


    response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null), MyData.class);
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
    //create DAPR client
    DaprClient daprClient = buildDaprClient();
    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");
    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to get the state with the etag
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null),
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
    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey), MyData.class);
    myDataResponse = response.block();

    //Review that the response is null, because the state was deleted
    assertNull(myDataResponse.getValue());
  }


  @Test(expected = RuntimeException.class)
  public void saveAndDeleteStateWithWrongEtag() {
    final String stateKey = "myeKeyToBeDeletedWithWrongEtag";

    //create DAPR client
    DaprClient daprClient = buildDaprClient();
    //Create dummy data to be store
    MyData data = new MyData();
    data.setPropertyA("data in property A");
    data.setPropertyB("data in property B");
    //Create deferred action to save the sate
    Mono<Void> saveResponse = daprClient.saveState(STATE_STORE_NAME, stateKey, null, data, null);
    //execute the save state action
    saveResponse.block();

    //Create deferred action to get the state with the etag
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State<>(stateKey, (MyData) null, null),
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
    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey), MyData.class);
    myDataResponse = response.block();

    //Review that the response is null, because the state was deleted
    assertNull(myDataResponse.getValue());
  }

  @Test(expected = RuntimeException.class)
  public void saveUpdateAndGetStateWithEtagAndStateOptionsFirstWrite() {
    final String stateKey = "keyToBeUpdatedWithEtagAndOptions";

    //create option with concurrency with first writte and consistency of strong
    StateOptions stateOptions = new StateOptions(StateOptions.Consistency.STRONG,
        StateOptions.Concurrency.FIRST_WRITE);

    //create dapr client
    DaprClient daprClient = buildDaprClient();
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

    //create dapr client
    DaprClient daprClient = buildDaprClient();
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

  @Test
  public void saveVerifyAndDeleteTransactionalStateString() {

    //create dapr client
    DaprClient daprClient = buildDaprClient();
    //The key use to store the state
    final String stateKey = "myTKey";

    //creation of a dummy data
    String data = "my state 3";

    TransactionalStateOperation<String> operation = createTransactionalStateOperation(
        TransactionalStateOperation.OperationType.UPSERT,
        createState(stateKey, null, null, data));

    //create of the deferred call to DAPR to execute the transaction
    Mono<Void> saveResponse = daprClient.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    //execute the save action
    saveResponse.block();

    //create of the deferred call to DAPR to get the state
    Mono<State<String>> response = daprClient.getState(STATE_STORE_NAME, new State(stateKey), String.class);

    //retrieve the state
    State<String> myDataResponse = response.block();

    //Assert that the response is the correct one
    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("my state 3", myDataResponse.getValue());
    operation = createTransactionalStateOperation(
        TransactionalStateOperation.OperationType.DELETE,
        createState(stateKey, null, null, data));
    //create of the deferred call to DAPR to execute the transaction
    Mono<Void> deleteResponse = daprClient.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    //execute the delete action
    deleteResponse.block();

    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey), String.class);
    State<String> deletedData = response.block();

    //Review that the response is null, because the state was deleted
    assertNull(deletedData.getValue());
  }

  @Test
  public void saveVerifyAndDeleteTransactionalState() {

    //create dapr client
    DaprClient daprClient = buildDaprClient();
    //The key use to store the state
    final String stateKey = "myTKey";

    //creation of a dummy data
    MyData data = new MyData();
    data.setPropertyA("data in property AA");
    data.setPropertyB("data in property BA");

    TransactionalStateOperation<MyData> operation = createTransactionalStateOperation(
        TransactionalStateOperation.OperationType.UPSERT,
        createState(stateKey, null, null, data));

    assertNotNull(daprClient);
    //create of the deferred call to DAPR to execute the transaction
    Mono<Void> saveResponse = daprClient.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    //execute the save action
    saveResponse.block();

    //create of the deferred call to DAPR to get the state
    Mono<State<MyData>> response = daprClient.getState(STATE_STORE_NAME, new State(stateKey), MyData.class);

    //retrieve the state
    State<MyData> myDataResponse = response.block();

    //Assert that the response is the correct one
    assertNotNull(myDataResponse.getEtag());
    assertNotNull(myDataResponse.getKey());
    assertNotNull(myDataResponse.getValue());
    assertEquals("data in property AA", myDataResponse.getValue().getPropertyA());
    assertEquals("data in property BA", myDataResponse.getValue().getPropertyB());

    operation = createTransactionalStateOperation(
        TransactionalStateOperation.OperationType.DELETE,
        createState(stateKey, null, null, data));
    //create of the deferred call to DAPR to execute the transaction
    Mono<Void> deleteResponse = daprClient.executeStateTransaction(STATE_STORE_NAME, Collections.singletonList(operation));
    //execute the delete action
    deleteResponse.block();

    response = daprClient.getState(STATE_STORE_NAME, new State(stateKey), MyData.class);
    State<MyData> deletedData = response.block();

    //Review that the response is null, because the state was deleted
    assertNull(deletedData.getValue());
  }

  @Test
  public void testInvalidEtag() {
    DaprClient daprClient = buildDaprClient();
    DaprException exception = assertThrows(DaprException.class,
        () -> daprClient.saveState(STATE_STORE_NAME, "myKey", "badEtag", "value", null).block());
    assertNotNull(exception.getMessage());
    // This will assert that eTag is parsed correctly and is not corrupted. The quotation from runtime helps here.
    assertTrue(exception.getMessage().contains("\"badEtag\""));
  }

  private <T> TransactionalStateOperation<T> createTransactionalStateOperation(
      TransactionalStateOperation.OperationType type,
      State<T> state) {
    return new TransactionalStateOperation<>(type, state);
  }

  private <T> State<T> createState(String stateKey, String etag, StateOptions options, T data) {
    return new State<>(stateKey, data, etag, options);
  }

  protected abstract DaprClient buildDaprClient();

}
