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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncryptRequestAlpha1Test {

  @Test
  public void testConstructorWithRequiredFields() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent", 
        plainTextStream, 
        "mykey", 
        "RSA-OAEP-256"
    );
    
    assertEquals("mycomponent", request.getComponentName());
    assertNotNull(request.getPlainTextStream());
    assertEquals("mykey", request.getKeyName());
    assertEquals("RSA-OAEP-256", request.getKeyWrapAlgorithm());
    assertNull(request.getDataEncryptionCipher());
    assertFalse(request.isOmitDecryptionKeyName());
    assertNull(request.getDecryptionKeyName());
  }

  @Test
  public void testFluentSetters() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent", 
        plainTextStream, 
        "mykey", 
        "RSA-OAEP-256"
    )
    .setDataEncryptionCipher("AES-GCM")
    .setOmitDecryptionKeyName(true)
    .setDecryptionKeyName("decrypt-key");
    
    assertEquals("AES-GCM", request.getDataEncryptionCipher());
    assertTrue(request.isOmitDecryptionKeyName());
    assertEquals("decrypt-key", request.getDecryptionKeyName());
  }

  @Test
  public void testFluentSettersReturnSameInstance() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent", 
        plainTextStream, 
        "mykey", 
        "RSA-OAEP-256"
    );
    
    EncryptRequestAlpha1 sameRequest = request.setDataEncryptionCipher("AES-GCM");
    assertEquals(request, sameRequest);
    
    sameRequest = request.setOmitDecryptionKeyName(true);
    assertEquals(request, sameRequest);
    
    sameRequest = request.setDecryptionKeyName("decrypt-key");
    assertEquals(request, sameRequest);
  }

  @Test
  public void testNullComponentName() {
    Flux<byte[]> plainTextStream = Flux.just("test data".getBytes(StandardCharsets.UTF_8));
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        null, 
        plainTextStream, 
        "mykey", 
        "RSA-OAEP-256"
    );
    
    assertNull(request.getComponentName());
  }

  @Test
  public void testNullPlainTextStream() {
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent", 
        null, 
        "mykey", 
        "RSA-OAEP-256"
    );
    
    assertNull(request.getPlainTextStream());
  }

  @Test
  public void testEmptyStream() {
    Flux<byte[]> emptyStream = Flux.empty();
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent", 
        emptyStream, 
        "mykey", 
        "RSA-OAEP-256"
    );
    
    assertNotNull(request.getPlainTextStream());
  }

  @Test
  public void testMultipleChunksStream() {
    Flux<byte[]> multiChunkStream = Flux.just(
        "chunk1".getBytes(StandardCharsets.UTF_8),
        "chunk2".getBytes(StandardCharsets.UTF_8),
        "chunk3".getBytes(StandardCharsets.UTF_8)
    );
    
    EncryptRequestAlpha1 request = new EncryptRequestAlpha1(
        "mycomponent", 
        multiChunkStream, 
        "mykey", 
        "A256KW"
    );
    
    assertNotNull(request.getPlainTextStream());
    assertEquals("A256KW", request.getKeyWrapAlgorithm());
  }
}
