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

package io.dapr.examples.binarystore;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.SetBinaryFileRequestAlpha1;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/**
 * Stores, reads, replaces, and deletes files with the Dapr binary store API (alpha).
 *
 * <p>Run with the in-memory binary store component in components/binarystore.
 */
public class BinaryStoreExample {

  private static final String STORE_NAME = "binarystore";
  private static final String FILE_NAME = "hello.txt";
  private static final String LARGE_FILE_NAME = "large.bin";
  private static final int LARGE_FILE_SIZE = 1024 * 1024;
  private static final int LARGE_FILE_CHUNKS = 4;

  /**
   * The main method of the example.
   *
   * @param args Command line arguments (unused).
   * @throws Exception if the client fails to close.
   */
  public static void main(String[] args) throws Exception {
    Map<Property<?>, String> overrides = Map.of(
        Properties.HTTP_PORT, "3500",
        Properties.GRPC_PORT, "50001"
    );

    try (DaprPreviewClient client = new DaprClientBuilder().withPropertyOverrides(overrides).buildPreviewClient()) {
      System.out.println("=== Dapr Binary Store Example ===");

      // Store a small file from a byte array. overwrite=false fails if the file exists.
      byte[] content = "Hello from the Dapr binary store!".getBytes(StandardCharsets.UTF_8);
      client.setBinaryFile(STORE_NAME, FILE_NAME, content, false).block();
      System.out.println("Stored file: " + FILE_NAME);

      byte[] stored = client.getBinaryFileBytes(STORE_NAME, FILE_NAME).block();
      System.out.println("Read file content: " + new String(stored, StandardCharsets.UTF_8));

      // Replace the file. overwrite=true replaces an existing file.
      byte[] updated = "Updated content".getBytes(StandardCharsets.UTF_8);
      client.setBinaryFile(STORE_NAME, FILE_NAME, updated, true).block();
      byte[] replaced = client.getBinaryFileBytes(STORE_NAME, FILE_NAME).block();
      System.out.println("Replaced file content: " + new String(replaced, StandardCharsets.UTF_8));

      // Stream a large file in chunks, then read it back as a stream of chunks.
      byte[] large = new byte[LARGE_FILE_SIZE];
      new Random().nextBytes(large);
      int chunkSize = LARGE_FILE_SIZE / LARGE_FILE_CHUNKS;
      Flux<byte[]> chunks = Flux.range(0, LARGE_FILE_CHUNKS)
          .map(i -> Arrays.copyOfRange(large, i * chunkSize, (i + 1) * chunkSize));
      client.setBinaryFile(new SetBinaryFileRequestAlpha1(STORE_NAME, LARGE_FILE_NAME, chunks)
          .setOverwrite(true)).block();
      long size = client.getBinaryFile(STORE_NAME, LARGE_FILE_NAME)
          .map(chunk -> (long) chunk.length)
          .reduce(0L, Long::sum)
          .block();
      System.out.println("Read large file: " + size + " bytes");

      client.deleteBinaryFile(STORE_NAME, FILE_NAME).block();
      client.deleteBinaryFile(STORE_NAME, LARGE_FILE_NAME).block();
      System.out.println("Deleted files");

      System.out.println("SUCCESS: The binary store example finished.");
    }
  }
}
