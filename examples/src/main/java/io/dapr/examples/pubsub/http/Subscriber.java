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

/**
 * Service for subscriber.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the server:
 * dapr run --app-id subscriber --app-port 3000 --port 3005 -- \
 *   java -jar examples/target/dapr-sdk-examples-0.2.0-SNAPSHOT-exec.jar io.dapr.examples.pubsub.http.Subscriber -p 3000
 */
public class Subscriber {

  /**
   * This is the entry point for this example app, which subscribes to a topic.
   * @param args The port this app will listen on.
   * @throws Exception An Exception on startup.
   */
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "The port this app will listen on");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
}