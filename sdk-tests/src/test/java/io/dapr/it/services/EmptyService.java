/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.services;

/**
 * Use this class in order to run DAPR with any needed services, like states.
 * <p>
 * To run manually, from repo root:
 * 1. mvn clean install
 * 2. dapr run --components-path ./components --dapr-grpc-port 41707 --dapr-http-port 32851 -- mvn exec:java -Dexec.mainClass=io.dapr.it.services.EmptyService -Dexec.classpathScope="test" -Dexec.args="-p 44511 -grpcPort 41707 -httpPort 32851" -pl=sdk
 */
public class EmptyService {

  public static final String SUCCESS_MESSAGE = "Hello from " + EmptyService.class.getSimpleName();

  public static void main(String[] args) throws InterruptedException {
    System.out.println(SUCCESS_MESSAGE);
  }
}
