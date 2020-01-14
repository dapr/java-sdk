/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.StateKeyValue;
import io.dapr.it.BaseIT;
import io.dapr.it.services.EmptyService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Mono;

/**
 * Test State HTTP DAPR capabilities using a DAPR instance with an empty service running
 */
public class HttpStateClientIT extends BaseIT {

    @BeforeClass
    public static void init() throws Exception {
        daprIntegrationTestingRunner =
                createDaprIntegrationTestingRunner(
                        "BUILD SUCCESS",
                        EmptyService.class,
                        false,
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
        Mono<Void> saveResponse= daprClient.saveState(stateKey,null,data, null);
        saveResponse.block();

        Mono<StateKeyValue<MyData>> response= daprClient.getState( new StateKeyValue<MyData>(null,stateKey,null),null,MyData.class);
        StateKeyValue<MyData> myDataResponse=response.block();

        Assert.assertEquals("data in property A",myDataResponse.getValue().getPropertyA());
        Assert.assertEquals("data in property B",myDataResponse.getValue().getPropertyB());
    }

    @Test
    public void saveUpdateAndGetState() {
        final String stateKey= "keyToBeUpdated";

        DaprClient daprClient= new DaprClientBuilder().build();
        MyData data= new MyData();
        data.setPropertyA("data in property A");
        data.setPropertyB("data in property B");
        Mono<Void> saveResponse= daprClient.saveState(stateKey,null,data, null);
        saveResponse.block();


        data.setPropertyA("data in property A");
        data.setPropertyB("data in property B2");
        saveResponse= daprClient.saveState(stateKey,null,data, null);
        saveResponse.block();

        Mono<StateKeyValue<MyData>> response= daprClient.getState( new StateKeyValue<MyData>(null,stateKey,null),null,MyData.class);
        StateKeyValue<MyData> myDataResponse=response.block();

        Assert.assertEquals("data in property A",myDataResponse.getValue().getPropertyA());
        Assert.assertEquals("data in property B2",myDataResponse.getValue().getPropertyB());
    }

    @Test
    public void saveAndDeleteState() {
        final String stateKey= "myeKeyToBeDeleted";

        DaprClient daprClient= new DaprClientBuilder().build();
        MyData data= new MyData();
        data.setPropertyA("data in property A");
        data.setPropertyB("data in property B");
        Mono<Void> saveResponse= daprClient.saveState(stateKey,null,data, null);
        saveResponse.block();

        Mono<StateKeyValue<MyData>> response= daprClient.getState( new StateKeyValue<MyData>(null,stateKey,null),null,MyData.class);
        StateKeyValue<MyData> myDataResponse=response.block();

        Assert.assertEquals("data in property A",myDataResponse.getValue().getPropertyA());
        Assert.assertEquals("data in property B",myDataResponse.getValue().getPropertyB());

        Mono<Void> deleteResponse= daprClient.deleteState( new StateKeyValue<MyData>(null,stateKey,null),null);
        deleteResponse.block();

        response= daprClient.getState( new StateKeyValue<MyData>(null,stateKey,null),null,MyData.class);
        myDataResponse=response.block();

        Assert.assertNull(myDataResponse.getValue());

    }

}
