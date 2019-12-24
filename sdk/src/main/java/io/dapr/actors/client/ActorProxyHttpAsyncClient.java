/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dapr.actors.*;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Http client to call actors methods.
 */
public class ActorProxyHttpAsyncClient extends AbstractDaprClient implements ActorProxyAsyncClient {

  /**
   * Shared Json serializer/deserializer as per Jackson's documentation.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private ActorId actorId;
  private String actorType;

  /**
   * Creates a new instance of {@link ActorProxyHttpAsyncClient}.
   *
   * @param port Port for calling Dapr. (e.g. 3500)
   * @param httpClient RestClient used for all API calls in this new instance.
   * @param actorId The actorId associated with the proxy
   * @param actorType actor implementation type of the actor associated with the proxy object.
   */
  ActorProxyHttpAsyncClient(int port, OkHttpClient httpClient,ActorId actorId, String actorType) {
    super(port, httpClient);
    this.setActorId(actorId);
    this.setActorType(actorType);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeActorMethod(String methodName, Object data,  Class<T> clazz) throws JsonProcessingException {

    Mono<String> result=this.invokeActorMethod(actorType,actorId.toString(),methodName,OBJECT_MAPPER.writeValueAsString(data));
    return result
            .filter(s -> (s != null) && (!s.isEmpty()))
            .map(s -> {
              try {
                return OBJECT_MAPPER.readValue(s, clazz);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeActorMethod(String methodName,  Class<T> clazz){

    Mono<String> result=this.invokeActorMethod(actorType,actorId.toString(),methodName,null);
    return result
            .filter(s -> (s != null) && (!s.isEmpty()))
            .map(s -> {
              try {
                return OBJECT_MAPPER.readValue(s, clazz);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Mono invokeActorMethod(String methodName) {

    Mono<String> result=this.invokeActorMethod(actorType,actorId.toString(),methodName,null);
    return result
            .filter(s -> (s != null) && (!s.isEmpty()))
            .map(s -> s);
  }

  @Override
  public Mono invokeActorMethod(String methodName, Object data) throws JsonProcessingException {
    Mono<String> result=this.invokeActorMethod(actorType,actorId.toString(),methodName,OBJECT_MAPPER.writeValueAsString(data));
    return result
            .filter(s -> (s != null) && (!s.isEmpty()))
            .map(s -> s);
  }


  protected Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
    String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
    return super.invokeAPI("PUT", url, jsonPayload);
  }




  /**
   * {@inheritDoc}
   */
  public ActorId getActorId() {
    return actorId;
  }

  private void setActorId(ActorId actorId) {
    this.actorId = actorId;
  }

  /**
   * {@inheritDoc}
   */
  public String getActorType() {
    return actorType;
  }

  private void setActorType(String actorType) {
    this.actorType = actorType;
  }
}
