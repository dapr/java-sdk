package io.dapr.examples.configuration.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.Map;

@RestController
public class ConfigSubscriberController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostMapping(path = "/configuration/{configStore}/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Void> handleMessage(@PathVariable Map<String, String> pathVarsMap, @RequestBody JsonNode obj) {
        return Mono.fromRunnable(() -> {
            try {
                for (Iterator<JsonNode> it = obj.get("items").elements(); it.hasNext(); ) {
                    JsonNode node = it.next();
                    String key = node.path("key").asText();
                    String value = node.path("value").asText();
                    System.out.println(value + " : key ->" + key);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
