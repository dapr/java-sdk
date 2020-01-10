/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.StateKeyValue;
import io.dapr.it.DaprIntegrationTestingRunner;
import io.dapr.it.services.EmptyService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Test State HTTP DAPR capabilities using a DAPR instance with an empty service running
 */
public class HttpStateClientIT {


    private static DaprIntegrationTestingRunner daprIntegrationTestingRunner;

    @BeforeClass
    public static void init() throws IOException, InterruptedException, TimeoutException, ExecutionException {
        daprIntegrationTestingRunner =
                DaprIntegrationTestingRunner.createDaprIntegrationTestingRunner(
                        "BUILD SUCCESS",
                        EmptyService.class,
                        false,
                        3500,
                        0
                );
        daprIntegrationTestingRunner.initializeDapr();
    }

    @Test
    public void saveAndGetState() {
        final String stateKey= "myKey";

        DaprClient daprClient= new DaprClientBuilder().build();
        MyData data= new MyData();
        data.setPropertyA("data in property A");
        data.setPropertyB("data in property B");
        //todo if I set the etag as "eTag" the save fails, but is nor reported.
        Mono<Void> saveResponse= daprClient.saveState(stateKey,"",data, null);
        saveResponse.block();

        Mono<MyData> response= daprClient.getState( new StateKeyValue<MyData>(null,stateKey,null),null,MyData.class);
        MyData myDataResponse=response.block();

        Assert.assertEquals("data in property A",myDataResponse.getPropertyA());
        Assert.assertEquals("data in property B",myDataResponse.getPropertyB());
    }


    @AfterClass
    public static void cleanUp() throws IOException {
        daprIntegrationTestingRunner.destroyDapr();
    }


    static class MyData {

        /// Gets or sets the value for PropertyA.
        private String propertyA;

        /// Gets or sets the value for PropertyB.
        private String propertyB;

        private MyData myData;


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
            return "MyData{" +
                    "propertyA='" + propertyA + '\'' +
                    ", propertyB='" + propertyB + '\'' +
                    '}';
        }

        public MyData getMyData() {
            return myData;
        }

        public void setMyData(MyData myData) {
            this.myData = myData;
        }
    }
}
