/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.AbstractDaprHttpClient;
import io.dapr.actors.Constants;
import io.dapr.exceptions.DaprException;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Http client to call Dapr's API for actors.
 */
//public class DaprHttpAsyncClient implements DaprAsyncClient {
class AppToDaprHttpAsyncClient extends AbstractDaprHttpClient implements AppToDaprAsyncClient {

  private ObjectMapper mapper;

  private Map<String,String> dataMap;


  /**
   * Creates a new instance of {@link AppToDaprHttpAsyncClient}.
   *
   * @param port Port for calling Dapr. (e.g. 3500)
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  public AppToDaprHttpAsyncClient(int port, OkHttpClient httpClient) {
    super(port, httpClient);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<String> getState(String actorType, String actorId, String keyName) {
    String url = String.format(Constants.ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
    return super.invokeAPI("GET", url, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveStateTransactionally(String actorType, String actorId, String data) {
    String url = String.format(Constants.ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    return super.invokeAPIVoid("PUT", url, data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return super.invokeAPIVoid("PUT", url, data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return super.invokeAPIVoid("DELETE", url, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return super.invokeAPIVoid("PUT", url, data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return super.invokeAPIVoid("DELETE", url, null);
  }


  public <T> Mono<T> publishEvent(String topic, String data, String method) throws DaprException {

    if(topic.isEmpty() || topic == null){
      throw new DaprException("500","Topic cannot be null or empty.");
    }

    if(method.isEmpty() || method == null){
      throw new DaprException("500","Method cannot be null or empty.");
    }


    String url = method.equals("POST") ?Constants.PUBLISH_PATH: Constants.PUBLISH_PATH+"/"+topic;

     try{

       dataMap = new HashMap();
       dataMap.put(topic,data);

       String jsonResult = mapper.writerWithDefaultPrettyPrinter()
               .writeValueAsString(dataMap);

       super.invokeAPI(method, url, jsonResult);
     }catch(Exception e){
      throw new RuntimeException(e);
    }
    return Mono.empty();
  }

  public <T> Mono<T> invokeBinding(String name, String data, String method) throws DaprException {

    if(name.isEmpty() || name == null){
      throw new DaprException("500","Name cannot be null or empty.");
    }

    if(method.isEmpty() || method == null){
      throw new DaprException("500","Method cannot be null or empty.");
    }

    String url = method.equals("POST") ?Constants.BINDING_PATH : Constants.BINDING_PATH +"/" +name;

    try{

      dataMap = new HashMap();
      dataMap.put(name,data);

      String jsonResult = mapper.writerWithDefaultPrettyPrinter()
              .writeValueAsString(dataMap);

      super.invokeAPI(method, url, jsonResult);
    }catch(Exception e){
      throw new RuntimeException(e);
    }

    return Mono.empty();
  }



  public <T> Mono<T> getState(String key) throws DaprException {

    if(key.isEmpty() || key == null){
      throw new DaprException("500","Name cannot be null or empty.");
    }

    String url = Constants.STATE_PATH+"/"+key;

    try{
      super.invokeAPI("GET", url, null);
    }catch(Exception e){
      throw new RuntimeException(e);
    }

    return Mono.empty();

  }

  public <T> Mono<T> saveState(String key, String data) throws DaprException {

    if(key.isEmpty() || key == null){
      throw new DaprException("500","Name cannot be null or empty.");
    }

    String url = Constants.STATE_PATH;

    try{

      dataMap = new HashMap();
      dataMap.put(key,data);

      String jsonResult = mapper.writerWithDefaultPrettyPrinter()
              .writeValueAsString(dataMap);

      super.invokeAPI("POST", url, jsonResult);
    }catch(Exception e){
      throw new RuntimeException(e);
    }

    return Mono.empty();

  }

  public <T> Mono<T> deleteState(String key) throws DaprException {

    if(key.isEmpty() || key == null){
      throw new DaprException("500","Name cannot be null or empty.");
    }

    String url = Constants.STATE_PATH+"/"+key;

    try{
      super.invokeAPI("DELETE", url, null);
    }catch(Exception e){
      throw new RuntimeException(e);
    }

    return Mono.empty();

  }

}
