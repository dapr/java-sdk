/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.actors.http;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.actors.runtime.ActorRuntime;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;

/**
 * Service for Actor runtime.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the server:
 * dapr run --app-id demoactorservice --app-port 3000 --port 3005 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.actors.http.DemoActorService -Dexec.args="-p 3000"
 */
public class DemoActorService {

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final HttpHandler ROUTES = new RoutingHandler()
    .get("/", DemoActorService::handleDaprConfig)
    .get("/dapr/config", DemoActorService::handleDaprConfig)
    .post("/actors/{actorType}/{id}", DemoActorService::handleActorActivate)
    .delete("/actors/{actorType}/{id}", DemoActorService::handleActorDeactivate)
    .put("/actors/{actorType}/{id}/method/{methodName}", DemoActorService::handleActorInvoke)
    .put("/actors/{actorType}/{id}/method/timer/{timerName}", DemoActorService::handleActorTimer)
    .put("/actors/{actorType}/{id}/method/remind/{reminderName}", DemoActorService::handleActorReminder);

  private final int port;

  private final Undertow server;

  private DemoActorService(int port) {
    this.port = port;
    this.server = Undertow
      .builder()
      .addHttpListener(port, "localhost")
      .setHandler(ROUTES)
      .build();
    ActorRuntime.getInstance().registerActor(DemoActorImpl.class);
  }

  private void start() {
    // Now we handle ctrl+c (or any other JVM shutdown)
    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        System.out.println("Server: shutting down gracefully ...");
        DemoActorService.this.server.stop();
        System.out.println("Server: Bye.");
      }
    });

    System.out.println(String.format("Server: listening on port %d ...", this.port));
    this.server.start();
  }

  private static void handleDaprConfig(HttpServerExchange exchange) throws IOException {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    String result = "";
    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.writeStartObject();
      generator.writeArrayFieldStart("entities");
      for(String actorClass : ActorRuntime.getInstance().getRegisteredActorTypes()) {
        generator.writeString(actorClass);
      }
      generator.writeEndArray();
      generator.writeStringField("actorIdleTimeout", "10s");
      generator.writeStringField("actorScanInterval", "1s");
      generator.writeStringField("drainOngoingCallTimeout", "1s");
      generator.writeBooleanField("drainBalancedActors", true);
      generator.writeEndObject();
      generator.close();
      writer.flush();
      result = writer.toString();
    }

    exchange.getResponseSender().send(result);
  }

  private static void handleActorActivate(HttpServerExchange exchange) {
    if (exchange.isInIoThread()) {
      exchange.dispatch(DemoActorService::handleActorActivate);
      return;
    }

    String actorType = findParamValueOrNull(exchange, "actorType");
    String actorId = findParamValueOrNull(exchange, "id");
    ActorRuntime.getInstance().activate(actorType, actorId).block();
    exchange.getResponseSender().send("");
  }

  private static void handleActorDeactivate(HttpServerExchange exchange) {
    if (exchange.isInIoThread()) {
      exchange.dispatch(DemoActorService::handleActorDeactivate);
      return;
    }

    String actorType = findParamValueOrNull(exchange, "actorType");
    String actorId = findParamValueOrNull(exchange, "id");
    ActorRuntime.getInstance().deactivate(actorType, actorId).block();
  }

  private static void handleActorInvoke(HttpServerExchange exchange) throws IOException {
    if (exchange.isInIoThread()) {
      exchange.dispatch(DemoActorService::handleActorInvoke);
      return;
    }

    String actorType = findParamValueOrNull(exchange, "actorType");
    String actorId = findParamValueOrNull(exchange, "id");
    String methodName = findParamValueOrNull(exchange, "methodName");
    exchange.startBlocking();
    String data = findMethodData(exchange.getInputStream());
    String result = ActorRuntime.getInstance().invoke(actorType, actorId, methodName, data).block();
    exchange.getResponseSender().send(buildResponse(result));
  }

  private static void handleActorTimer(HttpServerExchange exchange) throws IOException {
    if (exchange.isInIoThread()) {
      exchange.dispatch(DemoActorService::handleActorTimer);
      return;
    }

    String actorType = findParamValueOrNull(exchange, "actorType");
    String actorId = findParamValueOrNull(exchange, "id");
    String timerName = findParamValueOrNull(exchange, "timerName");
    ActorRuntime.getInstance().invokeTimer(actorType, actorId, timerName).block();
    exchange.getResponseSender().send("");
  }

  private static void handleActorReminder(HttpServerExchange exchange) throws IOException {
    if (exchange.isInIoThread()) {
      exchange.dispatch(DemoActorService::handleActorReminder);
      return;
    }

    String actorType = findParamValueOrNull(exchange, "actorType");
    String actorId = findParamValueOrNull(exchange, "id");
    String reminderName = findParamValueOrNull(exchange, "reminderName");
    exchange.startBlocking();
    String params = IOUtils.toString(exchange.getInputStream(), StandardCharsets.UTF_8);
    ActorRuntime.getInstance().invokeReminder(actorType, actorId, reminderName, params).block();
    exchange.getResponseSender().send("");
  }

  private static String findParamValueOrNull(HttpServerExchange exchange, String name) {
    Map<String, Deque<String>> params = exchange.getQueryParameters();
    if (params == null) {
      return null;
    }

    Deque<String> values = params.get(name);
    if ((values == null) || (values.isEmpty())) {
      return null;
    }

    return values.getFirst();
  }

  private static String findMethodData(InputStream stream) throws IOException {
    JsonNode root = OBJECT_MAPPER.readTree(stream);
    if (root == null) {
      return null;
    }

    JsonNode dataNode = root.get("data");
    if (dataNode == null) {
      return null;
    }

    return new String(dataNode.binaryValue(), StandardCharsets.UTF_8);
  }

  private static String buildResponse(String data) throws IOException {
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
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "Port to listen to.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));
    final DemoActorService service = new DemoActorService(port);
    service.start();
  }
}
