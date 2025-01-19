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

package io.dapr.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public final class Version {

  private static volatile AtomicReference<String> sdkVersion = new AtomicReference<>();

  /**
   * Retrieves sdk version from resources.
   *
   * @return String version of sdk.
   */
  public static String getSdkVersion() {
    var version = sdkVersion.get();

    if ((version != null) && !version.isBlank()) {
      return version;
    }

    try (InputStream input = Version.class.getResourceAsStream("/sdk_version.properties")) {
      Properties properties = new Properties();
      properties.load(input);
      var v = properties.getProperty("sdk_version", null);
      if (v == null) {
        throw new IllegalStateException("Did not find sdk_version property!");
      }

      if (v.isBlank()) {
        throw new IllegalStateException("Property sdk_version cannot be blank.");
      }

      version = "dapr-sdk-java/v" + v;
      sdkVersion.set(version);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load sdk_version property!", e);
    }

    return version;
  }

}
