/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dapr.durabletask;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DurableTaskGrpcClientTlsTest {
  private DurableTaskGrpcClient client;

  @AfterEach
  void tearDown() {
    if (client != null) {
      client.close();
      client = null;
    }
  }

  private File getTestCertificate() {
    return new File(getClass().getResource("/certs/test-cert.pem").getFile());
  }

  private File getTestKey() {
    return new File(getClass().getResource("/certs/test-cert.key").getFile());
  }

  private File getTestCaCertificate() {
    return new File(getClass().getResource("/certs/test-ca-cert.pem").getFile());
  }

  @Test
  void testBuildGrpcClientWithTls() {
    File certFile = getTestCertificate();
    File keyFile = getTestKey();

    assertDoesNotThrow(() -> {
      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
          .tlsCertPath(certFile.getAbsolutePath())
          .tlsKeyPath(keyFile.getAbsolutePath())
          .build();
    });
  }

  @Test
  void testBuildGrpcClientWithTlsAndCaCert() {
    File caCertFile = getTestCaCertificate();

    assertDoesNotThrow(() -> {
      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
          .tlsCaPath(caCertFile.getAbsolutePath())
          .build();
    });
  }

  @Test
  void testBuildGrpcClientWithMtlsAndCaCert() {
    File caCertFile = getTestCaCertificate();
    File certFile = getTestCertificate();
    File keyFile = getTestKey();

    assertDoesNotThrow(() -> {
      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
          .tlsCaPath(caCertFile.getAbsolutePath())
          .tlsCertPath(certFile.getAbsolutePath())
          .tlsKeyPath(keyFile.getAbsolutePath())
          .build();
    });
  }

  @Test
  void testBuildGrpcClientWithInvalidTlsCertificate() {
    assertThrows(RuntimeException.class, () ->
        new DurableTaskGrpcClientBuilder()
            .tlsCertPath("/nonexistent/cert.pem")
            .tlsKeyPath("/nonexistent/key.pem")
            .build());
  }

  @Test
  void testBuildGrpcClientWithInvalidCaCertificate() {
    assertThrows(RuntimeException.class, () ->
        new DurableTaskGrpcClientBuilder()
            .tlsCaPath("/nonexistent/ca.pem")
            .build());
  }

  @Test
  void testBuildGrpcClientWithInsecureTls() {
    assertDoesNotThrow(() -> {
      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
          .insecure(true)
          .build();
    });
  }

  @Test
  void testBuildGrpcClientWithPlaintext() {
    assertDoesNotThrow(() -> {
      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
          .build();
    });
  }
}