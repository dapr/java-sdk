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
 * Request to encrypt data using the Dapr Cryptography building block.
 * Uses streaming to handle large payloads efficiently.
 */
public class EncryptRequestAlpha1 {

  private final String componentName;
  private final Flux<byte[]> plainTextStream;
  private final String keyName;
  private final String keyWrapAlgorithm;
  private String dataEncryptionCipher;
  private boolean omitDecryptionKeyName;
  private String decryptionKeyName;

  /**
   * Constructor for EncryptRequestAlpha1.
   *
   * @param componentName    Name of the cryptography component. Required.
   * @param plainTextStream  Stream of plaintext data to encrypt. Required.
   * @param keyName          Name (or name/version) of the key to use for encryption. Required.
   * @param keyWrapAlgorithm Key wrapping algorithm to use. Required.
   *                         Supported options: A256KW (alias: AES), A128CBC, A192CBC, A256CBC, 
   *                         RSA-OAEP-256 (alias: RSA).
   */
  public EncryptRequestAlpha1(String componentName, Flux<byte[]> plainTextStream, 
                               String keyName, String keyWrapAlgorithm) {
    this.componentName = componentName;
    this.plainTextStream = plainTextStream;
    this.keyName = keyName;
    this.keyWrapAlgorithm = keyWrapAlgorithm;
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
   * Gets the plaintext data stream to encrypt.
   *
   * @return the plaintext stream as Flux of byte arrays
   */
  public Flux<byte[]> getPlainTextStream() {
    return plainTextStream;
  }

  /**
   * Gets the key name (or name/version).
   *
   * @return the key name
   */
  public String getKeyName() {
    return keyName;
  }

  /**
   * Gets the key wrap algorithm.
   *
   * @return the key wrap algorithm
   */
  public String getKeyWrapAlgorithm() {
    return keyWrapAlgorithm;
  }

  /**
   * Gets the data encryption cipher.
   *
   * @return the data encryption cipher, or null if not set
   */
  public String getDataEncryptionCipher() {
    return dataEncryptionCipher;
  }

  /**
   * Sets the cipher used to encrypt data.
   * Optional. Supported values: "aes-gcm" (default), "chacha20-poly1305".
   *
   * @param dataEncryptionCipher the cipher to use for data encryption
   * @return this request instance for method chaining
   */
  public EncryptRequestAlpha1 setDataEncryptionCipher(String dataEncryptionCipher) {
    this.dataEncryptionCipher = dataEncryptionCipher;
    return this;
  }

  /**
   * Checks if the decryption key name should be omitted from the encrypted document.
   *
   * @return true if the key name should be omitted
   */
  public boolean isOmitDecryptionKeyName() {
    return omitDecryptionKeyName;
  }

  /**
   * Sets whether to omit the decryption key name from the encrypted document.
   * If true, calls to decrypt must provide a key reference (name or name/version).
   * Defaults to false.
   *
   * @param omitDecryptionKeyName whether to omit the key name
   * @return this request instance for method chaining
   */
  public EncryptRequestAlpha1 setOmitDecryptionKeyName(boolean omitDecryptionKeyName) {
    this.omitDecryptionKeyName = omitDecryptionKeyName;
    return this;
  }

  /**
   * Gets the decryption key name to embed in the encrypted document.
   *
   * @return the decryption key name, or null if not set
   */
  public String getDecryptionKeyName() {
    return decryptionKeyName;
  }

  /**
   * Sets the key reference to embed in the encrypted document (name or name/version).
   * This is helpful if the reference of the key used to decrypt the document is
   * different from the one used to encrypt it.
   * If unset, uses the reference of the key used to encrypt the document.
   * This option is ignored if omitDecryptionKeyName is true.
   *
   * @param decryptionKeyName the key name to embed for decryption
   * @return this request instance for method chaining
   */
  public EncryptRequestAlpha1 setDecryptionKeyName(String decryptionKeyName) {
    this.decryptionKeyName = decryptionKeyName;
    return this;
  }
}
