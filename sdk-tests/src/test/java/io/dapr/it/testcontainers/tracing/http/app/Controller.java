/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dapr.it.testcontainers.tracing.http.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class Controller {

    @PostMapping(path = "/sleep")
    public void sleep(@RequestBody int seconds) throws InterruptedException {
        if (seconds < 0) {
            throw new IllegalArgumentException("Sleep time cannot be negative.");
        }
        Thread.sleep(seconds * 1000);
    }

    @GetMapping(path = "/health")
    public void health() {
    }
}
