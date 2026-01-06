/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.examples.crypto;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.DecryptRequestAlpha1;
import io.dapr.client.domain.EncryptRequestAlpha1;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * CryptoExample demonstrates using the Dapr Cryptography building block
 * to encrypt and decrypt data using a cryptography component.
 *
 * <p>This example shows:
 * <ul>
 *   <li>Encrypting plaintext data with a specified key and algorithm</li>
 *   <li>Decrypting ciphertext data back to plaintext</li>
 *   <li>Automatic key generation if keys don't exist</li>
 * </ul>
 *
 * <p>Prerequisites:
 * <ul>
 *   <li>Dapr installed and initialized</li>
 *   <li>A cryptography component configured (e.g., local storage crypto)</li>
 * </ul>
 */
public class CryptoExample {

  private static final String CRYPTO_COMPONENT_NAME = "localstoragecrypto";
  private static final String KEY_NAME = "rsa-private-key";
  private static final String KEY_WRAP_ALGORITHM = "RSA";
  private static final String KEYS_DIR = "components/crypto/keys";

  /**
   * The main method demonstrating encryption and decryption with Dapr.
   *
   * @param args Command line arguments (unused).
   */
  public static void main(String[] args) throws Exception {
    // Generate keys if they don't exist
    generateKeysIfNeeded();

    Map<Property<?>, String> overrides = Map.of(
        Properties.HTTP_PORT, "3500",
        Properties.GRPC_PORT, "50001"
    );

    try (DaprPreviewClient client = new DaprClientBuilder().withPropertyOverrides(overrides).buildPreviewClient()) {

      String originalMessage = "This is a secret message";
      byte[] plainText = originalMessage.getBytes(StandardCharsets.UTF_8);

      System.out.println("=== Dapr Cryptography Example ===");
      System.out.println("Original message: " + originalMessage);
      System.out.println();

      // Encrypt the message
      System.out.println("Encrypting message...");
      EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
          CRYPTO_COMPONENT_NAME,
          Flux.just(plainText),
          KEY_NAME,
          KEY_WRAP_ALGORITHM
      );

      byte[] encryptedData = client.encrypt(encryptRequest)
          .collectList()
          .map(CryptoExample::combineChunks)
          .block();

      System.out.println("Encryption successful!");
      System.out.println("Encrypted data length: " + encryptedData.length + " bytes");
      System.out.println();

      // Decrypt the message
      System.out.println("Decrypting message...");
      DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
          CRYPTO_COMPONENT_NAME,
          Flux.just(encryptedData)
      );

      byte[] decryptedData = client.decrypt(decryptRequest)
          .collectList()
          .map(CryptoExample::combineChunks)
          .block();

      String decryptedMessage = new String(decryptedData, StandardCharsets.UTF_8);
      System.out.println("Decryption successful!");
      System.out.println("Decrypted message: " + decryptedMessage);
      System.out.println();

      if (originalMessage.equals(decryptedMessage)) {
        System.out.println("SUCCESS: The decrypted message matches the original.");
      } else {
        System.out.println("ERROR: The decrypted message does not match the original.");
      }

    } catch (Exception e) {
      System.err.println("Error during crypto operations: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Generates RSA key pair if the key file doesn't exist.
   */
  private static void generateKeysIfNeeded() throws NoSuchAlgorithmException, IOException {
    Path keysDir = Paths.get(KEYS_DIR);
    Path keyFile = keysDir.resolve(KEY_NAME + ".pem");

    if (Files.exists(keyFile)) {
      System.out.println("Using existing key: " + keyFile.toAbsolutePath());
      return;
    }

    System.out.println("Generating RSA key pair...");
    Files.createDirectories(keysDir);

    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(4096);
    KeyPair keyPair = keyGen.generateKeyPair();

    String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
        + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded())
        + "\n-----END PRIVATE KEY-----\n";

    String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
        + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded())
        + "\n-----END PUBLIC KEY-----\n";

    Files.writeString(keyFile, privateKeyPem + publicKeyPem);
    System.out.println("Key generated: " + keyFile.toAbsolutePath());
  }

  /**
   * Combines byte array chunks into a single byte array.
   */
  private static byte[] combineChunks(java.util.List<byte[]> chunks) {
    int totalSize = chunks.stream().mapToInt(chunk -> chunk.length).sum();
    byte[] result = new byte[totalSize];
    int pos = 0;
    for (byte[] chunk : chunks) {
      System.arraycopy(chunk, 0, result, pos, chunk.length);
      pos += chunk.length;
    }
    return result;
  }
}
