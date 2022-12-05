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

public final class Version {

  private static String sdkVersion = null;

  /**
   * Retrieves sdk version from resources.
   *
   * @return String version of sdk.
   */
  public static String getSdkVersion() {

    if (sdkVersion != null) {
      return sdkVersion;
    }

    try (InputStream input = Version.class.getResourceAsStream("/sdk_version.properties");) {
      Properties properties = new Properties();
      properties.load(input);
      sdkVersion = "dapr-sdk-java/v" + properties.getProperty("sdk_version", "unknown");
    } catch (IOException e) {
      sdkVersion = "unknown";
    }

    return sdkVersion;
  }

}
