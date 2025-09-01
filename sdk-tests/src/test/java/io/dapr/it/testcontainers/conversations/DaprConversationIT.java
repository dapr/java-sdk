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

package io.dapr.it.testcontainers.conversations;

import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConversationInput;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationResponse;
import io.dapr.it.testcontainers.DaprPreviewClientConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                DaprPreviewClientConfiguration.class,
                TestConversationApplication.class
        }
)
@Testcontainers
@Tag("testcontainers")
public class DaprConversationIT {

    private static final Network DAPR_NETWORK = Network.newNetwork();
    private static final Random RANDOM = new Random();
    private static final int PORT = RANDOM.nextInt(1000) + 8000;

    @Container
    private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("conversation-dapr-app")
            .withComponent(new Component("echo", "conversation.echo", "v1", new HashMap<>()))
            .withNetwork(DAPR_NETWORK)
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withAppChannelAddress("host.testcontainers.internal")
            .withAppPort(PORT);

    /**
     * Expose the Dapr port to the host.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void daprProperties(DynamicPropertyRegistry registry) {
        registry.add("dapr.http.endpoint", DAPR_CONTAINER::getHttpEndpoint);
        registry.add("dapr.grpc.endpoint", DAPR_CONTAINER::getGrpcEndpoint);
        registry.add("server.port", () -> PORT);
    }

    @Autowired
    private DaprPreviewClient daprPreviewClient;

    @BeforeEach
    public void setUp(){
        org.testcontainers.Testcontainers.exposeHostPorts(PORT);
    }

    @Test
    public void testConversationSDKShouldHaveSameOutputAndInput() {
        ConversationInput conversationInput = new ConversationInput("input this");
        List<ConversationInput> conversationInputList = new ArrayList<>();
        conversationInputList.add(conversationInput);

        ConversationResponse response =
                this.daprPreviewClient.converse(new ConversationRequest("echo", conversationInputList)).block();

        Assertions.assertEquals("", response.getContextId());
        Assertions.assertEquals("input this", response.getConversationOutputs().get(0).getResult());
    }

    @Test
    public void testConversationSDKShouldScrubPIIWhenScrubPIIIsSetInRequestBody() {
        List<ConversationInput> conversationInputList = new ArrayList<>();
        conversationInputList.add(new ConversationInput("input this abcd@gmail.com"));
        conversationInputList.add(new ConversationInput("input this +12341567890"));

        ConversationResponse response =
                this.daprPreviewClient.converse(new ConversationRequest("echo", conversationInputList)
                        .setScrubPii(true)).block();

      Assertions.assertEquals("", response.getContextId());
      Assertions.assertEquals("input this <EMAIL_ADDRESS>\ninput this <PHONE_NUMBER>",
                response.getConversationOutputs().get(0).getResult());
    }

    @Test
    public void testConversationSDKShouldScrubPIIOnlyForTheInputWhereScrubPIIIsSet() {
        List<ConversationInput> conversationInputList = new ArrayList<>();
        conversationInputList.add(new ConversationInput("input this abcd@gmail.com"));
        conversationInputList.add(new ConversationInput("input this +12341567890").setScrubPii(true));

        ConversationResponse response =
                this.daprPreviewClient.converse(new ConversationRequest("echo", conversationInputList)).block();

        Assertions.assertEquals("", response.getContextId());
      Assertions.assertEquals("input this abcd@gmail.com\ninput this <PHONE_NUMBER>",
                response.getConversationOutputs().get(0).getResult());
    }
}
