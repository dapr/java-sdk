/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.methodinvoke.http_auth;

import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.codehaus.plexus.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MethodInvokeBasicAuthIT extends BaseIT {

    /**
     * Run of a Dapr application.
     */
    private DaprRun daprRun = null;

    @Before
    public void init() throws Exception {
        daprRun = startDaprApp(
          MethodInvokeBasicAuthIT.class.getSimpleName(),
          MethodInvokeBasicAuthService.SUCCESS_MESSAGE,
          MethodInvokeBasicAuthService.class,
          true,
          30000);

        daprRun.switchToHTTP();

        // Wait since service might be ready even after port is available.
        Thread.sleep(2000);
    }


    @Test
    public void testInvokeWithCreds() throws Exception {
        // Checking if the basic auth header is not included in the second POST call in the chain illustrated below:
        // [Test] ---(HTTP w/ Basic Auth)---> [POST /invoke/headers] ---(Dapr HTTP-to-HTTP invoke)--> [POST /headers]
        String url = String.format("http://localhost:%d/invoke/headers?appId=%s",
            daprRun.getAppPort(),
            daprRun.getAppName());
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders httpHeaders = getHeaders();

        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Map.class);

        assertTrue(responseEntity.hasBody());
        for(Object key : responseEntity.getBody().keySet()) {
            if ("Authorization".equalsIgnoreCase(key.toString())) {
                fail("Authorization is not expected to be included in the headers of receiving service.");
            }
        }
    }

    private static HttpHeaders getHeaders () {
        String adminuserCredentials = "myuser:mypass";
        String encodedCredentials =
            new String(Base64.encodeBase64(adminuserCredentials.getBytes()));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Basic " + encodedCredentials);
        httpHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return httpHeaders;
    }
}
