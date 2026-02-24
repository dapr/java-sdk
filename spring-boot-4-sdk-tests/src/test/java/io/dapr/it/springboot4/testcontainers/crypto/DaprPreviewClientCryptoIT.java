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

package io.dapr.it.springboot4.testcontainers.crypto;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.DecryptRequestAlpha1;
import io.dapr.client.domain.EncryptRequestAlpha1;
import io.dapr.config.Properties;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.MetadataEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static io.dapr.it.springboot4.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Dapr Cryptography Alpha1 API.
 */
@Testcontainers
@Tag("testcontainers")
public class DaprPreviewClientCryptoIT {

  private static final String CRYPTO_COMPONENT_NAME = "localstoragecrypto";
  private static final String KEY_NAME = "testkey";
  private static final String CONTAINER_KEYS_PATH = "/keys";

  private static Path tempKeysDir;
  private static DaprPreviewClient daprPreviewClient;

  @Container
  private static final DaprContainer DAPR_CONTAINER = createDaprContainer();

  private static DaprContainer createDaprContainer() {
    try {
      // Create temporary directory for keys
      tempKeysDir = Files.createTempDirectory("dapr-crypto-keys");
      
      // Generate and save a test RSA key pair in PEM format
      generateAndSaveRsaKeyPair(tempKeysDir);

      // Create the crypto component
      Component cryptoComponent = new Component(
          CRYPTO_COMPONENT_NAME,
          "crypto.dapr.localstorage",
          "v1",
          List.of(new MetadataEntry("path", CONTAINER_KEYS_PATH))
      );

      return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
          .withAppName("crypto-test-app")
          .withComponent(cryptoComponent)
          .withFileSystemBind(tempKeysDir.toString(), CONTAINER_KEYS_PATH, BindMode.READ_ONLY);

    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize test container", e);
    }
  }

  private static void generateAndSaveRsaKeyPair(Path keysDir) throws NoSuchAlgorithmException, IOException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(4096);
    KeyPair keyPair = keyGen.generateKeyPair();

    // Save the private key in PEM format
    String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
        Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded()) +
        "\n-----END PRIVATE KEY-----\n";

    // Save the public key in PEM format
    String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
        Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded()) +
        "\n-----END PUBLIC KEY-----\n";

    // Combine both keys in one PEM file
    String combinedPem = privateKeyPem + publicKeyPem;

    Path keyFile = keysDir.resolve(KEY_NAME);
    Files.writeString(keyFile, combinedPem);
    
    // Make the key file and directory readable by all (needed for container access)
    keyFile.toFile().setReadable(true, false);
    keysDir.toFile().setReadable(true, false);
    keysDir.toFile().setExecutable(true, false);
  }

  @BeforeAll
  static void setUp() {
    daprPreviewClient = new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, DAPR_CONTAINER.getHttpEndpoint())
        .withPropertyOverride(Properties.GRPC_ENDPOINT, DAPR_CONTAINER.getGrpcEndpoint())
        .buildPreviewClient();
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (daprPreviewClient != null) {
      daprPreviewClient.close();
    }
    // Clean up temp keys directory
    if (tempKeysDir != null && Files.exists(tempKeysDir)) {
      Files.walk(tempKeysDir)
          .sorted((a, b) -> -a.compareTo(b))
          .forEach(path -> {
            try {
              Files.delete(path);
            } catch (IOException e) {
              // Ignore cleanup errors
            }
          });
    }
  }

  @Test
  public void testEncryptAndDecryptSmallData() {
    String originalData = "Hello, World! This is a test message.";
    byte[] plainText = originalData.getBytes(StandardCharsets.UTF_8);

    // Encrypt
    EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(plainText),
        KEY_NAME,
        "RSA-OAEP-256"
    );

    byte[] encryptedData = daprPreviewClient.encrypt(encryptRequest)
        .collectList()
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

    assertNotNull(encryptedData);
    assertTrue(encryptedData.length > 0);

    // Decrypt
    DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(encryptedData)
    );

    byte[] decryptedData = daprPreviewClient.decrypt(decryptRequest)
        .collectList()
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

    assertNotNull(decryptedData);
    assertArrayEquals(plainText, decryptedData);
    assertEquals(originalData, new String(decryptedData, StandardCharsets.UTF_8));
  }

  @Test
  public void testEncryptAndDecryptLargeData() {
    // Generate a large data payload (1MB)
    byte[] largeData = new byte[1024 * 1024];
    new Random().nextBytes(largeData);

    // Encrypt
    EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(largeData),
        KEY_NAME,
        "RSA-OAEP-256"
    );

    byte[] encryptedData = daprPreviewClient.encrypt(encryptRequest)
        .collectList()
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

    assertNotNull(encryptedData);
    assertTrue(encryptedData.length > 0);

    // Decrypt
    DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(encryptedData)
    );

    byte[] decryptedData = daprPreviewClient.decrypt(decryptRequest)
        .collectList()
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

    assertNotNull(decryptedData);
    assertArrayEquals(largeData, decryptedData);
  }

  @Test
  public void testEncryptAndDecryptStreamedData() {
    // Create chunked data to simulate streaming
    byte[] chunk1 = "First chunk of data. ".getBytes(StandardCharsets.UTF_8);
    byte[] chunk2 = "Second chunk of data. ".getBytes(StandardCharsets.UTF_8);
    byte[] chunk3 = "Third and final chunk.".getBytes(StandardCharsets.UTF_8);

    // Combine for comparison later
    byte[] fullData = new byte[chunk1.length + chunk2.length + chunk3.length];
    System.arraycopy(chunk1, 0, fullData, 0, chunk1.length);
    System.arraycopy(chunk2, 0, fullData, chunk1.length, chunk2.length);
    System.arraycopy(chunk3, 0, fullData, chunk1.length + chunk2.length, chunk3.length);

    // Encrypt with multiple chunks
    EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(chunk1, chunk2, chunk3),
        KEY_NAME,
        "RSA-OAEP-256"
    );

    byte[] encryptedData = daprPreviewClient.encrypt(encryptRequest)
        .collectList()
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

    assertNotNull(encryptedData);
    assertTrue(encryptedData.length > 0);

    // Decrypt
    DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(encryptedData)
    );

    byte[] decryptedData = daprPreviewClient.decrypt(decryptRequest)
        .collectList()
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

    assertNotNull(decryptedData);
    assertArrayEquals(fullData, decryptedData);
  }

  @Test
  public void testEncryptWithOptionalParameters() {
    String originalData = "Test message with optional parameters.";
    byte[] plainText = originalData.getBytes(StandardCharsets.UTF_8);

    // Encrypt with optional data encryption cipher
    EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(plainText),
        KEY_NAME,
        "RSA-OAEP-256"
    ).setDataEncryptionCipher("aes-gcm");

    byte[] encryptedData = daprPreviewClient.encrypt(encryptRequest)
        .collectList()
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

    assertNotNull(encryptedData);
    assertTrue(encryptedData.length > 0);

    // Decrypt
    DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
        CRYPTO_COMPONENT_NAME,
        Flux.just(encryptedData)
    );

    byte[] decryptedData = daprPreviewClient.decrypt(decryptRequest)
        .collectList()
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

    assertNotNull(decryptedData);
    assertArrayEquals(plainText, decryptedData);
  }
}
