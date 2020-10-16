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

import java.time.Duration;

/**
 * Service for Actor runtime.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the server:
 * dapr run --components-path ./components --app-id demoactorservice --app-port 3000 --dapr-http-port 3005 \
 *   -- java -jar target/dapr-java-sdk-examples-exec.jar \
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
    final int port = Integer.parseInt(cmd.getOptionValue("port"));

    // Idle timeout until actor instance is deactivated.
    ActorRuntime.getInstance().getConfig().setActorIdleTimeout(Duration.ofSeconds(30));
    // How often actor instances are scanned for deactivation and balance.
    ActorRuntime.getInstance().getConfig().setActorScanInterval(Duration.ofSeconds(10));
    // How long to wait until for draining an ongoing API call for an actor instance.
    ActorRuntime.getInstance().getConfig().setDrainOngoingCallTimeout(Duration.ofSeconds(10));
    // Determines whether to drain API calls for actors instances being balanced.
    ActorRuntime.getInstance().getConfig().setDrainBalancedActors(true);

    // Register the Actor class.
    ActorRuntime.getInstance().registerActor(DemoActorImpl.class);

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
}
