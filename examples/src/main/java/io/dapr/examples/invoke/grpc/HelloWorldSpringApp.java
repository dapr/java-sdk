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

package io.dapr.examples.invoke.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run in server mode:
 * dapr run --app-id hellogrpc-spring-app --app-port 3000 \
 * -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.invoke.grpc.HelloWorldSpringApp -p 3000
 */
@RestController
@SpringBootApplication
public class HelloWorldSpringApp {

  /**
   * Starts the service.
   * 
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

    SpringApplication app = new SpringApplication(HelloWorldSpringApp.class);
    app.run(String.format("--server.port=%d", port));
  }

  @RequestMapping("/invoke/grpc/say")
  String invokeGrpcSayMethod() {
    return invokeHelloGrpcMethod("say");
  }

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  public static DaprClient client = new DaprClientBuilder().build();

  static String helloGrpcServiceAppId = "hellogrpc";

  /**
   * Invoke the hellogrpc method.
   * 
   * @param method to invoke
   * @return invoke method returned values.
   */
  public static String invokeHelloGrpcMethod(String method) {
    Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

    String message = "Message @ " + utcNowAsString;

    System.out.println("Sending message: " + message);
    byte[] ret = client.invokeMethod(
        helloGrpcServiceAppId, method, message, HttpExtension.NONE, TypeRef.BYTE_ARRAY).block();
    System.out.println("Message sent: " + message);
    System.out.println("  => ret = " + ret.toString());

    return ret.toString();
  }
}
