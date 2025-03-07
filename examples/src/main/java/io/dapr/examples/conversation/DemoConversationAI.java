package io.dapr.examples.conversation;

import io.dapr.ai.client.DaprConversationClient;
import io.dapr.ai.client.DaprConversationInput;
import io.dapr.ai.client.DaprConversationResponse;
import io.dapr.v1.DaprProtos;
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
        try (DaprConversationClient client = new DaprConversationClient()) {
            DaprConversationInput daprConversationInput = new DaprConversationInput("11");

            // Component name is the name provided in the metadata block of the conversation.yaml file.
            Mono<DaprConversationResponse> instanceId = client.converse("openai", new ArrayList<>(Collections.singleton(daprConversationInput)), "1234", false, 0.0d);
            System.out.printf("Started a new chaining model workflow with instance ID: %s%n", instanceId);
            DaprConversationResponse response = instanceId.block();

            System.out.println(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}