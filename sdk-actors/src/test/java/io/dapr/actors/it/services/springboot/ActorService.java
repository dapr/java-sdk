package io.dapr.actors.it.services.springboot;

import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.client.DefaultObjectSerializer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class ActorService {

  /**
   * Starts the service.
   * @param args Expects the port: -p PORT
   * @throws Exception If cannot start service.
   */
  public static void main(String[] args) throws Exception {

    // If port string is not valid, it will throw an exception.
    long port = Long.parseLong(args[0].split(",")[1]);
    ActorRuntime.getInstance().registerActor(DemoActorImpl.class, new DefaultObjectSerializer());

    DaprApplication.start(port);
  }
}
