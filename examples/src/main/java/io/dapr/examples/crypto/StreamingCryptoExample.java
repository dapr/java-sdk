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
import java.util.Random;

/**
 * StreamingCryptoExample demonstrates using the Dapr Cryptography building block
 * with streaming data for handling large payloads efficiently.
 *
 * <p>This example shows:
 * <ul>
 *   <li>Encrypting large data using streaming</li>
 *   <li>Using optional parameters like data encryption cipher</li>
 *   <li>Handling chunked data for encryption/decryption</li>
 * </ul>
 */
public class StreamingCryptoExample {

  private static final String CRYPTO_COMPONENT_NAME = "localstoragecrypto";
  private static final String KEY_NAME = "rsa-private-key";
  private static final String KEY_WRAP_ALGORITHM = "RSA";
  private static final String KEYS_DIR = "components/crypto/keys";

  /**
   * The main method demonstrating streaming encryption and decryption with Dapr.
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

      System.out.println("=== Dapr Streaming Cryptography Example ===");
      System.out.println();

      // Example 1: Streaming multiple chunks
      System.out.println("--- Example 1: Multi-chunk Encryption ---");
      demonstrateChunkedEncryption(client);
      System.out.println();

      // Example 2: Large data encryption
      System.out.println("--- Example 2: Large Data Encryption ---");
      demonstrateLargeDataEncryption(client);
      System.out.println();

      // Example 3: Custom encryption cipher
      System.out.println("--- Example 3: Custom Encryption Cipher ---");
      demonstrateCustomCipher(client);

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
   * Demonstrates encrypting data sent in multiple chunks.
   */
  private static void demonstrateChunkedEncryption(DaprPreviewClient client) {
    byte[] chunk1 = "First chunk of data. ".getBytes(StandardCharsets.UTF_8);
    byte[] chunk2 = "Second chunk of data. ".getBytes(StandardCharsets.UTF_8);
    byte[] chunk3 = "Third and final chunk.".getBytes(StandardCharsets.UTF_8);

    byte[] fullData = new byte[chunk1.length + chunk2.length + chunk3.length];
    System.arraycopy(chunk1, 0, fullData, 0, chunk1.length);
    System.arraycopy(chunk2, 0, fullData, chunk1.length, chunk2.length);
    System.arraycopy(chunk3, 0, fullData, chunk1.length + chunk2.length, chunk3.length);

    System.out.println("Original data: " + new String(fullData, StandardCharsets.UTF_8));
    System.out.println("Sending as 3 chunks...");

    EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(chunk1, chunk2, chunk3),
        KEY_NAME,
        KEY_WRAP_ALGORITHM
    );

    byte[] encryptedData = collectBytes(client.encrypt(encryptRequest));
    System.out.println("Encrypted data size: " + encryptedData.length + " bytes");

    DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(encryptedData)
    );

    byte[] decryptedData = collectBytes(client.decrypt(decryptRequest));
    String decryptedMessage = new String(decryptedData, StandardCharsets.UTF_8);
    System.out.println("Decrypted data: " + decryptedMessage);
    System.out.println("Verification: " + (new String(fullData, StandardCharsets.UTF_8).equals(decryptedMessage)
        ? "SUCCESS" : "FAILED"));
  }

  /**
   * Demonstrates encrypting a large data payload.
   */
  private static void demonstrateLargeDataEncryption(DaprPreviewClient client) {
    int size = 100 * 1024;
    byte[] largeData = new byte[size];
    new Random().nextBytes(largeData);

    System.out.println("Original data size: " + size + " bytes (100KB)");

    EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(largeData),
        KEY_NAME,
        KEY_WRAP_ALGORITHM
    );

    long startTime = System.currentTimeMillis();
    byte[] encryptedData = collectBytes(client.encrypt(encryptRequest));
    long encryptTime = System.currentTimeMillis() - startTime;
    System.out.println("Encrypted data size: " + encryptedData.length + " bytes (took " + encryptTime + "ms)");

    DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(encryptedData)
    );

    startTime = System.currentTimeMillis();
    byte[] decryptedData = collectBytes(client.decrypt(decryptRequest));
    long decryptTime = System.currentTimeMillis() - startTime;
    System.out.println("Decrypted data size: " + decryptedData.length + " bytes (took " + decryptTime + "ms)");

    boolean matches = java.util.Arrays.equals(largeData, decryptedData);
    System.out.println("Verification: " + (matches ? "SUCCESS" : "FAILED"));
  }

  /**
   * Demonstrates using a custom data encryption cipher.
   */
  private static void demonstrateCustomCipher(DaprPreviewClient client) {
    String message = "Message encrypted with custom cipher (aes-gcm)";
    byte[] plainText = message.getBytes(StandardCharsets.UTF_8);

    System.out.println("Original message: " + message);

    EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(plainText),
        KEY_NAME,
        KEY_WRAP_ALGORITHM
    ).setDataEncryptionCipher("aes-gcm");

    byte[] encryptedData = collectBytes(client.encrypt(encryptRequest));
    System.out.println("Encrypted with aes-gcm cipher, size: " + encryptedData.length + " bytes");

    DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(encryptedData)
    );

    byte[] decryptedData = collectBytes(client.decrypt(decryptRequest));
    String decryptedMessage = new String(decryptedData, StandardCharsets.UTF_8);
    System.out.println("Decrypted message: " + decryptedMessage);
    System.out.println("Verification: " + (message.equals(decryptedMessage) ? "SUCCESS" : "FAILED"));
  }

  /**
   * Helper method to collect streaming bytes into a single byte array.
   */
  private static byte[] collectBytes(Flux<byte[]> stream) {
    return stream.collectList()
        .map(chunks -> {
          int totalSize = chunks.stream().mapToInt(chunk -> chunk.length).sum();
          byte[] result = new byte[totalSize];
          int pos = 0;
          for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, pos, chunk.length);
            pos += chunk.length;
          }
          return result;
        })
        .block();
  }
}
