/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.durabletask.orchestration;

import io.dapr.durabletask.orchestration.exception.VersionNotRegisteredException;

import java.util.HashMap;
import java.util.logging.Logger;

public class TaskOrchestrationFactories {
  private static final Logger logger = Logger.getLogger(TaskOrchestrationFactories.class.getPackage().getName());

  final HashMap<String, TaskOrchestrationFactory> orchestrationFactories = new HashMap<>();
  final HashMap<String, HashMap<String, TaskOrchestrationFactory>> versionedOrchestrationFactories = new HashMap<>();
  final HashMap<String, String> latestVersionOrchestrationFactories = new HashMap<>();

  /**
   * Adds a new orchestration factory to the registry.
   *
   * @param factory the factory to add
   */
  public void addOrchestration(TaskOrchestrationFactory factory) {
    String key = factory.getName();
    if (this.emptyString(key)) {
      throw new IllegalArgumentException("A non-empty task orchestration name is required.");
    }

    if (this.orchestrationFactories.containsKey(key)) {
      throw new IllegalArgumentException(
          String.format("A task orchestration factory named %s is already registered.", key));
    }

    if (emptyString(factory.getVersionName())) {
      this.orchestrationFactories.put(key, factory);
      return;
    }

    if (!this.versionedOrchestrationFactories.containsKey(key)) {
      this.versionedOrchestrationFactories.put(key, new HashMap<>());
    }

    if (this.versionedOrchestrationFactories.get(key).containsKey(factory.getVersionName())) {
      throw new IllegalArgumentException("The version name " + factory.getVersionName() + "for "
          + factory.getName() + " is already registered.");
    }

    this.versionedOrchestrationFactories.get(key).put(factory.getVersionName(), factory);

    if (factory.isLatestVersion()) {
      logger.info("Setting1 latest version for " + key + " to " + factory.getVersionName());
      if (this.latestVersionOrchestrationFactories.containsKey(key)) {
        throw new IllegalStateException("Latest version already set for " + key);
      }
      this.latestVersionOrchestrationFactories.put(key, factory.getVersionName());
    }

  }

  /**
   * Gets the orchestration factory for the specified orchestration name.
   *
   * @param orchestrationName the orchestration name
   * @return the orchestration factory
   */
  public TaskOrchestrationFactory getOrchestrationFactory(String orchestrationName) {
    logger.info("Get orchestration factory for " + orchestrationName);
    if (this.orchestrationFactories.containsKey(orchestrationName)) {
      return this.orchestrationFactories.get(orchestrationName);
    }

    return this.getOrchestrationFactory(orchestrationName, "");
  }

  /**
   * Gets the orchestration factory for the specified orchestration name and version.
   *
   * @param orchestrationName the orchestration name
   * @param versionName       the version name
   * @return the orchestration factory
   */
  public TaskOrchestrationFactory getOrchestrationFactory(String orchestrationName, String versionName) {
    logger.info("Get orchestration factory for " + orchestrationName + " version " + versionName);
    if (this.orchestrationFactories.containsKey(orchestrationName)) {
      return this.orchestrationFactories.get(orchestrationName);
    }

    if (!this.versionedOrchestrationFactories.containsKey(orchestrationName)) {
      logger.warning("No orchestration factory registered for " + orchestrationName);
      return null;
    }

    if (this.emptyString(versionName)) {
      logger.info("No version specified, returning latest version");
      String latestVersion = this.latestVersionOrchestrationFactories.get(orchestrationName);
      logger.info("Latest version is " + latestVersion);
      return this.versionedOrchestrationFactories.get(orchestrationName).get(latestVersion);
    }

    if (this.versionedOrchestrationFactories.get(orchestrationName).containsKey(versionName)) {
      return this.versionedOrchestrationFactories.get(orchestrationName).get(versionName);
    }

    throw new VersionNotRegisteredException();
  }

  private boolean emptyString(String s) {
    return s == null || s.isEmpty();
  }
}
