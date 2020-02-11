/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.actors.http;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.springboot.DaprApplication;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * Service for Actor runtime.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the server:
 * dapr run --app-id demoactorservice --app-port 3000 --port 3005 \
 *   -- java -jar examples/target/dapr-sdk-examples-0.2.0-SNAPSHOT-exec.jar
 *   io.dapr.examples.actors.http.DemoActorService -p 3000
 */
public class DemoActorService {

  /**
   * The main method of this app.
   * @param args The port the app will listen on.
   * @throws Exception An Exception.
   */
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "Port the will listen to.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    // Register the Actor class.
    ActorRuntime.getInstance().registerActor(DemoActorImpl.class);

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
}
