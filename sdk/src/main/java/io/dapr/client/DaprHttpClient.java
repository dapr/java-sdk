/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import okhttp3.*;
import reactor.core.publisher.Mono;

// base class of hierarchy
public abstract class AbstractDaprClient {

  /**
   * Defines the standard application/json type for HTTP calls in Dapr.
   */
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json; charset=utf-8");

  /**
   * Shared object representing an empty request body in JSON.
   */
  private static final RequestBody REQUEST_BODY_EMPTY_JSON = RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, "");

  /**
   * JSON Object Mapper.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * The base url used for form urls. This is typically "http://localhost:3500".
   */
  private final String baseUrl;

  /**
   * Http client used for all API calls.
   */
  private final OkHttpClient httpClient;

  /**
   * Creates a new instance of {@link DaprHttpClient}.
   *
   * @param port Port for calling Dapr. (e.g. 3500)
   * @param httpClient RestClient used for all API calls in this new instance.
   */
  public DaprHttpClient(int port, OkHttpClient httpClient) {
    this.baseUrl = String.format("http://%s:%d/", Constants.DEFAULT_HOSTNAME, port);;
    this.httpClient = httpClient;
  }

  // common methods
  /**
   * Invokes an API asynchronously that returns Void.
   *
   * @param method HTTP method.
   * @param urlString url as String.
   * @param json JSON payload or null.
   * @return Asynchronous Void
   */
  protected final Mono<Void> invokeAPIVoid(String method, String urlString, String json) {
    return this.invokeAPI(method, urlString, json).then();
  }

  /**
   * Invokes an API asynchronously that returns a text payload.
   *
   * @param method HTTP method.
   * @param urlString url as String.
   * @param json JSON payload or null.
   * @return Asynchronous text
   */
  public final Mono<String> invokeAPI(String method, String urlString, String json) throws RuntimeException {

    DaprHttpCallback cb = new DaprHttpCallback() {

      @Override
      public void onFailure(Call call, Exception e) {
        Mono.error(e);
      }

      @Override
      public void onSuccess(String response) {
        Mono.just(response);
      }
    };
    try {
      tryInvokeAPI(method, urlString, json, cb);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return Mono.empty();
  }

  /**
   * Invokes an API asynchronously and returns a text payload.
   *
   * @param method HTTP method.
   * @param urlString url as String.
   * @param json JSON payload or null.
   * @return text
   */
  private final void tryInvokeAPI(String method, String urlString, String json, final DaprHttpCallback cb) throws IOException, DaprException {
    String requestId = UUID.randomUUID().toString();
    RequestBody body = json != null ? RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, json) : REQUEST_BODY_EMPTY_JSON;

    Request request = new Request.Builder()
      .url(new URL(this.baseUrl + urlString))
      .method(method, body)
      .addHeader(Constants.HEADER_DAPR_REQUEST_ID, requestId)
      .build();

    this.httpClient.newCall(request).enqueue(new Callback() {

      @Override
      public void onFailure(Call call, IOException e) {
        cb.onFailure(call, e);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        try (ResponseBody responseBody = response.body()) {
          if (!response.isSuccessful()) {
            DaprError error = parseDaprError(response.body().string());
            response.close();
            if ((error != null) && (error.getErrorCode() != null) && (error.getMessage() != null)) {
              throw new DaprException(error);
            }
          } else {
            String respBodyString = responseBody.string();
            cb.onSuccess(respBodyString);
            response.close();
          }
        }
      }
    });

  }

  /**
   * Tries to parse an error from Dapr response body.
   *
   * @param json Response body from Dapr.
   * @return DaprError or null if could not parse.
   */
  protected static DaprError parseDaprError(String json) {
    if (json == null) {
      return null;
    }

    try {
      return OBJECT_MAPPER.readValue(json, DaprError.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public interface DaprHttpCallback {

    /**
     * Called when the server response was not 2xx or when an exception was
     * thrown in the process
     *
     * @param call - in case of server error (4xx, 5xx) this contains the server
     * response in case of IO exception this is null
     * @param e - contains the exception. in case of server error (4xx, 5xx)
     * this is null
     */
    public void onFailure(Call call, Exception e);

    /**
     * Contains the server response
     *
     * @param response Success response.
     */
    public void onSuccess(String response);
  }

}
