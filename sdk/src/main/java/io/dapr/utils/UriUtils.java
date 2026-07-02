/*
 * Copyright 2026 The Dapr Authors
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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Utilities for building and resolving the URIs used by the Dapr HTTP APIs.
 */
public class UriUtils {

  /**
   * Resolves {@code relativePath} against {@code baseUri} via {@link URI#resolve(String)}
   * without modifying it. If {@code relativePath} contains characters that are illegal in a
   * URI (e.g. spaces), this throws {@link IllegalArgumentException} with guidance to
   * {@link #encodePath(String)}.
   *
   * <p>The raw {@code relativePath} is never echoed in the thrown exception (it may contain
   * sensitive identifiers); only the offending index is surfaced.
   *
   * @param baseUri      the base URI to resolve against.
   * @param relativePath the relative path to resolve.
   * @return the resolved URI.
   * @throws IllegalArgumentException if {@code relativePath} contains characters that are
   *     illegal in a URI; encode it first with {@link #encodePath(String)}.
   */
  public static URI resolve(URI baseUri, String relativePath) {
    Objects.requireNonNull(baseUri, "baseUri");
    Objects.requireNonNull(relativePath, "relativePath");
    try {
      return baseUri.resolve(relativePath);
    } catch (IllegalArgumentException exception) {
      // The wrapped URISyntaxException echoes the full relativePath in its message, which may
      // contain sensitive identifiers; surface only the offending index, never the input, and
      // do not chain the cause.
      String position = (exception.getCause() instanceof URISyntaxException syntaxException
          && syntaxException.getIndex() >= 0) ? " at index " + syntaxException.getIndex() : "";
      throw new IllegalArgumentException(
          "relativePath contains a character that is illegal in a URI" + position
              + " (e.g. a space). Encode it first with UriUtils.encodePath(String).");
    }
  }

  /**
   * Percent-encodes each {@code /}-delimited segment of the path portion of
   * {@code relativePath} (preserving the separators) so it can be safely passed to
   * {@link #resolve(URI, String)} when a segment contains characters that are illegal in a
   * URI path, such as spaces. Mirrors the per-segment encoding the deprecated
   * {@code DaprClient.invokeMethod} APIs applied internally.
   *
   * <p>The query string (from the first {@code ?} onwards) is appended unchanged and must be
   * pre-encoded by the caller. A leading slash is preserved, so {@link URI#resolve(String)}
   * still treats it as replacing the entire base path.
   *
   * @param relativePath the raw, unencoded path to encode.
   * @return {@code relativePath} with each path segment percent-encoded.
   */
  public static String encodePath(String relativePath) {
    Objects.requireNonNull(relativePath, "relativePath");
    int queryStart = relativePath.indexOf('?');
    String path = queryStart < 0 ? relativePath : relativePath.substring(0, queryStart);
    String query = queryStart < 0 ? "" : relativePath.substring(queryStart);

    StringBuilder encoded = new StringBuilder(path.length() + 16);
    int start = 0;
    for (int i = 0; i <= path.length(); i++) {
      if (i == path.length() || path.charAt(i) == '/') {
        String segment = path.substring(start, i);
        if (!segment.isEmpty()) {
          encoded.append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        if (i < path.length()) {
          encoded.append('/');
        }
        start = i + 1;
      }
    }

    return encoded.append(query).toString();
  }
}
