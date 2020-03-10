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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Command {

  private static final int SUCCESS_WAIT_TIMEOUT_MINUTES = 5;

  private final String successMessage;

  private final String command;

  private Process process;

  public Command(String successMessage, String command) {
    this.successMessage = successMessage;
    this.command = command;
  }

  public void run() throws InterruptedException, IOException {
    final AtomicBoolean success = new AtomicBoolean(false);
    final Semaphore finished = new Semaphore(0);
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
                  success.set(true);
                  finished.release();
                  // continue.
                }
              }
            }
          }
        }
        if (!success.get()) {
          finished.release();
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });

    stdoutReader.start();
    // Waits for success to happen within 1 minute.
    finished.tryAcquire(SUCCESS_WAIT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    if (!success.get()) {
      System.out.println("TEST WARNING: Could find success criteria for command: " + command);
    }
  }
}
