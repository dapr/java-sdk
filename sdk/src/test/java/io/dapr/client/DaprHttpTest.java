/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

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

import static org.junit.Assert.assertEquals;


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
    public void invokePostMethod() throws IOException {

        mockInterceptor.addRule()
                .post("http://localhost:3500/v1.0/state")
                .respond(EXPECTED_RESULT);

        DaprHttp daprHttp = new DaprHttp("http://localhost",3500,okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("POST","v1.0/state",null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT,body);

    }

    @Test
    public void invokeDeleteMethod() throws IOException {

        mockInterceptor.addRule()
                .delete("http://localhost:3500/v1.0/state")
                .respond(EXPECTED_RESULT);

        DaprHttp daprHttp = new DaprHttp("http://localhost",3500,okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("DELETE","v1.0/state",null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT,body);

    }

    @Test
    public void invokeGetMethod() throws IOException {

        mockInterceptor.addRule()
                .get("http://localhost:3500/v1.0/get")
                .respond(EXPECTED_RESULT);

        DaprHttp daprHttp = new DaprHttp("http://localhost",3500,okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("GET","v1.0/get",null);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT,body);

    }

    @Test
    public void invokeMethodWithHeaders() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put("header","value");
        headers.put("header1","value1");

        mockInterceptor.addRule()
                .get("http://localhost:3500/v1.0/get")
                .respond(EXPECTED_RESULT);
        DaprHttp daprHttp = new DaprHttp("http://localhost",3500,okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("GET","v1.0/get",headers);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT,body);

    }

    @Test(expected = RuntimeException.class)
    public void invokeMethodRuntimeException() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put("header","value");
        headers.put("header1","value1");

        mockInterceptor.addRule()
                .get("http://localhost:3500/v1.0/get")
                .respond(500, ResponseBody.create(MediaType.parse("application/json"),
                        "{\"errorCode\":\"500\",\"message\":\"Error\"}"));

        DaprHttp daprHttp = new DaprHttp("http://localhost",3500,okHttpClient);

        Mono<DaprHttp.Response> mono = daprHttp.invokeAPI("GET","v1.0/get",headers);
        DaprHttp.Response response = mono.block();
        String body = serializer.deserialize(response.getBody(), String.class);
        assertEquals(EXPECTED_RESULT,body);
    }

}