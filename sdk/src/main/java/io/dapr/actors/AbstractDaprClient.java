package io.dapr.actors;



import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

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
     * The base url used for form urls.  This is typically "http://localhost:3500".
     */
    private final String baseUrl;

    /**
     * Http client used for all API calls.
     */
    private final OkHttpClient httpClient;

    /**
     * Creates a new instance of {@link AbstractDaprClient}.
     * @param port Port for calling Dapr. (e.g. 3500)
     * @param httpClient RestClient used for all API calls in this new instance.
     */
    public AbstractDaprClient(int port, OkHttpClient httpClient)
    {
        this.baseUrl = String.format("http://%s:%d/", Constants.DEFAULT_HOSTNAME, port);;
        this.httpClient = httpClient;
    }

    // common methods
    /**
     * Invokes an API asynchronously that returns Void.
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
     * @param method HTTP method.
     * @param urlString url as String.
     * @param json JSON payload or null.
     * @return Asynchronous text
     */
    protected final Mono<String> invokeAPI(String method, String urlString, String json) {
        return Mono.fromSupplier(() -> {
            try {
                return tryInvokeAPI(method, urlString, json);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Invokes an API synchronously and returns a text payload.
     * @param method HTTP method.
     * @param urlString url as String.
     * @param json JSON payload or null.
     * @return text
     */
    protected final String tryInvokeAPI(String method, String urlString, String json) throws IOException {
        String requestId = UUID.randomUUID().toString();
        RequestBody body = json != null ? RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, json) : REQUEST_BODY_EMPTY_JSON;

        Request request = new Request.Builder()
                .url(new URL(this.baseUrl + urlString))
                .method(method, body)
                .addHeader(Constants.HEADER_DAPR_REQUEST_ID, requestId)
                .build();

        // TODO: make this call async as well.
        Response response = this.httpClient.newCall(request).execute();
        if (!response.isSuccessful())
        {
            DaprError error = parseDaprError(response.body().string());
            if ((error != null) && (error.getErrorCode() != null) && (error.getMessage() != null))  {
                throw new DaprException(error);
            }

            throw new DaprException("UNKNOWN", String.format("Dapr's Actor API %s failed with return code %d %s", urlString, response.code()));
        }

        return response.body().string();
    }

    /**
     * Tries to parse an error from Dapr response body.
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
}
