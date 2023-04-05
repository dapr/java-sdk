package io.dapr.client;

import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static io.dapr.client.ObjectSerializer.OBJECT_MAPPER;

public class DefaultDaprErrorResponseParser implements DaprErrorResponseParser {

    DaprError error;

    @Override
    public DaprException parse(int statusCode, byte[] response) {
        String errorMessage = (response == null || new String(response).isEmpty()) ? "HTTP status code: " + statusCode : new String(response, StandardCharsets.UTF_8);
        String errorCode = "UNKNOWN";
        DaprException unknownException = new DaprException(errorCode, errorMessage);

        if ((response != null) && (response.length != 0)) {
            try {
                error = OBJECT_MAPPER.readValue(response, DaprError.class);
            } catch (IOException e) {
                return new DaprException("UNKNOWN", new String(response, StandardCharsets.UTF_8));
            }
        }

        if (error != null) {
            errorMessage = error.getMessage() == null ? errorMessage : error.getMessage();
            errorCode = error.getErrorCode() == null ? errorCode : error.getErrorCode();
            return new DaprException(errorCode, errorMessage);
        }
        return unknownException;
    }
}
