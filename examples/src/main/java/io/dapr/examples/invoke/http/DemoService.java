/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.invoke.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.dapr.DaprClientGrpc;
import io.dapr.DaprClientProtos;
import io.dapr.runtime.Dapr;
import io.dapr.runtime.MethodListener;
import io.dapr.springboot.DaprApplication;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import static io.dapr.examples.DaprExamplesProtos.SayRequest;
import static io.dapr.examples.DaprExamplesProtos.SayResponse;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run in server mode:
 * dapr run --app-id invokedemo --app-port 3000 --port 3005 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.invoke.http.DemoService -Dexec.args="-p 3000"
 */
public class DemoService {

  /**
   * Shared Json serializer/deserializer.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Format to output date and time.
   */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * Starts the service.
   * @param args Expects the port: -p PORT
   * @throws Exception If cannot start service.
   */
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "Port to listen to.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    Dapr.getInstance().registerServiceMethod("say", (data, metadata) -> {
      String message = data == null ? "" : new String(data, StandardCharsets.UTF_8);

      Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

      String metadataString = metadata == null ? "" : OBJECT_MAPPER.writeValueAsString(metadata);

      // Handles the request by printing message.
      System.out.println(
        "Server: " + message + " @ " + utcNowAsString + " and metadata: " + metadataString);

      return Mono.just(utcNowAsString.getBytes(StandardCharsets.UTF_8));
    });

    DaprApplication.start(port);
  }
}
