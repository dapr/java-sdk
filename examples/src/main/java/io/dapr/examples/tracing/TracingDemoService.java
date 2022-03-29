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

package io.dapr.examples.tracing;

import io.dapr.examples.DaprApplication;
import io.dapr.examples.OpenTelemetryInterceptor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * Main method to invoke DemoService to test tracing.
 *
 * <p>Instrumentation is handled in {@link OpenTelemetryInterceptor}.
 *
 * <p>1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run in server mode:
 * dapr run --app-id tracingdemo --app-port 3000 \
 *   -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.tracing.TracingDemoService -p 3000
 * 4. Run middle server:
 * dapr run --app-id tracingdemoproxy --app-port 3001 \
 *   -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.tracing.TracingDemoService -p 3001
 */
public class TracingDemoService {

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

    DaprApplication.start(port);
  }
}
