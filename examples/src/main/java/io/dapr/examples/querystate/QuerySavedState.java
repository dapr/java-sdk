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
import io.dapr.client.domain.query.Query;
import io.dapr.client.domain.query.Sorting;
import io.dapr.client.domain.query.filters.EqFilter;

import java.util.Arrays;
import java.util.Objects;

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

  public static class MyData {

    /// Gets or sets the value for PropertyA.
    private String propertyA;

    /// Gets or sets the value for PropertyB.
    private String propertyB;

    public String getPropertyB() {
      return propertyB;
    }

    public void setPropertyB(String propertyB) {
      this.propertyB = propertyB;
    }

    public String getPropertyA() {
      return propertyA;
    }

    public void setPropertyA(String propertyA) {
      this.propertyA = propertyA;
    }

    @Override
    public String toString() {
      return "MyData{"
          + "propertyA='" + propertyA + '\''
          + ", propertyB='" + propertyB + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MyData myData = (MyData) o;
      return Objects.equals(propertyA, myData.propertyA)
          && Objects.equals(propertyB, myData.propertyB);
    }

    @Override
    public int hashCode() {
      return Objects.hash(propertyA, propertyB);
    }
  }

  private static final String STATE_STORE_NAME = "mongo-statestore";

  private static final String FIRST_KEY_NAME = "key1";

  private static final String SECOND_KEY_NAME = "key2";

  private static final String THIRD_KEY_NAME = "key3";

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

      String searchVal = args.length == 0 ? "searchValue" : args[0];

      MyData dataQueried = new MyData();
      dataQueried.setPropertyA(searchVal);
      dataQueried.setPropertyB("query");
      MyData dataNotQueried = new MyData();
      dataNotQueried.setPropertyA("no query");
      dataNotQueried.setPropertyB("no query");


      System.out.println("Insert key: " + FIRST_KEY_NAME + ", data: " + dataQueried);
      client.saveState(STATE_STORE_NAME, FIRST_KEY_NAME, dataQueried).block();
      System.out.println("Insert key: " + SECOND_KEY_NAME + ", data: " + dataQueried);
      client.saveState(STATE_STORE_NAME, SECOND_KEY_NAME, dataQueried).block();
      System.out.println("Insert key: " + THIRD_KEY_NAME + ", data: " + dataNotQueried);
      client.saveState(STATE_STORE_NAME, THIRD_KEY_NAME, dataNotQueried).block();
      Query query = new Query().setFilter(new EqFilter<>("propertyA", searchVal))
          .setSort(Arrays.asList(new Sorting("propertyB", Sorting.Order.ASC)));

      QueryStateRequest request = new QueryStateRequest(STATE_STORE_NAME)
          .setQuery(query);


      QueryStateResponse<MyData> result = previewClient.queryState(request, MyData.class).block();

      System.out.println("Found " + result.getResults().size() + " items.");
      for (QueryStateItem<MyData> item : result.getResults()) {
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
