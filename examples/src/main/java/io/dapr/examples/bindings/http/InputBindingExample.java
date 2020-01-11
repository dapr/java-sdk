/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.bindings.http;

import io.dapr.runtime.Dapr;
import io.dapr.springboot.DaprApplication;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Mono;

/**
 * Service for input binding example.
 * 1. From your repo root, build and install jars:
 * mvn clean install
 * 2. cd to [repo-root]/examples
 * 3. Run :
 * dapr run --app-id inputbinding --app-port 3000 --port 3005 -- mvn exec:java -Dexec.mainClass=io.dapr.examples.bindings.http.InputBindingExample -Dexec.args="-p 3000"
 */
@SpringBootApplication
public class InputBindingExample {

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "Port Dapr will listen to.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    final String BINDING_NAME = "sample123";

    // "sample123" is the name of the binding.  It will be received at url /v1.0/bindings/sample123
    Dapr.getInstance().registerInputBinding(BINDING_NAME, (message, metadata) -> Mono
      .fromSupplier(() -> {
        System.out.println("Received message through binding: " + (message == null ? "" : new String(message)));
        return Boolean.TRUE;
      })
      .then(Mono.empty()));

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
}
