package io.dapr.client;

import io.dapr.exceptions.DaprError;
import io.dapr.exceptions.DaprException;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static io.dapr.client.ObjectSerializer.OBJECT_MAPPER;

public class DefaultDaprErrorResponseParser implements DaprErrorResponseParser {

    DaprError error;

    @Override
    public DaprException parse(@NotNull okhttp3.Response response) throws IOException {
        byte[] json = getBodyBytesOrEmptyArray(response);
        DaprException unknownException = new DaprException("UNKNOWN", "HTTP status code: " + response.code());

        if ((json != null) && (json.length != 0)) {
            try {
                error = OBJECT_MAPPER.readValue(json, DaprError.class);
            } catch (IOException e) {
                return new DaprException("UNKNOWN", new String(json, StandardCharsets.UTF_8));
            }
        }

        if (error != null) {
            return new DaprException(error.getErrorCode(), error.getMessage());
        }
        return unknownException;
    }

    private byte[] getBodyBytesOrEmptyArray(okhttp3.Response response) throws IOException {
        ResponseBody body = response.body();
        if (body != null) {
            return body.bytes();
        }

        return EMPTY_BYTES;
    }

    /**
     * Empty input or output.
     */
    private final byte[] EMPTY_BYTES = new byte[0];
}
