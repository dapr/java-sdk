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

import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest.BodyPublisher;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DaprBodyPublishersTest {

  @Test
  public void json_stringIsJsonEncodedWithQuotes() throws Exception {
    BodyPublisher publisher = DaprBodyPublishers.json("message one");
    assertEquals("\"message one\"", drain(publisher));
    assertEquals("\"message one\"".getBytes().length, publisher.contentLength());
  }

  @Test
  public void json_pojoIsSerializedAsJsonObject() throws Exception {
    BodyPublisher publisher = DaprBodyPublishers.json(Map.of("id", "o-1", "qty", 3));
    String body = drain(publisher);
    // Map ordering is not guaranteed; check both fields are present and shape is an object.
    assertTrue(body.startsWith("{") && body.endsWith("}"), () -> "unexpected JSON: " + body);
    assertTrue(body.contains("\"id\":\"o-1\""), () -> "missing id field: " + body);
    assertTrue(body.contains("\"qty\":3"), () -> "missing qty field: " + body);
  }

  @Test
  public void json_nullYieldsEmptyBody() {
    BodyPublisher publisher = DaprBodyPublishers.json(null);
    assertEquals(0L, publisher.contentLength());
  }

  private static String drain(BodyPublisher publisher) throws Exception {
    List<ByteBuffer> chunks = new ArrayList<>();
    CountDownLatch done = new CountDownLatch(1);
    publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
      @Override
      public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
      }

      @Override
      public void onNext(ByteBuffer item) {
        chunks.add(item);
      }

      @Override
      public void onError(Throwable throwable) {
        done.countDown();
      }

      @Override
      public void onComplete() {
        done.countDown();
      }
    });
    assertTrue(done.await(5, TimeUnit.SECONDS));
    int total = chunks.stream().mapToInt(ByteBuffer::remaining).sum();
    byte[] out = new byte[total];
    int offset = 0;
    for (ByteBuffer chunk : chunks) {
      int len = chunk.remaining();
      chunk.get(out, offset, len);
      offset += len;
    }
    return new String(out);
  }
}
