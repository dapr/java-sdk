package io.dapr.examples.conversation;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConversationInput;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;

public class DemoConversationAI {
    /**
     * The main method to start the client.
     *
     * @param args Input arguments (unused).
     */
    public static void main(String[] args) {
        try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
            ConversationInput daprConversationInput = new ConversationInput("Hello How are you ?");

            // Component name is the name provided in the metadata block of the conversation.yaml file.
            Mono<ConversationResponse> instanceId = client.converse(new ConversationRequest("openai", new ArrayList<>(Collections.singleton(daprConversationInput)))
                .setContextId("contextId")
                .setScrubPii(true).setTemperature(1.1d));
            System.out.printf("Started a new chaining model workflow with instance ID: %s%n", instanceId);
            ConversationResponse response = instanceId.block();

            System.out.println(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}