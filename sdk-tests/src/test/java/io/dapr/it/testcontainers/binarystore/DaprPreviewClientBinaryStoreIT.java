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

package io.dapr.it.testcontainers.binarystore;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.SetBinaryFileRequestAlpha1;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for the Dapr binary store Alpha1 API.
 */
@Testcontainers
@Tag("testcontainers")
public class DaprPreviewClientBinaryStoreIT {

  private static final String STORE_NAME = "binarystore";

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("binarystore-test-app")
      .withComponent(new Component(STORE_NAME, "binarystore.in-memory", "v1", Collections.emptyList()));

  private static DaprPreviewClient daprPreviewClient;

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
  }

  @Test
  public void shouldStoreAndReadSmallFile() {
    String fileName = uniqueFileName();
    byte[] content = "hello binary store".getBytes(StandardCharsets.UTF_8);

    daprPreviewClient.setBinaryFile(STORE_NAME, fileName, content, false).block();

    assertArrayEquals(content, daprPreviewClient.getBinaryFileBytes(STORE_NAME, fileName).block());
  }

  @Test
  public void shouldStoreAndReadLargeFileAsChunks() {
    String fileName = uniqueFileName();
    byte[] content = new byte[1024 * 1024];
    new Random(42).nextBytes(content);
    // Uneven chunks check that the SDK splits and orders them correctly.
    Flux<byte[]> chunks = Flux.just(
        Arrays.copyOfRange(content, 0, 100_000),
        Arrays.copyOfRange(content, 100_000, 700_000),
        Arrays.copyOfRange(content, 700_000, content.length));

    daprPreviewClient.setBinaryFile(new SetBinaryFileRequestAlpha1(STORE_NAME, fileName, chunks)).block();

    List<byte[]> received = daprPreviewClient.getBinaryFile(STORE_NAME, fileName).collectList().block();
    ByteArrayOutputStream joined = new ByteArrayOutputStream();
    received.forEach(chunk -> joined.write(chunk, 0, chunk.length));
    assertArrayEquals(content, joined.toByteArray());
  }

  @Test
  public void shouldFailWhenFileExistsAndOverwriteIsFalse() {
    String fileName = uniqueFileName();
    daprPreviewClient.setBinaryFile(STORE_NAME, fileName, bytes("first"), false).block();

    DaprException error = assertThrows(DaprException.class,
        () -> daprPreviewClient.setBinaryFile(STORE_NAME, fileName, bytes("second"), false).block());

    assertEquals("ALREADY_EXISTS", error.getErrorCode());
    assertArrayEquals(bytes("first"), daprPreviewClient.getBinaryFileBytes(STORE_NAME, fileName).block());
  }

  @Test
  public void shouldReplaceFileWhenOverwriteIsTrue() {
    String fileName = uniqueFileName();
    daprPreviewClient.setBinaryFile(STORE_NAME, fileName, bytes("first"), false).block();

    daprPreviewClient.setBinaryFile(STORE_NAME, fileName, bytes("second"), true).block();

    assertArrayEquals(bytes("second"), daprPreviewClient.getBinaryFileBytes(STORE_NAME, fileName).block());
  }

  @Test
  public void shouldFailToReadDeletedFile() {
    String fileName = uniqueFileName();
    daprPreviewClient.setBinaryFile(STORE_NAME, fileName, bytes("to delete"), false).block();

    daprPreviewClient.deleteBinaryFile(STORE_NAME, fileName).block();

    DaprException error = assertThrows(DaprException.class,
        () -> daprPreviewClient.getBinaryFileBytes(STORE_NAME, fileName).block());
    assertEquals("NOT_FOUND", error.getErrorCode());
  }

  private static String uniqueFileName() {
    return "file-" + UUID.randomUUID() + ".bin";
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
