package io.dapr.exceptions;

import okhttp3.Response;

public class DaprHttpException extends RuntimeException {
    private final Response response;

    public DaprHttpException(Response response) {
        super("Dapr HTTP exception: " + response.code() + " " + response.message());
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}
