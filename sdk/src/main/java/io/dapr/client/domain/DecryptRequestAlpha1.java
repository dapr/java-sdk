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

import reactor.core.publisher.Flux;

/**
 * Request to decrypt data using the Dapr Cryptography building block.
 * Uses streaming to handle large payloads efficiently.
 */
public class DecryptRequestAlpha1 {

  private final String componentName;
  private final Flux<byte[]> cipherTextStream;
  private String keyName;

  /**
   * Constructor for DecryptRequestAlpha1.
   *
   * @param componentName    Name of the cryptography component. Required.
   * @param cipherTextStream Stream of ciphertext data to decrypt. Required.
   */
  public DecryptRequestAlpha1(String componentName, Flux<byte[]> cipherTextStream) {
    this.componentName = componentName;
    this.cipherTextStream = cipherTextStream;
  }

  /**
   * Gets the cryptography component name.
   *
   * @return the component name
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * Gets the ciphertext data stream to decrypt.
   *
   * @return the ciphertext stream as Flux of byte arrays
   */
  public Flux<byte[]> getCipherTextStream() {
    return cipherTextStream;
  }

  /**
   * Gets the key name (or name/version) to use for decryption.
   *
   * @return the key name, or null if using the key embedded in the ciphertext
   */
  public String getKeyName() {
    return keyName;
  }

  /**
   * Sets the key name (or name/version) to decrypt the message.
   * This overrides any key reference included in the message if present.
   * This is required if the message doesn't include a key reference
   * (i.e., was created with omitDecryptionKeyName set to true).
   *
   * @param keyName the key name to use for decryption
   * @return this request instance for method chaining
   */
  public DecryptRequestAlpha1 setKeyName(String keyName) {
    this.keyName = keyName;
    return this;
  }
}
