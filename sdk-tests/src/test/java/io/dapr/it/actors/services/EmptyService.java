/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services;

/**
 * Use this class in order to run DAPR with any needed services, like states.
 * <p>
 * To run manually, from repo root:
 * 1. mvn clean install
 * 2. dapr run --grpc-port 41707 --port 32851 -- mvn exec:java -Dexec.mainClass=io.dapr.it.services.EmptyService -Dexec.classpathScope="test" -Dexec.args="-p 44511 -grpcPort 41707 -httpPort 32851" -pl=sdk
 */
public class EmptyService {
  public static void main(String[] args) {
    System.out.println("Hello from EmptyService");
  }
}
