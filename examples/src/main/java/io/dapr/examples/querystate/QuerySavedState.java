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

package io.dapr.examples.querystate;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.query.Query;
import io.dapr.client.domain.query.Sorting;
import io.dapr.client.domain.query.filters.EqFilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. send a message to be saved as state:
 * dapr run --components-path ./components/state -- \
 *   java -Ddapr.api.protocol=HTTP -jar target/dapr-java-sdk-examples-exec.jar \
 *   io.dapr.examples.querystate.QuerySavedState 'my message'
 */
public class QuerySavedState {

  private static final String STATE_STORE_NAME = "mongo-statestore";

  /**
   * Executes the sate actions.
   * @param args messages to be sent as state value.
   */
  public static void main(String[] args) throws Exception {
    DaprClientBuilder builder = new DaprClientBuilder();
    try (DaprClient client = builder.build(); DaprPreviewClient previewClient = builder.buildPreviewClient()) {
      System.out.println("Waiting for Dapr sidecar ...");
      client.waitForSidecar(10000).block();
      System.out.println("Dapr sidecar is ready.");

      Listing first = new Listing();
      first.setPropertyType("apartment");
      first.setId("1000");
      first.setCity("Seattle");
      first.setState("WA");
      Listing second = new Listing();
      second.setPropertyType("row-house");
      second.setId("1002");
      second.setCity("Seattle");
      second.setState("WA");
      Listing third = new Listing();
      third.setPropertyType("apartment");
      third.setId("1003");
      third.setCity("Portland");
      third.setState("OR");
      Listing fourth = new Listing();
      fourth.setPropertyType("apartment");
      fourth.setId("1001");
      fourth.setCity("Portland");
      fourth.setState("OR");
      Map<String, String> meta = new HashMap<>();
      meta.put("contentType", "application/json");

      SaveStateRequest request = new SaveStateRequest(STATE_STORE_NAME).setStates(
          new State<>("1", first, null, meta, null),
          new State<>("2", second, null, meta, null),
          new State<>("3", third, null, meta, null),
          new State<>("4", fourth, null, meta, null)
      );
      client.saveBulkState(request).block();

      System.out.println("Insert key: 1" + ", data: " + first);
      System.out.println("Insert key: 2" + ", data: " + second);
      System.out.println("Insert key: 3" + ", data: " + third);
      System.out.println("Insert key: 4" + ", data: " + fourth);


      Query query = new Query()
          .setFilter(new EqFilter<>("propertyType", "apartment"))
          .setSort(Arrays.asList(new Sorting("id", Sorting.Order.DESC)));

      QueryStateRequest queryStateRequest = new QueryStateRequest(STATE_STORE_NAME)
          .setQuery(query);

      QueryStateResponse<Listing> result = previewClient.queryState(queryStateRequest, Listing.class).block();

      System.out.println("Found " + result.getResults().size() + " items.");
      for (QueryStateItem<Listing> item : result.getResults()) {
        System.out.println("Key: " + item.getKey());
        System.out.println("Data: " + item.getValue());
      }

      // This is an example, so for simplicity we are just exiting here.
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done");
      System.exit(0);
    }
  }
}
