/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import io.dapr.utils.ObjectSerializer;
import okhttp3.*;
import okhttp3.mock.Behavior;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


public class DaprHttpTest {

    private OkHttpClient okHttpClient;

    private MockInterceptor mockInterceptor;

    private ObjectSerializer serializer = new ObjectSerializer();

    private final String EXPECTED_RESULT = "{\"data\":\"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"}";

    @Before
    public void setUp() throws Exception {
        mockInterceptor = new MockInterceptor(Behavior.UNORDERED);
        okHttpClient = new OkHttpClient.Builder().addInterceptor(mockInterceptor).build();
    }

    @Test
    public void invokeMethod() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "text/html");
        headers.put("header1", "value1");

        mockInterceptor.addRule()
                .post("http://localhost:3500/v1.0/state")
                .respond(EXPECTED_RESULT);

        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);
        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("POST", "v1.0/state", null, (byte[])null, headers);

        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT, body);
    }

    @Test
    public void invokePostMethod() throws IOException {

        mockInterceptor.addRule()
                .post("http://localhost:3500/v1.0/state")
                .respond(EXPECTED_RESULT)
                .addHeader("Header","Value");

        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("POST","v1.0/state",null,"", null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT, body);

    }

    @Test
    public void invokeDeleteMethod() throws IOException {

        mockInterceptor.addRule()
                .delete("http://localhost:3500/v1.0/state")
                .respond(EXPECTED_RESULT);

        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("DELETE","v1.0/state", null, (String) null,null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT, body);

    }

    @Test
    public void invokeGetMethod() throws IOException {

        mockInterceptor.addRule()
                .get("http://localhost:3500/v1.0/get")
                .respond(EXPECTED_RESULT);

        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("GET","v1.0/get",null, null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT, body);

    }

    @Test
    public void invokeMethodWithHeaders() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put("header", "value");
        headers.put("header1", "value1");

        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.put("orderId", "41");

        mockInterceptor.addRule()
                .get("http://localhost:3500/v1.0/state/order?orderId=41")
                .respond(EXPECTED_RESULT);
        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("GET","v1.0/state/order", urlParameters, headers);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT, body);

    }

    @Test(expected = RuntimeException.class)
    public void invokePostMethodRuntime() throws IOException {

        mockInterceptor.addRule()
                .post("http://localhost:3500/v1.0/state")
                .respond(500);

        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("POST","v1.0/state",null, null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT, body);

    }

    @Test(expected = RuntimeException.class)
    public void invokePostDaprError() throws IOException {

        mockInterceptor.addRule()
                .post("http://localhost:3500/v1.0/state")
                .respond(500, ResponseBody.create(MediaType.parse("text"),
                        "{\"errorCode\":null,\"message\":null}"));

        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("POST","v1.0/state",null, null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT, body);

    }

    @Test(expected = RuntimeException.class)
    public void invokePostMethodUnknownError() throws IOException {

        mockInterceptor.addRule()
                .post("http://localhost:3500/v1.0/state")
                .respond(500, ResponseBody.create(MediaType.parse("application/json"),
                        "{\"errorCode\":\"null\",\"message\":\"null\"}"));

        DaprHttp daprHttp = new DaprHttp(3500,okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("POST","v1.0/state",null, null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT, body);

    }

    @Test
    public void getHeadersAndStatus(){
        mockInterceptor.addRule()
                .post("http://localhost:3500/v1.0/state")
                .respond(500, ResponseBody.create(MediaType.parse("application/json"),
                        "{\"errorCode\":\"null\",\"message\":\"null\"}"));
        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);
        System.out.println(daprHttp);
    }

    @Test ()
    public void testCallbackCalledAtTheExpectedTimeTest() throws IOException {
        String deletedStateKey = "deletedKey";
        String existingState = "existingState";
        String urlDeleteState = Constants.STATE_PATH + "/" + deletedStateKey;
        String urlExistingState = Constants.STATE_PATH + "/" + existingState;
        mockInterceptor.addRule()
            .get("http://localhost:3500/" + urlDeleteState)
            .respond(200, ResponseBody.create(MediaType.parse("application/json"),
               deletedStateKey));
        mockInterceptor.addRule()
            .delete("http://localhost:3500/" + urlDeleteState)
            .respond(204);
        mockInterceptor.addRule()
            .get("http://localhost:3500/" +urlExistingState)
            .respond(200, ResponseBody.create(MediaType.parse("application/json"),
                existingState));
        DaprHttp daprHttp = new DaprHttp(3500, okHttpClient);
        Mono<DaprHttp.Response> response = daprHttp.invokeAPI("GET", urlExistingState, null, null);
        assertEquals(existingState, serializer.deserialize(response.block().getBody(), String.class));
        Mono<DaprHttp.Response> responseDeleted = daprHttp.invokeAPI("GET", urlDeleteState, null, null);
        Mono<DaprHttp.Response> responseDeleteKey = daprHttp.invokeAPI("DELETE", urlDeleteState, null, null);
        assertEquals("", serializer.deserialize(responseDeleteKey.block().getBody(), String.class));
        mockInterceptor.reset();
        mockInterceptor.addRule()
            .get("http://localhost:3500/" +urlDeleteState)
            .respond(404, ResponseBody.create(MediaType.parse("application/json"),
                "{\"errorCode\":\"404\",\"message\":\"State Not Fuund\"}"));
        try {
            responseDeleted.block();
            fail("Expected DaprException");
        } catch (Exception ex) {
            assertEquals(DaprException.class, ex.getCause().getCause().getClass());
        }
    }

}
