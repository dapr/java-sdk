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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncryptRequestAlpha1Test {

  private static final String COMPONENT_NAME = "mycomponent";
  private static final String KEY_NAME = "mykey";
  private static final String KEY_WRAP_ALGORITHM = "RSA-OAEP-256";
  private static final String TEST_DATA = "test data";

  @Test
  @DisplayName("Constructor should set all required fields correctly")
  public void testConstructorWithRequiredFields() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertEquals(COMPONENT_NAME, request.getComponentName());
    assertNotNull(request.getPlainTextStream());
    assertEquals(KEY_NAME, request.getKeyName());
    assertEquals(KEY_WRAP_ALGORITHM, request.getKeyWrapAlgorithm());
    assertNull(request.getDataEncryptionCipher());
    assertFalse(request.isOmitDecryptionKeyName());
    assertNull(request.getDecryptionKeyName());
  }

  @Test
  @DisplayName("Fluent setters should set optional fields correctly")
  public void testFluentSetters() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    )
    .setDataEncryptionCipher("aes-gcm")
    .setOmitDecryptionKeyName(true)
    .setDecryptionKeyName("decrypt-key");
    
    assertEquals("aes-gcm", request.getDataEncryptionCipher());
    assertTrue(request.isOmitDecryptionKeyName());
    assertEquals("decrypt-key", request.getDecryptionKeyName());
  }

  @Test
  @DisplayName("Fluent setters should return same instance for method chaining")
  public void testFluentSettersReturnSameInstance() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    EncryptRequestAlpha1 sameRequest = request.setDataEncryptionCipher("aes-gcm");
    assertSame(request, sameRequest);
    
    sameRequest = request.setOmitDecryptionKeyName(true);
    assertSame(request, sameRequest);
    
    sameRequest = request.setDecryptionKeyName("decrypt-key");
    assertSame(request, sameRequest);
  }

  @Test
  @DisplayName("Constructor should accept null component name")
  public void testNullComponentName() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        null, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertNull(request.getComponentName());
  }

  @Test
  @DisplayName("Constructor should accept null plaintext stream")
  public void testNullPlainTextStream() {
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        null, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertNull(request.getPlainTextStream());
  }

  @Test
  @DisplayName("Constructor should accept null key name")
  public void testNullKeyName() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        null, 
        KEY_WRAP_ALGORITHM
    );
    
    assertNull(request.getKeyName());
  }

  @Test
  @DisplayName("Constructor should accept null key wrap algorithm")
  public void testNullKeyWrapAlgorithm() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        null
    );
    
    assertNull(request.getKeyWrapAlgorithm());
  }

  @Test
  @DisplayName("Constructor should accept empty stream")
  public void testEmptyStream() {
    Flux<byte[]> emptyStream = Flux.empty();
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        emptyStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertNotNull(request.getPlainTextStream());
    StepVerifier.create(request.getPlainTextStream())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle multiple chunks in stream")
  public void testMultipleChunksStream() {
    byte[] chunk1 = "chunk1".getBytes(StandardCharsets.UTF_8);
    byte[] chunk2 = "chunk2".getBytes(StandardCharsets.UTF_8);
    byte[] chunk3 = "chunk3".getBytes(StandardCharsets.UTF_8);
    
    Flux<byte[]> multiChunkStream = Flux.just(chunk1, chunk2, chunk3);
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        multiChunkStream, 
        KEY_NAME, 
        "A256KW"
    );
    
    assertNotNull(request.getPlainTextStream());
    assertEquals("A256KW", request.getKeyWrapAlgorithm());
    
    List<byte[]> collectedChunks = new ArrayList<>();
    StepVerifier.create(request.getPlainTextStream())
        .recordWith(() -> collectedChunks)
        .expectNextCount(3)
        .verifyComplete();
    
    assertEquals(3, collectedChunks.size());
    assertArrayEquals(chunk1, collectedChunks.get(0));
    assertArrayEquals(chunk2, collectedChunks.get(1));
    assertArrayEquals(chunk3, collectedChunks.get(2));
  }

  @ParameterizedTest
  @DisplayName("Should support various key wrap algorithms")
  @ValueSource(strings = {"A256KW", "AES", "A128CBC", "A192CBC", "A256CBC", "RSA-OAEP-256", "RSA"})
  public void testVariousKeyWrapAlgorithms(String algorithm) {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        algorithm
    );
    
    assertEquals(algorithm, request.getKeyWrapAlgorithm());
  }

  @ParameterizedTest
  @DisplayName("Should support various data encryption ciphers")
  @ValueSource(strings = {"aes-gcm", "chacha20-poly1305"})
  public void testVariousDataEncryptionCiphers(String cipher) {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    ).setDataEncryptionCipher(cipher);
    
    assertEquals(cipher, request.getDataEncryptionCipher());
  }

  @Test
  @DisplayName("setDataEncryptionCipher should accept null value")
  public void testSetDataEncryptionCipherNull() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    )
    .setDataEncryptionCipher("aes-gcm")
    .setDataEncryptionCipher(null);
    
    assertNull(request.getDataEncryptionCipher());
  }

  @Test
  @DisplayName("setDecryptionKeyName should accept null value")
  public void testSetDecryptionKeyNameNull() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    )
    .setDecryptionKeyName("some-key")
    .setDecryptionKeyName(null);
    
    assertNull(request.getDecryptionKeyName());
  }

  @Test
  @DisplayName("setOmitDecryptionKeyName should toggle boolean value")
  public void testSetOmitDecryptionKeyNameToggle() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertFalse(request.isOmitDecryptionKeyName());
    
    request.setOmitDecryptionKeyName(true);
    assertTrue(request.isOmitDecryptionKeyName());
    
    request.setOmitDecryptionKeyName(false);
    assertFalse(request.isOmitDecryptionKeyName());
  }

  @Test
  @DisplayName("Should handle large data stream")
  public void testLargeDataStream() {
    byte[] largeChunk = new byte[1024 * 1024]; // 1MB chunk
    for (int i = 0; i < largeChunk.length; i++) {
      largeChunk[i] = (byte) (i % 256);
    }
    
    Flux<byte[]> largeStream = Flux.just(largeChunk);
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        largeStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertNotNull(request.getPlainTextStream());
    
    StepVerifier.create(request.getPlainTextStream())
        .expectNextMatches(data -> data.length == 1024 * 1024)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty byte array in stream")
  public void testEmptyByteArrayInStream() {
    byte[] emptyArray = new byte[0];
    
    Flux<byte[]> stream = Flux.just(emptyArray);
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        stream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertNotNull(request.getPlainTextStream());
    
    StepVerifier.create(request.getPlainTextStream())
        .expectNextMatches(data -> data.length == 0)
        .verifyComplete();
  }

  @Test
  @DisplayName("Should handle key name with version")
  public void testKeyNameWithVersion() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    String keyNameWithVersion = "mykey/v1";
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        keyNameWithVersion, 
        KEY_WRAP_ALGORITHM
    );
    
    assertEquals(keyNameWithVersion, request.getKeyName());
  }

  @Test
  @DisplayName("Should handle decryption key name with version")
  public void testDecryptionKeyNameWithVersion() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    String decryptionKeyWithVersion = "decrypt-key/v2";
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    ).setDecryptionKeyName(decryptionKeyWithVersion);
    
    assertEquals(decryptionKeyWithVersion, request.getDecryptionKeyName());
  }

  @Test
  @DisplayName("Should handle empty component name")
  public void testEmptyComponentName() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "", 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertEquals("", request.getComponentName());
  }

  @Test
  @DisplayName("Should handle whitespace-only component name")
  public void testWhitespaceComponentName() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "   ", 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    assertEquals("   ", request.getComponentName());
  }

  @Test
  @DisplayName("Should handle empty key name")
  public void testEmptyKeyName() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        "", 
        KEY_WRAP_ALGORITHM
    );
    
    assertEquals("", request.getKeyName());
  }

  @Test
  @DisplayName("Should handle empty key wrap algorithm")
  public void testEmptyKeyWrapAlgorithm() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        ""
    );
    
    assertEquals("", request.getKeyWrapAlgorithm());
  }

  @Test
  @DisplayName("Should handle stream with special characters in data")
  public void testStreamWithSpecialCharacters() {
    String specialData = "特殊字符 🔐 データ";
    Flux<byte[]> stream = Flux.just(specialData.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        stream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    );
    
    StepVerifier.create(request.getPlainTextStream())
        .expectNextMatches(data -> new String(data, StandardCharsets.UTF_8).equals(specialData))
        .verifyComplete();
  }

  @Test
  @DisplayName("Complete configuration with all optional fields")
  public void testCompleteConfiguration() {
    Flux<byte[]> plainTextStream = Flux.just(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        COMPONENT_NAME, 
        plainTextStream, 
        KEY_NAME, 
        KEY_WRAP_ALGORITHM
    )
    .setDataEncryptionCipher("chacha20-poly1305")
    .setOmitDecryptionKeyName(true)
    .setDecryptionKeyName("different-key/v3");
    
    assertEquals(COMPONENT_NAME, request.getComponentName());
    assertNotNull(request.getPlainTextStream());
    assertEquals(KEY_NAME, request.getKeyName());
    assertEquals(KEY_WRAP_ALGORITHM, request.getKeyWrapAlgorithm());
    assertEquals("chacha20-poly1305", request.getDataEncryptionCipher());
    assertTrue(request.isOmitDecryptionKeyName());
    assertEquals("different-key/v3", request.getDecryptionKeyName());
  }
}
