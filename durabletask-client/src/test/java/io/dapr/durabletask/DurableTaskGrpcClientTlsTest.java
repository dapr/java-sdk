///*
// * Copyright 2025 The Dapr Authors
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *     http://www.apache.org/licenses/LICENSE-2.0
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
//limitations under the License.
//*/
//package io.dapr.durabletask;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//import org.junit.jupiter.api.condition.EnabledOnOs;
//import org.junit.jupiter.api.condition.OS;
//import org.junit.jupiter.api.Assumptions;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
//import java.security.cert.X509Certificate;
//import java.util.Base64;
//import java.util.Date;
//import java.math.BigInteger;
//
//import org.bouncycastle.asn1.x500.X500Name;
//import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
//import org.bouncycastle.cert.X509v3CertificateBuilder;
//import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
//import org.bouncycastle.operator.ContentSigner;
//import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class DurableTaskGrpcClientTlsTest {
//  private static final int DEFAULT_PORT = 4001;
//  private static final String DEFAULT_SIDECAR_IP = "127.0.0.1";
//
//  @TempDir
//  Path tempDir;
//
//  // Track the client for cleanup
//  private DurableTaskGrpcClient client;
//
//  @AfterEach
//  void tearDown() throws Exception {
//    if (client != null) {
//      client.close();
//      client = null;
//    }
//  }
//
//  // Helper method to generate a key pair for testing
//  private static KeyPair generateKeyPair() throws Exception {
//    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//    keyPairGenerator.initialize(2048);
//    return keyPairGenerator.generateKeyPair();
//  }
//
//  // Helper method to generate a self-signed certificate
//  private static X509Certificate generateCertificate(KeyPair keyPair) throws Exception {
//    X500Name issuer = new X500Name("CN=Test Certificate");
//    X500Name subject = new X500Name("CN=Test Certificate");
//    Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
//    Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);
//    SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
//    X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
//            issuer,
//            BigInteger.valueOf(System.currentTimeMillis()),
//            notBefore,
//            notAfter,
//            subject,
//            publicKeyInfo
//    );
//    ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
//    return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
//  }
//
//  private static void writeCertificateToFile(X509Certificate cert, File file) throws Exception {
//    String certPem = "-----BEGIN CERTIFICATE-----\n" +
//            Base64.getEncoder().encodeToString(cert.getEncoded()) +
//            "\n-----END CERTIFICATE-----";
//    Files.write(file.toPath(), certPem.getBytes());
//  }
//
//  private static void writePrivateKeyToFile(KeyPair keyPair, File file) throws Exception {
//    String keyPem = "-----BEGIN PRIVATE KEY-----\n" +
//            Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()) +
//            "\n-----END PRIVATE KEY-----";
//    Files.write(file.toPath(), keyPem.getBytes());
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithTls() throws Exception {
//    // Generate test certificate and key
//    KeyPair keyPair = generateKeyPair();
//    X509Certificate cert = generateCertificate(keyPair);
//
//    File certFile = File.createTempFile("test-cert", ".pem");
//    File keyFile = File.createTempFile("test-key", ".pem");
//    try {
//      writeCertificateToFile(cert, certFile);
//      writePrivateKeyToFile(keyPair, keyFile);
//
//      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//              .tlsCertPath(certFile.getAbsolutePath())
//              .tlsKeyPath(keyFile.getAbsolutePath())
//              .build();
//
//      assertNotNull(client);
//      // Note: We can't easily test the actual TLS configuration without a real server
//    } finally {
//      certFile.delete();
//      keyFile.delete();
//    }
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithTlsAndEndpoint() throws Exception {
//    // Generate test certificate and key
//    KeyPair keyPair = generateKeyPair();
//    X509Certificate cert = generateCertificate(keyPair);
//
//    File certFile = File.createTempFile("test-cert", ".pem");
//    File keyFile = File.createTempFile("test-key", ".pem");
//    try {
//      writeCertificateToFile(cert, certFile);
//      writePrivateKeyToFile(keyPair, keyFile);
//
//      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//              .tlsCertPath(certFile.getAbsolutePath())
//              .tlsKeyPath(keyFile.getAbsolutePath())
//              .port(443)
//              .build();
//
//      assertNotNull(client);
//    } finally {
//      certFile.delete();
//      keyFile.delete();
//    }
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithInvalidTlsCert() {
//    assertThrows(RuntimeException.class, () -> {
//      new DurableTaskGrpcClientBuilder()
//              .tlsCertPath("/nonexistent/cert.pem")
//              .tlsKeyPath("/nonexistent/key.pem")
//              .build();
//    });
//  }
//
//  @Test
//  @EnabledOnOs({OS.LINUX, OS.MAC})
//  public void testBuildGrpcManagedChannelWithTlsAndUnixSocket() throws Exception {
//    // Skip this test since Unix socket support is not implemented yet
//    Assumptions.assumeTrue(false, "Unix socket support not implemented yet");
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithTlsAndDnsAuthority() throws Exception {
//    // Generate test certificate and key
//    KeyPair keyPair = generateKeyPair();
//    X509Certificate cert = generateCertificate(keyPair);
//
//    File certFile = File.createTempFile("test-cert", ".pem");
//    File keyFile = File.createTempFile("test-key", ".pem");
//    try {
//      writeCertificateToFile(cert, certFile);
//      writePrivateKeyToFile(keyPair, keyFile);
//
//      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//              .tlsCertPath(certFile.getAbsolutePath())
//              .tlsKeyPath(keyFile.getAbsolutePath())
//              .port(443)
//              .build();
//
//      assertNotNull(client);
//    } finally {
//      certFile.delete();
//      keyFile.delete();
//    }
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithTlsAndCaCert() throws Exception {
//    // Generate test CA certificate
//    KeyPair caKeyPair = generateKeyPair();
//    X509Certificate caCert = generateCertificate(caKeyPair);
//
//    File caCertFile = File.createTempFile("test-ca-cert", ".pem");
//    try {
//      writeCertificateToFile(caCert, caCertFile);
//
//      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//              .tlsCaPath(caCertFile.getAbsolutePath())
//              .build();
//
//      assertNotNull(client);
//    } finally {
//      caCertFile.delete();
//    }
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithTlsAndCaCertAndEndpoint() throws Exception {
//    // Generate test CA certificate
//    KeyPair caKeyPair = generateKeyPair();
//    X509Certificate caCert = generateCertificate(caKeyPair);
//
//    File caCertFile = File.createTempFile("test-ca-cert", ".pem");
//    try {
//      writeCertificateToFile(caCert, caCertFile);
//
//      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//              .tlsCaPath(caCertFile.getAbsolutePath())
//              .port(443)
//              .build();
//
//      assertNotNull(client);
//    } finally {
//      caCertFile.delete();
//    }
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithInvalidCaCert() {
//    assertThrows(RuntimeException.class, () -> {
//      new DurableTaskGrpcClientBuilder()
//              .tlsCaPath("/nonexistent/ca.pem")
//              .build();
//    });
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithMtlsAndCaCert() throws Exception {
//    // Generate test certificates
//    KeyPair caKeyPair = generateKeyPair();
//    X509Certificate caCert = generateCertificate(caKeyPair);
//    KeyPair clientKeyPair = generateKeyPair();
//    X509Certificate clientCert = generateCertificate(clientKeyPair);
//
//    File caCertFile = File.createTempFile("test-ca-cert", ".pem");
//    File clientCertFile = File.createTempFile("test-client-cert", ".pem");
//    File clientKeyFile = File.createTempFile("test-client-key", ".pem");
//    try {
//      writeCertificateToFile(caCert, caCertFile);
//      writeCertificateToFile(clientCert, clientCertFile);
//      writePrivateKeyToFile(clientKeyPair, clientKeyFile);
//
//      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//              .tlsCaPath(caCertFile.getAbsolutePath())
//              .tlsCertPath(clientCertFile.getAbsolutePath())
//              .tlsKeyPath(clientKeyFile.getAbsolutePath())
//              .build();
//
//      assertNotNull(client);
//    } finally {
//      caCertFile.delete();
//      clientCertFile.delete();
//      clientKeyFile.delete();
//    }
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithInsecureTls() throws Exception {
//    client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//            .insecure(true)
//            .port(443)
//            .build();
//
//    assertNotNull(client);
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithInsecureTlsAndMtls() throws Exception {
//    // Generate test certificates
//    KeyPair caKeyPair = generateKeyPair();
//    X509Certificate caCert = generateCertificate(caKeyPair);
//    KeyPair clientKeyPair = generateKeyPair();
//    X509Certificate clientCert = generateCertificate(clientKeyPair);
//
//    File caCertFile = File.createTempFile("test-ca-cert", ".pem");
//    File clientCertFile = File.createTempFile("test-client-cert", ".pem");
//    File clientKeyFile = File.createTempFile("test-client-key", ".pem");
//    try {
//      writeCertificateToFile(caCert, caCertFile);
//      writeCertificateToFile(clientCert, clientCertFile);
//      writePrivateKeyToFile(clientKeyPair, clientKeyFile);
//
//      client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//              .insecure(true)
//              .tlsCaPath(caCertFile.getAbsolutePath())
//              .tlsCertPath(clientCertFile.getAbsolutePath())
//              .tlsKeyPath(clientKeyFile.getAbsolutePath())
//              .port(443)
//              .build();
//
//      assertNotNull(client);
//    } finally {
//      caCertFile.delete();
//      clientCertFile.delete();
//      clientKeyFile.delete();
//    }
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithInsecureTlsAndCustomEndpoint() throws Exception {
//    client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//            .insecure(true)
//            .port(443)
//            .build();
//
//    assertNotNull(client);
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithPlaintext() throws Exception {
//    // No TLS config provided, should use plaintext
//    client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//            .port(443)
//            .build();
//
//    assertNotNull(client);
//  }
//
//  @Test
//  public void testBuildGrpcManagedChannelWithPlaintextAndCustomEndpoint() throws Exception {
//    // No TLS config provided, should use plaintext
//    client = (DurableTaskGrpcClient) new DurableTaskGrpcClientBuilder()
//            .port(50001)    // Custom port
//            .build();
//
//    assertNotNull(client);
//  }
//}