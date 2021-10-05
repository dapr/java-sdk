/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.methodinvoke.http_auth;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class MethodInvokeController {

    private static final DaprClient CLIENT = new DaprClientBuilder().build();

    @PostMapping("/headers")
    public ResponseEntity<?> headers(@RequestHeader Map<String, String> headers){
        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    @PostMapping("/invoke/headers")
    public Mono<ResponseEntity<?>> invokeHeaders(@RequestParam(name = "appId")String appId){
        return CLIENT.invokeMethod(appId, "headers", HttpExtension.POST, new HashMap<>(), byte[].class)
            .map(r -> new ResponseEntity<>(r, HttpStatus.OK)
        );
    }
}
