/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.client.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DecryptRequestAlpha1Test {

  private static final String COMPONENT_NAME = "mycomponent";
  private static final String ENCRYPTED_DATA = "encrypted data";

  @Test
  @DisplayName("Constructor should set required fields correctly")
  public void testConstructorWithRequiredFields() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        cipherTextStream
    );
    
    assertEquals(COMPONENT_NAME, request.getComponentName());
    assertNotNull(request.getCipherTextStream());
    assertNull(request.getKeyName());
  }

  @Test
  @DisplayName("setKeyName should set key name correctly")
  public void testFluentSetKeyName() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        cipherTextStream
    ).setKeyName("mykey");
    
    assertEquals("mykey", request.getKeyName());
  }

  @Test
  @DisplayName("setKeyName should return same instance for method chaining")
  public void testFluentSetterReturnsSameInstance() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        cipherTextStream
    );
    
    DecryptRequestAlpha1 sameRequest = request.setKeyName("mykey");
    assertSame(request, sameRequest);
  }

  @Test
  @DisplayName("Constructor should accept null component name")
  public void testNullComponentName() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        null, 
        cipherTextStream
    );
    
    assertNull(request.getComponentName());
  }

  @Test
  @DisplayName("Constructor should accept null ciphertext stream")
  public void testNullCipherTextStream() {
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        null
    );
    
    assertNull(request.getCipherTextStream());
  }

  @Test
  @DisplayName("Constructor should accept empty stream")
  public void testEmptyStream() {
    Flux<byte[]> emptyStream = Flux.empty();
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        emptyStream
    );
    
    assertNotNull(request.getCipherTextStream());
    StepVerifier.create(request.getCipherTextStream())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle multiple chunks in stream")
  public void testMultipleChunksStream() {
    byte[] chunk1 = "chunk1".getBytes(StandardCharsets.UTF_8);
    byte[] chunk2 = "chunk2".getBytes(StandardCharsets.UTF_8);
    byte[] chunk3 = "chunk3".getBytes(StandardCharsets.UTF_8);
    
    Flux<byte[]> multiChunkStream = Flux.just(chunk1, chunk2, chunk3);
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        multiChunkStream
    );
    
    assertNotNull(request.getCipherTextStream());
    
    List<byte[]> collectedChunks = new ArrayList<>();
    StepVerifier.create(request.getCipherTextStream())
        .recordWith(() -> collectedChunks)
        .expectNextCount(3)
        .verifyComplete();
    
    assertEquals(3, collectedChunks.size());
    assertArrayEquals(chunk1, collectedChunks.get(0));
    assertArrayEquals(chunk2, collectedChunks.get(1));
    assertArrayEquals(chunk3, collectedChunks.get(2));
  }

  @Test
  @DisplayName("setKeyName should accept null value")
  public void testSetKeyNameNull() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        cipherTextStream
    )
    .setKeyName("some-key")
    .setKeyName(null);
    
    assertNull(request.getKeyName());
  }

  @Test
  @DisplayName("Should handle key name with version")
  public void testKeyNameWithVersion() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    String keyNameWithVersion = "mykey/v1";
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        cipherTextStream
    ).setKeyName(keyNameWithVersion);
    
    assertEquals(keyNameWithVersion, request.getKeyName());
  }

  @Test
  @DisplayName("Should handle empty component name")
  public void testEmptyComponentName() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        "", 
        cipherTextStream
    );
    
    assertEquals("", request.getComponentName());
  }

  @Test
  @DisplayName("Should handle whitespace-only component name")
  public void testWhitespaceComponentName() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        "   ", 
        cipherTextStream
    );
    
    assertEquals("   ", request.getComponentName());
  }

  @Test
  @DisplayName("Should handle empty key name")
  public void testEmptyKeyName() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        cipherTextStream
    ).setKeyName("");
    
    assertEquals("", request.getKeyName());
  }

  @Test
  @DisplayName("Should handle large data stream")
  public void testLargeDataStream() {
    byte[] largeChunk = new byte[1024 * 1024]; // 1MB chunk
    for (int i = 0; i < largeChunk.length; i++) {
      largeChunk[i] = (byte) (i % 256);
    }
    
    Flux<byte[]> largeStream = Flux.just(largeChunk);
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        largeStream
    );
    
    assertNotNull(request.getCipherTextStream());
    
    StepVerifier.create(request.getCipherTextStream())
        .expectNextMatches(data -> data.length == 1024 * 1024)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty byte array in stream")
  public void testEmptyByteArrayInStream() {
    byte[] emptyArray = new byte[0];
    
    Flux<byte[]> stream = Flux.just(emptyArray);
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        stream
    );
    
    assertNotNull(request.getCipherTextStream());
    
    StepVerifier.create(request.getCipherTextStream())
        .expectNextMatches(data -> data.length == 0)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle stream with binary data")
  public void testStreamWithBinaryData() {
    byte[] binaryData = new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, (byte) 0xFD};
    
    Flux<byte[]> stream = Flux.just(binaryData);
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        stream
    );
    
    StepVerifier.create(request.getCipherTextStream())
        .expectNextMatches(data -> {
          if (data.length != binaryData.length) return false;
          for (int i = 0; i < data.length; i++) {
            if (data[i] != binaryData[i]) return false;
          }
          return true;
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Complete decryption request with key name")
  public void testCompleteConfiguration() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        cipherTextStream
    ).setKeyName("decryption-key/v2");
    
    assertEquals(COMPONENT_NAME, request.getComponentName());
    assertNotNull(request.getCipherTextStream());
    assertEquals("decryption-key/v2", request.getKeyName());
  }

  @Test
  @DisplayName("Should handle multiple setKeyName calls")
  public void testMultipleSetKeyNameCalls() {
    Flux<byte[]> cipherTextStream = Flux.just(ENCRYPTED_DATA.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        cipherTextStream
    )
    .setKeyName("key1")
    .setKeyName("key2")
    .setKeyName("key3");
    
    assertEquals("key3", request.getKeyName());
  }

  @Test
  @DisplayName("Should handle many chunks stream")
  public void testManyChunksStream() {
    int numberOfChunks = 100;
    List<byte[]> chunks = new ArrayList<>();
    for (int i = 0; i < numberOfChunks; i++) {
      chunks.add(("chunk" + i).getBytes(StandardCharsets.UTF_8));
    }
    
    Flux<byte[]> manyChunksStream = Flux.fromIterable(chunks);
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        manyChunksStream
    );
    
    assertNotNull(request.getCipherTextStream());
    
    StepVerifier.create(request.getCipherTextStream())
        .expectNextCount(numberOfChunks)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle stream with special characters in data")
  public void testStreamWithSpecialCharacters() {
    String specialData = "特殊字符 🔓 データ";
    Flux<byte[]> stream = Flux.just(specialData.getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        COMPONENT_NAME, 
        stream
    );
    
    StepVerifier.create(request.getCipherTextStream())
        .expectNextMatches(data -> new String(data, StandardCharsets.UTF_8).equals(specialData))
        .verifyComplete();
  }
}
