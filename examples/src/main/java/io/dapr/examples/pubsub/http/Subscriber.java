/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.pubsub.http;

import io.dapr.springboot.DaprApplication;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Service for subscriber.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the server:
 * dapr run --app-id subscriber --app-port 3000 --port 3005 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.pubsub.http.Subscriber -Dexec.args="-p 3000"
 */
@SpringBootApplication
public class Subscriber {

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "Port Dapr will listen to.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
}
