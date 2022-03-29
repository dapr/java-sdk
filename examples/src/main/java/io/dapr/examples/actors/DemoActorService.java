/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.examples.actors;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.examples.DaprApplication;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.time.Duration;

/**
 * Service for Actor runtime.
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd to [repo-root]/examples
 * 3. Run the server:
 * dapr run --components-path ./components/actors --app-id demoactorservice --app-port 3000 \
 *   -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.actors.DemoActorService -p 3000
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
