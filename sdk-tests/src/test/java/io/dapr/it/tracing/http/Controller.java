package io.dapr.it.tracing.http;

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
}
