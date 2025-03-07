package io.dapr.ai.client;

import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.config.Properties;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class DaprConversationClientTest {

  private DaprGrpc.DaprStub daprStub;

  @Before
  public void initialize() {

    ManagedChannel channel = mock(ManagedChannel.class);
    daprStub = mock(DaprGrpc.DaprStub.class);
    when(daprStub.getChannel()).thenReturn(channel);
    when(daprStub.withInterceptors(Mockito.any(), Mockito.any())).thenReturn(daprStub);
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenComponentNameIsNull() throws Exception {
    try  (DaprConversationClient daprConversationClient = new DaprConversationClient()) {
      List<DaprConversationInput> daprConversationInputs = new ArrayList<>();
      daprConversationInputs.add(new DaprConversationInput("Hello there !"));

      IllegalArgumentException exception =
          Assert.assertThrows(IllegalArgumentException.class, () ->
              daprConversationClient.converse(null, daprConversationInputs).block());
      Assert.assertEquals("Conversation component name cannot be null or empty.", exception.getMessage());
    }
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenConversationComponentIsEmpty() throws Exception {
    try  (DaprConversationClient daprConversationClient = new DaprConversationClient()) {
      List<DaprConversationInput> daprConversationInputs = new ArrayList<>();
      daprConversationInputs.add(new DaprConversationInput("Hello there !"));

      IllegalArgumentException exception =
          Assert.assertThrows(IllegalArgumentException.class, () ->
              daprConversationClient.converse("", daprConversationInputs).block());
      Assert.assertEquals("Conversation component name cannot be null or empty.", exception.getMessage());
    }
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenConversationInputIsEmpty() throws Exception {
    try  (DaprConversationClient daprConversationClient = new DaprConversationClient()) {
      List<DaprConversationInput> daprConversationInputs = new ArrayList<>();

      IllegalArgumentException exception =
          Assert.assertThrows(IllegalArgumentException.class, () ->
              daprConversationClient.converse("openai", daprConversationInputs).block());
      Assert.assertEquals("Conversation inputs cannot be null or empty.", exception.getMessage());
    }
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenConversationInputIsNull() throws Exception {
    try  (DaprConversationClient daprConversationClient =
              new DaprConversationClient(new Properties(), null)) {

      IllegalArgumentException exception =
          Assert.assertThrows(IllegalArgumentException.class, () ->
              daprConversationClient.converse("openai", null).block());
      Assert.assertEquals("Conversation inputs cannot be null or empty.", exception.getMessage());
    }
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenConversationInputContentIsNull() throws Exception {
    try  (DaprConversationClient daprConversationClient = new DaprConversationClient()) {
      List<DaprConversationInput> daprConversationInputs = new ArrayList<>();
      daprConversationInputs.add(new DaprConversationInput(null));

      IllegalArgumentException exception =
          Assert.assertThrows(IllegalArgumentException.class, () ->
              daprConversationClient.converse("openai", daprConversationInputs).block());
      Assert.assertEquals("Conversation input content cannot be null or empty.", exception.getMessage());
    }
  }

  @Test
  public void converseShouldThrowIllegalArgumentExceptionWhenConversationInputContentIsEmpty() throws Exception {
    try  (DaprConversationClient daprConversationClient = new DaprConversationClient()) {
      List<DaprConversationInput> daprConversationInputs = new ArrayList<>();
      daprConversationInputs.add(new DaprConversationInput(""));

      IllegalArgumentException exception =
          Assert.assertThrows(IllegalArgumentException.class, () ->
              daprConversationClient.converse("openai", daprConversationInputs).block());
      Assert.assertEquals("Conversation input content cannot be null or empty.", exception.getMessage());
    }
  }

  @Test
  public void converseShouldReturnConversationResponseWhenRequiredInputsAreValid() throws Exception {
    DaprProtos.ConversationResponse conversationResponse = DaprProtos.ConversationResponse.newBuilder()
            .addOutputs(DaprProtos.ConversationResult.newBuilder().setResult("Hello How are you").build()).build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ConversationResponse> observer = invocation.getArgument(1);
      observer.onNext(conversationResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha1(any(DaprProtos.ConversationRequest.class), any());

    try  (DaprConversationClient daprConversationClient =
              new DaprConversationClient(daprStub, new ResiliencyOptions())) {
      List<DaprConversationInput> daprConversationInputs = new ArrayList<>();
      daprConversationInputs.add(new DaprConversationInput("Hello there"));

      DaprConversationResponse daprConversationResponse =
          daprConversationClient.converse("openai", daprConversationInputs).block();

      ArgumentCaptor<DaprProtos.ConversationRequest> captor =
          ArgumentCaptor.forClass(DaprProtos.ConversationRequest.class);
      verify(daprStub, times(1)).converseAlpha1(captor.capture(), Mockito.any());

      DaprProtos.ConversationRequest conversationRequest = captor.getValue();

      Assert.assertEquals("openai", conversationRequest.getName());
      Assert.assertEquals("Hello there", conversationRequest.getInputs(0).getContent());
      Assert.assertEquals("Hello How are you",
          daprConversationResponse.getDaprConversationOutputs().get(0).getResult());
    }
  }

  @Test
  public void converseShouldReturnConversationResponseWhenRequiredAndOptionalInputsAreValid() throws Exception {
    DaprProtos.ConversationResponse conversationResponse = DaprProtos.ConversationResponse.newBuilder()
        .setContextID("contextId")
        .addOutputs(DaprProtos.ConversationResult.newBuilder().setResult("Hello How are you").build()).build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ConversationResponse> observer = invocation.getArgument(1);
      observer.onNext(conversationResponse);
      observer.onCompleted();
      return null;
    }).when(daprStub).converseAlpha1(any(DaprProtos.ConversationRequest.class), any());

    try (DaprConversationClient daprConversationClient = new DaprConversationClient(daprStub, null)) {
      DaprConversationInput daprConversationInput = new DaprConversationInput("Hello there")
          .setRole(DaprConversationRole.ASSISSTANT)
          .setScrubPii(true);

      List<DaprConversationInput> daprConversationInputs = new ArrayList<>();
      daprConversationInputs.add(daprConversationInput);

      DaprConversationResponse daprConversationResponse =
          daprConversationClient.converse("openai", daprConversationInputs,
              "contextId", true, 1.1d).block();

      ArgumentCaptor<DaprProtos.ConversationRequest> captor =
          ArgumentCaptor.forClass(DaprProtos.ConversationRequest.class);
      verify(daprStub, times(1)).converseAlpha1(captor.capture(), Mockito.any());

      DaprProtos.ConversationRequest conversationRequest = captor.getValue();

      Assert.assertEquals("openai", conversationRequest.getName());
      Assert.assertEquals("contextId", conversationRequest.getContextID());
      Assert.assertTrue(conversationRequest.getScrubPII());
      Assert.assertEquals(1.1d, conversationRequest.getTemperature(), 0d);
      Assert.assertEquals("Hello there", conversationRequest.getInputs(0).getContent());
      Assert.assertTrue(conversationRequest.getInputs(0).getScrubPII());
      Assert.assertEquals(DaprConversationRole.ASSISSTANT.toString(), conversationRequest.getInputs(0).getRole());
      Assert.assertEquals("contextId", daprConversationResponse.getContextId());
      Assert.assertEquals("Hello How are you",
          daprConversationResponse.getDaprConversationOutputs().get(0).getResult());
    }
  }
}
