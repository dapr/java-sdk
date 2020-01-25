/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Command {

  private final String successMessage;

  private final String command;

  private Process process;

  public Command(String successMessage, String command) {
    this.successMessage = successMessage;
    this.command = command;
  }

  public void run() throws InterruptedException, IOException {
    final Semaphore success = new Semaphore(0);
    this.process = Runtime.getRuntime().exec(command);

    final Thread stdoutReader = new Thread(() -> {
      try {
        try (InputStream stdin = this.process.getInputStream()) {
          try (InputStreamReader isr = new InputStreamReader(stdin)) {
            try (BufferedReader br = new BufferedReader(isr)) {
              String line;
              while ((line = br.readLine()) != null) {
                System.out.println(line);
                if (line.contains(successMessage)) {
                  success.release();
                }
              }
            }
          }
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });

    stdoutReader.start();
    // Waits for success to happen within 1 minute.
    success.tryAcquire(1, TimeUnit.MINUTES);
  }
}
