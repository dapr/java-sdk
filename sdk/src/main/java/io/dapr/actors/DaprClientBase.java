package io.dapr.actors;



//import com.fasterxml.jackson.databind.ObjectMapper;
//import okhttp3.*;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

// base class of hierarchy
public class DaprClientBase {

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
