/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.examples.configuration.http;

import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.examples.DaprApplication;
import io.dapr.springboot.DaprRuntime;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.util.Collections;
import java.util.function.BiConsumer;

/**
 * A sample springboot application to register and receive for updates on configuration items sent by dapr.
 * springboot-sdk implements a generic route and enables users to register for different handlers per
 * configuration store.
 * Users are free to write their own controllers to handle any specific route suited to the need.
 * java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.configuration.http.ConfigurationSubscriber -p 3009
 */
public class ConfigurationHandler {
  /**
   * This is entry point for Configuration Subscriber service.
   * @param args Arguments for main
   * @throws Exception Throws Exception
   */
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "The port this app will listen on");
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    // Handler to receive updates for a particular store.
    BiConsumer<String, SubscribeConfigurationResponse> biConsumer = (store, resp) -> {
      System.out.println("Configuration update received for store: " + store);
      resp.getItems().forEach((k,v) -> {
        System.out.println("Key: " + k + " Value :" + v.getValue());
      });
    };

    DaprRuntime.getInstance().registerConfigurationChangeHandler("configstore", biConsumer);
    DaprApplication.start(port);
  }
}
