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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DecryptRequestAlpha1Test {

  @Test
  public void testConstructorWithRequiredFields() {
    Flux<byte[]> cipherTextStream = Flux.just("encrypted data".getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        "mycomponent", 
        cipherTextStream
    );
    
    assertEquals("mycomponent", request.getComponentName());
    assertNotNull(request.getCipherTextStream());
    assertNull(request.getKeyName());
  }

  @Test
  public void testFluentSetKeyName() {
    Flux<byte[]> cipherTextStream = Flux.just("encrypted data".getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        "mycomponent", 
        cipherTextStream
    ).setKeyName("mykey");
    
    assertEquals("mykey", request.getKeyName());
  }

  @Test
  public void testFluentSetterReturnsSameInstance() {
    Flux<byte[]> cipherTextStream = Flux.just("encrypted data".getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        "mycomponent", 
        cipherTextStream
    );
    
    DecryptRequestAlpha1 sameRequest = request.setKeyName("mykey");
    assertEquals(request, sameRequest);
  }

  @Test
  public void testNullComponentName() {
    Flux<byte[]> cipherTextStream = Flux.just("encrypted data".getBytes(StandardCharsets.UTF_8));
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        null, 
        cipherTextStream
    );
    
    assertNull(request.getComponentName());
  }

  @Test
  public void testNullCipherTextStream() {
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        "mycomponent", 
        null
    );
    
    assertNull(request.getCipherTextStream());
  }

  @Test
  public void testEmptyStream() {
    Flux<byte[]> emptyStream = Flux.empty();
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        "mycomponent", 
        emptyStream
    );
    
    assertNotNull(request.getCipherTextStream());
  }

  @Test
  public void testMultipleChunksStream() {
    Flux<byte[]> multiChunkStream = Flux.just(
        "chunk1".getBytes(StandardCharsets.UTF_8),
        "chunk2".getBytes(StandardCharsets.UTF_8),
        "chunk3".getBytes(StandardCharsets.UTF_8)
    );
    
    DecryptRequestAlpha1 request = new DecryptRequestAlpha1(
        "mycomponent", 
        multiChunkStream
    );
    
    assertNotNull(request.getCipherTextStream());
  }
}
