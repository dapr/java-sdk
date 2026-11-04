/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.client;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.dapr.client.domain.AssistantMessage;
import io.dapr.client.domain.ConversationInput;
import io.dapr.client.domain.ConversationInputAlpha2;
import io.dapr.client.domain.ConversationMessage;
import io.dapr.client.domain.ConversationMessageContent;
import io.dapr.client.domain.ConversationOutput;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationResponse;
import io.dapr.client.domain.ConversationResponseAlpha2;
import io.dapr.client.domain.ConversationResultAlpha2;
import io.dapr.client.domain.ConversationResultChoices;
import io.dapr.client.domain.ConversationResultCompletionUsage;
import io.dapr.client.domain.ConversationResultCompletionUsageDetails;
import io.dapr.client.domain.ConversationResultMessage;
import io.dapr.client.domain.ConversationResultPromptUsageDetails;
import io.dapr.client.domain.ConversationToolCalls;
import io.dapr.client.domain.ConversationToolCallsOfFunction;
import io.dapr.client.domain.ConversationTools;
import io.dapr.client.domain.ConversationToolsFunction;
import io.dapr.client.domain.ToolMessage;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.v1.DaprAiProtos;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs the Conversation API calls of {@link DaprClientImpl} and maps their protos.
 */
final class DaprConversationClient {

  private final DaprGrpcCalls grpcCalls;

  private final DaprObjectSerializer objectSerializer;

  DaprConversationClient(DaprGrpcCalls grpcCalls, DaprObjectSerializer objectSerializer) {
    this.grpcCalls = grpcCalls;
    this.objectSerializer = objectSerializer;
  }

  @Deprecated(forRemoval = true)
  Mono<ConversationResponse> converse(ConversationRequest conversationRequest) {

    try {
      validateConversationRequest(conversationRequest);

      DaprAiProtos.ConversationRequest.Builder protosConversationRequestBuilder = DaprAiProtos.ConversationRequest
          .newBuilder().setTemperature(conversationRequest.getTemperature())
          .setScrubPII(conversationRequest.isScrubPii())
          .setName(conversationRequest.getName());

      if (conversationRequest.getContextId() != null) {
        protosConversationRequestBuilder.setContextID(conversationRequest.getContextId());
      }

      for (ConversationInput input : conversationRequest.getInputs()) {
        if (input.getContent() == null || input.getContent().isEmpty()) {
          throw new IllegalArgumentException("Conversation input content cannot be null or empty.");
        }

        DaprAiProtos.ConversationInput.Builder conversationInputOrBuilder = DaprAiProtos.ConversationInput.newBuilder()
            .setContent(input.getContent())
            .setScrubPII(input.isScrubPii());

        if (input.getRole() != null) {
          conversationInputOrBuilder.setRole(input.getRole().toString());
        }

        protosConversationRequestBuilder.addInputs(conversationInputOrBuilder.build());
      }

      Mono<DaprAiProtos.ConversationResponse> conversationResponseMono = Mono.deferContextual(
          context -> grpcCalls.createMono(
              it -> grpcCalls.intercept(context)
                  .converseAlpha1(protosConversationRequestBuilder.build(), it)
          )
      );

      return conversationResponseMono.map(conversationResponse -> {

        List<ConversationOutput> conversationOutputs = new ArrayList<>();
        for (DaprAiProtos.ConversationResult conversationResult : conversationResponse.getOutputsList()) {
          Map<String, byte[]> parameters = new HashMap<>();
          for (Map.Entry<String, Any> entrySet : conversationResult.getParametersMap().entrySet()) {
            parameters.put(entrySet.getKey(), entrySet.getValue().toByteArray());
          }

          ConversationOutput conversationOutput =
              new ConversationOutput(conversationResult.getResult(), parameters);
          conversationOutputs.add(conversationOutput);
        }

        return new ConversationResponse(conversationResponse.getContextID(), conversationOutputs);
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private void validateConversationRequest(ConversationRequest conversationRequest) {
    if ((conversationRequest.getName() == null) || (conversationRequest.getName().trim().isEmpty())) {
      throw new IllegalArgumentException("LLM name cannot be null or empty.");
    }

    if ((conversationRequest.getInputs() == null) || (conversationRequest
        .getInputs().isEmpty())) {
      throw new IllegalArgumentException("Conversation inputs cannot be null or empty.");
    }
  }

  Mono<ConversationResponseAlpha2> converseAlpha2(ConversationRequestAlpha2 conversationRequestAlpha2) {
    try {
      if ((conversationRequestAlpha2.getName() == null) || (conversationRequestAlpha2.getName().trim().isEmpty())) {
        throw new IllegalArgumentException("LLM name cannot be null or empty.");
      }

      if (conversationRequestAlpha2.getInputs() == null || conversationRequestAlpha2.getInputs().isEmpty()) {
        throw new IllegalArgumentException("Conversation Inputs cannot be null or empty.");
      }

      DaprAiProtos.ConversationRequestAlpha2 protoRequest = buildConversationRequestProto(conversationRequestAlpha2);

      Mono<DaprAiProtos.ConversationResponseAlpha2> conversationResponseMono = Mono.deferContextual(
          context -> grpcCalls.createMono(
              it -> grpcCalls.intercept(context).converseAlpha2(protoRequest, it)
          )
      );

      DaprAiProtos.ConversationResponseAlpha2 conversationResponse = conversationResponseMono.block();

      assert conversationResponse != null;

      List<ConversationResultAlpha2> results = buildConversationResults(conversationResponse.getOutputsList());
      return Mono.just(new ConversationResponseAlpha2(conversationResponse.getContextId(), results));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private DaprAiProtos.ConversationRequestAlpha2 buildConversationRequestProto(ConversationRequestAlpha2 request) {
    DaprAiProtos.ConversationRequestAlpha2.Builder builder = DaprAiProtos.ConversationRequestAlpha2
        .newBuilder()
        .setTemperature(request.getTemperature())
        .setScrubPii(request.isScrubPii())
        .setName(request.getName());

    if (request.getContextId() != null) {
      builder.setContextId(request.getContextId());
    }

    if (request.getToolChoice() != null) {
      builder.setToolChoice(request.getToolChoice());
    }


    if (request.getTools() != null) {
      for (ConversationTools tool : request.getTools()) {
        builder.addTools(buildConversationTools(tool));
      }
    }

    if (request.getMetadata() != null) {
      builder.putAllMetadata(request.getMetadata());
    }

    if (request.getParameters() != null) {
      Map<String, Any> parameters = request.getParameters()
          .entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> {
                try {
                  return Any.newBuilder().setValue(ByteString.copyFrom(objectSerializer.serialize(e.getValue())))
                      .build();
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              })
          );
      builder.putAllParameters(parameters);
    }

    for (ConversationInputAlpha2 input : request.getInputs()) {
      DaprAiProtos.ConversationInputAlpha2.Builder inputBuilder = DaprAiProtos.ConversationInputAlpha2
              .newBuilder()
              .setScrubPii(input.isScrubPii());

      if (input.getMessages() != null) {
        for (ConversationMessage message : input.getMessages()) {
          DaprAiProtos.ConversationMessage protoMessage = buildConversationMessage(message);
          inputBuilder.addMessages(protoMessage);
        }
      }

      builder.addInputs(inputBuilder.build());
    }

    if (request.getResponseFormat() != null) {
      builder.setResponseFormat(request.getResponseFormat());
    }

    if (request.getPromptCacheRetention() != null) {
      Duration javaDuration = request.getPromptCacheRetention();
      builder.setPromptCacheRetention(
          com.google.protobuf.Duration.newBuilder()
              .setSeconds(javaDuration.getSeconds())
              .setNanos(javaDuration.getNano())
              .build()
      );
    }
    
    return builder.build();
  }

  private DaprAiProtos.ConversationTools buildConversationTools(ConversationTools tool) {
    ConversationToolsFunction function = tool.getFunction();

    DaprAiProtos.ConversationToolsFunction.Builder protoFunction = DaprAiProtos.ConversationToolsFunction.newBuilder()
        .setName(function.getName());

    if (function.getDescription() != null) {
      protoFunction.setDescription(function.getDescription());
    }

    if (function.getParameters() != null) {
      Map<String, Value> functionParams = function.getParameters()
          .entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> {
                try {
                  return ProtobufValueHelper.toProtobufValue(e.getValue());
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              }
          ));

      protoFunction.setParameters(Struct.newBuilder().putAllFields(functionParams).build());
    }

    return DaprAiProtos.ConversationTools.newBuilder().setFunction(protoFunction).build();
  }

  private DaprAiProtos.ConversationMessage buildConversationMessage(ConversationMessage message) {
    DaprAiProtos.ConversationMessage.Builder messageBuilder = DaprAiProtos.ConversationMessage.newBuilder();

    switch (message.getRole()) {
      case TOOL:
        DaprAiProtos.ConversationMessageOfTool.Builder toolMessage =
            DaprAiProtos.ConversationMessageOfTool.newBuilder();
        if (message.getName() != null) {
          toolMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          toolMessage.addAllContent(getConversationMessageContent(message));
        }
        if (((ToolMessage)message).getToolId() != null) {
          toolMessage.setToolId(((ToolMessage)message).getToolId());
        }
        messageBuilder.setOfTool(toolMessage);
        break;
      case USER:
        DaprAiProtos.ConversationMessageOfUser.Builder userMessage =
            DaprAiProtos.ConversationMessageOfUser.newBuilder();
        if (message.getName() != null) {
          userMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          userMessage.addAllContent(getConversationMessageContent(message));
        }
        messageBuilder.setOfUser(userMessage);
        break;
      case ASSISTANT:
        DaprAiProtos.ConversationMessageOfAssistant.Builder assistantMessage =
            DaprAiProtos.ConversationMessageOfAssistant.newBuilder();

        if (message.getName() != null) {
          assistantMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          assistantMessage.addAllContent(getConversationMessageContent(message));
        }
        if (((AssistantMessage)message).getToolCalls() != null) {
          assistantMessage.addAllToolCalls(getConversationToolCalls((AssistantMessage)message));
        }
        messageBuilder.setOfAssistant(assistantMessage);
        break;
      case DEVELOPER:
        DaprAiProtos.ConversationMessageOfDeveloper.Builder developerMessage =
            DaprAiProtos.ConversationMessageOfDeveloper.newBuilder();
        if (message.getName() != null) {
          developerMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          developerMessage.addAllContent(getConversationMessageContent(message));
        }
        messageBuilder.setOfDeveloper(developerMessage);
        break;
      case SYSTEM:
        DaprAiProtos.ConversationMessageOfSystem.Builder systemMessage =
            DaprAiProtos.ConversationMessageOfSystem.newBuilder();
        if (message.getName() != null) {
          systemMessage.setName(message.getName());
        }
        if (message.getContent() != null) {
          systemMessage.addAllContent(getConversationMessageContent(message));
        }
        messageBuilder.setOfSystem(systemMessage);
        break;
      default:
        throw new IllegalArgumentException("No role of type " + message.getRole() + " found");
    }

    return messageBuilder.build();
  }

  private List<ConversationResultAlpha2> buildConversationResults(
      List<DaprAiProtos.ConversationResultAlpha2> protoResults) {
    List<ConversationResultAlpha2> results = new ArrayList<>();

    for (DaprAiProtos.ConversationResultAlpha2 protoResult : protoResults) {
      List<ConversationResultChoices> choices = new ArrayList<>();

      for (DaprAiProtos.ConversationResultChoices protoChoice : protoResult.getChoicesList()) {
        ConversationResultMessage message = buildConversationResultMessage(protoChoice);
        choices.add(new ConversationResultChoices(protoChoice.getFinishReason(), protoChoice.getIndex(), message));
      }

      results.add(new ConversationResultAlpha2(
          choices,
          protoResult.getModel(),
          getConversationResultCompletionUsage(protoResult))
      );
    }
    
    return results;
  }

  private static ConversationResultCompletionUsage getConversationResultCompletionUsage(
      DaprAiProtos.ConversationResultAlpha2 protoResult) {
    var usage = new ConversationResultCompletionUsage(
        protoResult.getUsage().getCompletionTokens(),
        protoResult.getUsage().getPromptTokens(),
        protoResult.getUsage().getTotalTokens());

    usage.setCompletionTokenDetails(new ConversationResultCompletionUsageDetails(
        protoResult.getUsage().getCompletionTokensDetails().getAcceptedPredictionTokens(),
        protoResult.getUsage().getCompletionTokensDetails().getAudioTokens(),
        protoResult.getUsage().getCompletionTokensDetails().getReasoningTokens(),
        protoResult.getUsage().getCompletionTokensDetails().getRejectedPredictionTokens()));

    usage.setPromptTokenDetails(new ConversationResultPromptUsageDetails(
        protoResult.getUsage().getPromptTokensDetails().getAudioTokens(),
        protoResult.getUsage().getPromptTokensDetails().getCachedTokens()
    ));
    return usage;
  }

  private ConversationResultMessage buildConversationResultMessage(DaprAiProtos.ConversationResultChoices protoChoice) {
    if (!protoChoice.hasMessage()) {
      return null;
    }

    List<ConversationToolCalls> toolCalls = new ArrayList<>();

    for (DaprAiProtos.ConversationToolCalls protoToolCall : protoChoice.getMessage().getToolCallsList()) {
      ConversationToolCallsOfFunction function = null;
      if (protoToolCall.hasFunction()) {
        function = new ConversationToolCallsOfFunction(
            protoToolCall.getFunction().getName(),
            protoToolCall.getFunction().getArguments()
        );
      }

      ConversationToolCalls conversationToolCalls = new ConversationToolCalls(function);
      conversationToolCalls.setId(protoToolCall.getId());

      toolCalls.add(conversationToolCalls);
    }
    
    return new ConversationResultMessage(protoChoice.getMessage().getContent(), toolCalls
    );
  }

  private List<DaprAiProtos.ConversationMessageContent> getConversationMessageContent(
      ConversationMessage conversationMessage) {

    List<DaprAiProtos.ConversationMessageContent> conversationMessageContents = new ArrayList<>();
    for (ConversationMessageContent conversationMessageContent: conversationMessage.getContent()) {
      conversationMessageContents.add(DaprAiProtos.ConversationMessageContent.newBuilder()
          .setText(conversationMessageContent.getText())
          .build());
    }

    return conversationMessageContents;
  }

  private List<DaprAiProtos.ConversationToolCalls> getConversationToolCalls(
      AssistantMessage assistantMessage) {
    List<DaprAiProtos.ConversationToolCalls> conversationToolCalls = new ArrayList<>();
    for (ConversationToolCalls conversationToolCall: assistantMessage.getToolCalls()) {
      DaprAiProtos.ConversationToolCalls.Builder toolCallsBuilder = DaprAiProtos.ConversationToolCalls.newBuilder()
          .setFunction(DaprAiProtos.ConversationToolCallsOfFunction.newBuilder()
                  .setName(conversationToolCall.getFunction().getName())
                  .setArguments(conversationToolCall.getFunction().getArguments())
                  .build());
      if (conversationToolCall.getId() != null) {
        toolCallsBuilder.setId(conversationToolCall.getId());
      }

      conversationToolCalls.add(toolCallsBuilder.build());
    }

    return conversationToolCalls;
  }
}
