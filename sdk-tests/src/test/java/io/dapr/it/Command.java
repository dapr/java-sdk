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

package io.dapr.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Command {

  private static final int SUCCESS_WAIT_TIMEOUT_MINUTES = 5;

  private static final int DESTROY_WAIT_TIMEOUT_SECONDS = 5;

  private final String successMessage;

  private final String command;

  private final String shell;

  private Process process;

  private Map<String, String> env;

  public Command(String successMessage, String command, Map<String, String> env) {
    this.successMessage = successMessage;
    this.command = command;
    this.env = env;
    this.shell = detectShell();
  }

  public Command(String successMessage, String command) {
    this(successMessage, command, null);
  }

  public void run() throws InterruptedException, IOException {
    final AtomicBoolean success = new AtomicBoolean(false);
    final Semaphore finished = new Semaphore(0);
    ProcessBuilder processBuilder = new ProcessBuilder(this.shell, "-c", command);
    if (this.env != null) {
      processBuilder.environment().putAll(this.env);
    }
    this.process = processBuilder.start();

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
      throw new IllegalStateException("Could not find success criteria for command: " + command);
    }
  }

  public void stop() throws InterruptedException {
    if (this.process != null) {
      this.process.destroy();
      Thread.sleep(DESTROY_WAIT_TIMEOUT_SECONDS * 1000);
      if (this.process.isAlive()) {
        this.process.destroyForcibly();
      }
      this.process = null;
    }
  }

  @Override
  public String toString() {
    return this.command;
  }

  private static String detectShell() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("windows")) {
      return "powershell.exe";
    }

    return "bash";
  }
}
