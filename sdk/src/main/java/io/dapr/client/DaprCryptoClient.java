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

package io.dapr.client;

import com.google.protobuf.ByteString;
import io.dapr.client.domain.DecryptRequestAlpha1;
import io.dapr.client.domain.EncryptRequestAlpha1;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprCryptoProtos;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;

/**
 * Cryptography API calls of {@link DaprClientImpl}: streaming encrypt and decrypt (alpha).
 */
final class DaprCryptoClient {

  private final DaprGrpcCalls grpcCalls;

  DaprCryptoClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Flux<byte[]> encrypt(EncryptRequestAlpha1 request) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("EncryptRequestAlpha1 cannot be null.");
      }
      if (request.getComponentName() == null || request.getComponentName().trim().isEmpty()) {
        throw new IllegalArgumentException("Component name cannot be null or empty.");
      }
      if (request.getKeyName() == null || request.getKeyName().trim().isEmpty()) {
        throw new IllegalArgumentException("Key name cannot be null or empty.");
      }
      if (request.getKeyWrapAlgorithm() == null || request.getKeyWrapAlgorithm().trim().isEmpty()) {
        throw new IllegalArgumentException("Key wrap algorithm cannot be null or empty.");
      }
      if (request.getPlainTextStream() == null) {
        throw new IllegalArgumentException("Plaintext stream cannot be null.");
      }

      return Flux.create(sink -> {
        // Create response observer to receive encrypted data
        final StreamObserver<DaprCryptoProtos.EncryptResponse> responseObserver =
            new StreamObserver<DaprCryptoProtos.EncryptResponse>() {
              @Override
              public void onNext(DaprCryptoProtos.EncryptResponse response) {
                if (response.hasPayload()) {
                  byte[] data = response.getPayload().getData().toByteArray();
                  if (data.length > 0) {
                    sink.next(data);
                  }
                }
              }

              @Override
              public void onError(Throwable t) {
                sink.error(DaprException.propagate(new DaprException("ENCRYPT_ERROR",
                    "Error during encryption: " + t.getMessage(), t)));
              }

              @Override
              public void onCompleted() {
                sink.complete();
              }
            };

        // Build options for the first message
        DaprCryptoProtos.EncryptRequestOptions.Builder optionsBuilder = DaprCryptoProtos.EncryptRequestOptions
            .newBuilder()
            .setComponentName(request.getComponentName())
            .setKeyName(request.getKeyName())
            .setKeyWrapAlgorithm(request.getKeyWrapAlgorithm());

        if (request.getDataEncryptionCipher() != null && !request.getDataEncryptionCipher().isEmpty()) {
          optionsBuilder.setDataEncryptionCipher(request.getDataEncryptionCipher());
        }
        optionsBuilder.setOmitDecryptionKeyName(request.isOmitDecryptionKeyName());
        if (request.getDecryptionKeyName() != null && !request.getDecryptionKeyName().isEmpty()) {
          optionsBuilder.setDecryptionKeyName(request.getDecryptionKeyName());
        }

        final DaprCryptoProtos.EncryptRequestOptions options = optionsBuilder.build();
        final long[] sequenceNumber = {0};
        final boolean[] firstMessage = {true};

        // Get the request stream observer from gRPC
        final StreamObserver<DaprCryptoProtos.EncryptRequest> requestObserver =
            grpcCalls.intercept(null).encryptAlpha1(responseObserver);

        // Subscribe to the plaintext stream and send chunks
        request.getPlainTextStream()
            .doOnNext(chunk -> {
              DaprCryptoProtos.EncryptRequest.Builder reqBuilder = DaprCryptoProtos.EncryptRequest.newBuilder()
                  .setPayload(CommonProtos.StreamPayload.newBuilder()
                      .setData(ByteString.copyFrom(chunk))
                      .setSeq(sequenceNumber[0]++)
                      .build());

              // Include options only in the first message
              if (firstMessage[0]) {
                reqBuilder.setOptions(options);
                firstMessage[0] = false;
              }

              requestObserver.onNext(reqBuilder.build());
            })
            .doOnError(error -> {
              requestObserver.onError(error);
              sink.error(DaprException.propagate(new DaprException("ENCRYPT_ERROR", 
                  "Error reading plaintext stream: " + error.getMessage(), error)));
            })
            .doOnComplete(() -> {
              requestObserver.onCompleted();
            })
            .subscribe();
      });
    } catch (Exception ex) {
      return DaprException.wrapFlux(ex);
    }
  }

  Flux<byte[]> decrypt(DecryptRequestAlpha1 request) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("DecryptRequestAlpha1 cannot be null.");
      }
      if (request.getComponentName() == null || request.getComponentName().trim().isEmpty()) {
        throw new IllegalArgumentException("Component name cannot be null or empty.");
      }
      if (request.getCipherTextStream() == null) {
        throw new IllegalArgumentException("Ciphertext stream cannot be null.");
      }

      return Flux.create(sink -> {
        // Create response observer to receive decrypted data
        final StreamObserver<DaprCryptoProtos.DecryptResponse> responseObserver =
            new StreamObserver<DaprCryptoProtos.DecryptResponse>() {
              @Override
              public void onNext(DaprCryptoProtos.DecryptResponse response) {
                if (response.hasPayload()) {
                  byte[] data = response.getPayload().getData().toByteArray();
                  if (data.length > 0) {
                    sink.next(data);
                  }
                }
              }

              @Override
              public void onError(Throwable t) {
                sink.error(DaprException.propagate(new DaprException("DECRYPT_ERROR",
                    "Error during decryption: " + t.getMessage(), t)));
              }

              @Override
              public void onCompleted() {
                sink.complete();
              }
            };

        // Build options for the first message
        DaprCryptoProtos.DecryptRequestOptions.Builder optionsBuilder = DaprCryptoProtos.DecryptRequestOptions
            .newBuilder()
            .setComponentName(request.getComponentName());

        if (request.getKeyName() != null && !request.getKeyName().isEmpty()) {
          optionsBuilder.setKeyName(request.getKeyName());
        }

        final DaprCryptoProtos.DecryptRequestOptions options = optionsBuilder.build();
        final long[] sequenceNumber = {0};
        final boolean[] firstMessage = {true};

        // Get the request stream observer from gRPC
        final StreamObserver<DaprCryptoProtos.DecryptRequest> requestObserver =
            grpcCalls.intercept(null).decryptAlpha1(responseObserver);

        // Subscribe to the ciphertext stream and send chunks
        request.getCipherTextStream()
            .doOnNext(chunk -> {
              DaprCryptoProtos.DecryptRequest.Builder reqBuilder = DaprCryptoProtos.DecryptRequest.newBuilder()
                  .setPayload(CommonProtos.StreamPayload.newBuilder()
                      .setData(ByteString.copyFrom(chunk))
                      .setSeq(sequenceNumber[0]++)
                      .build());

              // Include options only in the first message
              if (firstMessage[0]) {
                reqBuilder.setOptions(options);
                firstMessage[0] = false;
              }

              requestObserver.onNext(reqBuilder.build());
            })
            .doOnError(error -> {
              requestObserver.onError(error);
              sink.error(DaprException.propagate(new DaprException("DECRYPT_ERROR", 
                  "Error reading ciphertext stream: " + error.getMessage(), error)));
            })
            .doOnComplete(() -> {
              requestObserver.onCompleted();
            })
            .subscribe();
      });
    } catch (Exception ex) {
      return DaprException.wrapFlux(ex);
    }
  }
}
