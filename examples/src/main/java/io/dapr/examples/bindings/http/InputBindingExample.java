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

package io.dapr.examples.bindings.http;

import io.dapr.examples.DaprApplication;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * Service for input binding example.
 * 1. From your repo root, build and install jars:
 * mvn clean install
 * 2. cd to [repo-root]/examples
 * 3. Run :
 * dapr run --components-path ./components/bindings --app-id inputbinding --app-port 3000 \
 *   -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.bindings.http.InputBindingExample -p 3000
 */
public class InputBindingExample {

  /**
   * The entry point of this app.
   * @param args The port this app will listen on.
   * @throws Exception The Exception.
   */
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "The port this app will listen on.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
}
