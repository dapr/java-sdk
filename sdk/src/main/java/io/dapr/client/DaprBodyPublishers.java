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

package io.dapr.client;

import io.dapr.serializer.DefaultObjectSerializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;

/**
 * Convenience {@link BodyPublisher} factories for use with {@link DaprInvokeHttpClient}
 * and the standard {@link java.net.http.HttpClient}.
 *
 * <p>{@link DaprClient#invokeHttpClient(String)} intentionally does <em>not</em>
 * serialize request bodies for you — callers pass raw {@link BodyPublisher}s
 * exactly as with any {@link java.net.http.HttpClient}. This class provides an
 * opt-in helper that matches the JSON encoding the deprecated
 * {@code DaprClient.invokeMethod} APIs applied internally, easing migration.
 *
 * <p>Example:
 * <pre>{@code
 * record Order(String id, int qty) {}
 *
 * HttpRequest request = invoker.newRequestBuilder("orders")
 *     .header("Content-Type", "application/json")
 *     .POST(DaprBodyPublishers.json(new Order("o-1", 3)))
 *     .build();
 * }</pre>
 */
public final class DaprBodyPublishers {

  private static final DefaultObjectSerializer SERIALIZER = new DefaultObjectSerializer();

  private DaprBodyPublishers() {
  }

  /**
   * Serializes the given value as JSON using the SDK's default object serializer
   * (Jackson) and returns a {@link BodyPublisher} carrying the resulting bytes.
   *
   * <p>This matches the wire encoding the deprecated
   * {@code DaprClient.invokeMethod} APIs applied to request payloads: e.g.
   * {@code json("hello")} emits {@code "hello"} (a JSON string) and
   * {@code json(null)} emits an empty body.
   *
   * <p>Callers are still responsible for setting an appropriate
   * {@code Content-Type} header (typically {@code application/json}).
   *
   * <p>This helper is a convenience for the default-serializer case. It does
   * <em>not</em> honor a custom {@link io.dapr.serializer.DaprObjectSerializer}
   * configured on the {@link DaprClientBuilder}. Callers with a custom serializer
   * should serialize the value themselves and wrap the resulting bytes:
   * <pre>{@code
   * byte[] bytes = mySerializer.serialize(value);
   * BodyPublisher body = HttpRequest.BodyPublishers.ofByteArray(bytes);
   * }</pre>
   * The only behavior this helper adds over a direct {@code ofByteArray} call is
   * choosing a length-known {@link BodyPublisher} so the JDK emits
   * {@code Content-Length} rather than {@code Transfer-Encoding: chunked}.
   *
   * @param value object to serialize; {@code null} yields an empty body.
   * @return a body publisher carrying the JSON-encoded bytes.
   * @throws UncheckedIOException if serialization fails.
   */
  public static BodyPublisher json(Object value) {
    try {
      byte[] bytes = SERIALIZER.serialize(value);
      return bytes == null ? BodyPublishers.noBody() : BodyPublishers.ofByteArray(bytes);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to JSON-serialize request body", e);
    }
  }
}
