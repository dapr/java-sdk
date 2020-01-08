/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.runtime.Dapr;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * SpringBoot Controller to handle callback APIs for Dapr.
 * TODO: use POJOs instead of String when possible.
 * TODO: JavaDocs.
 */
@RestController
public class DaprController {

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @GetMapping("/")
  public String index() {
    return "Greetings from Dapr!";
  }

  @GetMapping("/dapr/config")
  public String daprConfig() throws Exception {
    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeArrayFieldStart("entities");
      for (String actorClass : ActorRuntime.getInstance().getRegisteredActorTypes()) {
        generator.writeString(actorClass);
      }
      generator.writeEndArray();
      // TODO: handle configuration.
      generator.writeEndObject();
      generator.close();
      writer.flush();
      return writer.toString();
    }
  }

  @GetMapping("/dapr/subscribe")
  public String daprSubscribe() throws Exception {
    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartArray();
      for (String topic : Dapr.getInstance().getSubscribedTopics()) {
        generator.writeString(topic);
      }
      generator.writeEndArray();
      generator.close();
      writer.flush();
      return writer.toString();
    }
  }

  @PostMapping(path = "/{name}")
  public Mono<byte[]> invokeMethodOrTopic(@PathVariable("name") String name,
                                          @RequestBody(required = false) byte[] body,
                                          @RequestHeader Map<String, String> header) {
    return Dapr.getInstance().handleInvocation(name, body, header);
  }

  @PutMapping(path = "/{name}")
  public Mono<byte[]> invokeMethodOrTopicViaPut(@PathVariable("name") String name,
                                          @RequestBody(required = false) byte[] body,
                                          @RequestHeader Map<String, String> header) {
    return Dapr.getInstance().handleInvocation(name, body, header);
  }

  @PostMapping(path = "/actors/{type}/{id}")
  public Mono<Void> activateActor(@PathVariable("type") String type,
                                  @PathVariable("id") String id) throws Exception {
    return ActorRuntime.getInstance().activate(type, id);
  }

  @DeleteMapping(path = "/actors/{type}/{id}")
  public Mono<Void> deactivateActor(@PathVariable("type") String type,
                                    @PathVariable("id") String id) throws Exception {
    return ActorRuntime.getInstance().deactivate(type, id);
  }

  @PutMapping(path = "/actors/{type}/{id}/method/{method}")
  public Mono<String> invokeActorMethod(@PathVariable("type") String type,
                                        @PathVariable("id") String id,
                                        @PathVariable("method") String method,
                                        @RequestBody(required = false) String body) {
    try {
      String data = findMethodData(body);
      return ActorRuntime.getInstance().invoke(type, id, method, data).map(r -> buildResponse(r));
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  @PutMapping(path = "/actors/{type}/{id}/method/timer/{timer}")
  public Mono<Void> invokeActorTimer(@PathVariable("type") String type,
                                     @PathVariable("id") String id,
                                     @PathVariable("timer") String timer) {
    return ActorRuntime.getInstance().invokeTimer(type, id, timer);
  }

  @PutMapping(path = "/actors/{type}/{id}/method/remind/{reminder}")
  public Mono<Void> invokeActorReminder(@PathVariable("type") String type,
                                        @PathVariable("id") String id,
                                        @PathVariable("reminder") String reminder,
                                        @RequestBody(required = false) String body) {
    return ActorRuntime.getInstance().invokeReminder(type, id, reminder, body);
  }

  private static String findMethodData(String body) throws IOException {
    if (body == null) {
      return null;
    }

    JsonNode root = OBJECT_MAPPER.readTree(body);
    if (root == null) {
      return null;
    }

    JsonNode dataNode = root.get("data");
    if (dataNode == null) {
      return null;
    }

    return new String(dataNode.binaryValue(), StandardCharsets.UTF_8);
  }

  private static String buildResponse(String data) throws RuntimeException {
    try {
      try (Writer writer = new StringWriter()) {
        JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
        generator.writeStartObject();
        if (data != null) {
          generator.writeBinaryField("data", data.getBytes());
        }
        generator.writeEndObject();
        generator.close();
        writer.flush();
        return writer.toString();
      }
    } catch (IOException e) {
      // Make Mono happy.
      throw new RuntimeException(e);
    }
  }
}
