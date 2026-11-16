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
import io.dapr.client.domain.SetBinaryFileRequestAlpha1;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprBinaryStoreProtos;
import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs the binary file calls (alpha) of {@link DaprClientImpl}: get, set and delete.
 */
final class DaprBinaryFileClient {

  /**
   * Maximum size of one upload chunk sent to the binary store API.
   */
  private static final int BINARY_FILE_CHUNK_SIZE = 32 * 1024;

  private final DaprGrpcCalls grpcCalls;

  DaprBinaryFileClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Mono<Void> deleteBinaryFile(String storeName, String fileName) {
    try {
      validateBinaryFileArgs(storeName, fileName);

      DaprBinaryStoreProtos.DeleteBinaryFileRequestAlpha1 request =
          DaprBinaryStoreProtos.DeleteBinaryFileRequestAlpha1.newBuilder()
              .setComponentName(storeName)
              .setFileName(fileName)
              .build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprBinaryStoreProtos.DeleteBinaryFileResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).deleteBinaryFileAlpha1(request, it)
          )
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private static void validateBinaryFileArgs(String storeName, String fileName) {
    if (storeName == null || storeName.trim().isEmpty()) {
      throw new IllegalArgumentException("Store name cannot be null or empty.");
    }
    if (fileName == null || fileName.trim().isEmpty()) {
      throw new IllegalArgumentException("File name cannot be null or empty.");
    }
  }

  Flux<byte[]> getBinaryFile(String storeName, String fileName) {
    try {
      validateBinaryFileArgs(storeName, fileName);

      DaprBinaryStoreProtos.GetBinaryFileRequestAlpha1 request =
          DaprBinaryStoreProtos.GetBinaryFileRequestAlpha1.newBuilder()
              .setComponentName(storeName)
              .setFileName(fileName)
              .build();

      return Flux.deferContextual(context -> Flux.<byte[]>create(sink -> {
        final AtomicReference<ClientCallStreamObserver<DaprBinaryStoreProtos.GetBinaryFileRequestAlpha1>> callStream =
            new AtomicReference<>();
        final AtomicBoolean unboundedDemand = new AtomicBoolean();

        final ClientResponseObserver<DaprBinaryStoreProtos.GetBinaryFileRequestAlpha1,
            DaprBinaryStoreProtos.GetBinaryFileResponseAlpha1> responseObserver =
            new ClientResponseObserver<DaprBinaryStoreProtos.GetBinaryFileRequestAlpha1,
                DaprBinaryStoreProtos.GetBinaryFileResponseAlpha1>() {
              @Override
              public void beforeStart(
                  ClientCallStreamObserver<DaprBinaryStoreProtos.GetBinaryFileRequestAlpha1> requestStream) {
                // Receive messages only when the subscriber asks for them, so a slow subscriber
                // does not make the client buffer the whole file.
                requestStream.disableAutoRequestWithInitial(0);
                callStream.set(requestStream);
              }

              @Override
              public void onNext(DaprBinaryStoreProtos.GetBinaryFileResponseAlpha1 response) {
                byte[] data = response.getPayload().getData().toByteArray();
                if (data.length > 0) {
                  sink.next(data);
                  return;
                }
                // An empty chunk uses no subscriber demand, so ask for one more message.
                // Unbounded demand has already asked for all messages.
                ClientCallStreamObserver<DaprBinaryStoreProtos.GetBinaryFileRequestAlpha1> stream = callStream.get();
                if (stream != null && !unboundedDemand.get()) {
                  stream.request(1);
                }
              }

              @Override
              public void onError(Throwable t) {
                // After a cancel, gRPC reports CANCELLED. The subscriber is gone, so drop it.
                if (!sink.isCancelled()) {
                  sink.error(DaprException.propagate(t));
                }
              }

              @Override
              public void onCompleted() {
                sink.complete();
              }
            };

        // The subscriber can cancel before the call starts. Do not start the call then.
        if (sink.isCancelled()) {
          return;
        }
        grpcCalls.intercept(context).getBinaryFileAlpha1(request, responseObserver);

        // The call has started, so gRPC now accepts cancel and request calls.
        ClientCallStreamObserver<DaprBinaryStoreProtos.GetBinaryFileRequestAlpha1> stream = callStream.get();
        if (stream != null) {
          sink.onCancel(() -> stream.cancel("Subscriber cancelled the download.", null));
          sink.onRequest(n -> {
            if (n == Long.MAX_VALUE) {
              unboundedDemand.set(true);
            }
            stream.request((int) Math.min(n, Integer.MAX_VALUE));
          });
        }
      }));
    } catch (Exception ex) {
      return DaprException.wrapFlux(ex);
    }
  }

  Mono<byte[]> getBinaryFileBytes(String storeName, String fileName) {
    return getBinaryFile(storeName, fileName)
        .collect(ByteArrayOutputStream::new, (out, chunk) -> out.write(chunk, 0, chunk.length))
        .map(ByteArrayOutputStream::toByteArray);
  }

  Mono<Void> setBinaryFile(String storeName, String fileName, byte[] content, boolean overwrite) {
    if (content == null) {
      return DaprException.wrapMono(new IllegalArgumentException("Content cannot be null."));
    }
    return setBinaryFile(
        new SetBinaryFileRequestAlpha1(storeName, fileName, Flux.just(content)).setOverwrite(overwrite));
  }

  Mono<Void> setBinaryFile(SetBinaryFileRequestAlpha1 request) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("SetBinaryFileRequestAlpha1 cannot be null.");
      }
      validateBinaryFileArgs(request.getStoreName(), request.getFileName());
      if (request.getContent() == null) {
        throw new IllegalArgumentException("Content cannot be null.");
      }

      final DaprBinaryStoreProtos.SetBinaryFileRequestAlpha1 initialMessage =
          DaprBinaryStoreProtos.SetBinaryFileRequestAlpha1.newBuilder()
              .setInitialRequest(DaprBinaryStoreProtos.SetBinaryFileRequestInitialAlpha1.newBuilder()
                  .setComponentName(request.getStoreName())
                  .setFileName(request.getFileName())
                  .setOverwrite(request.isOverwrite())
                  .build())
              .build();

      return Mono.deferContextual(context -> Mono.<Void>create(sink -> {
        final Disposable.Swap contentSubscription = Disposables.swap();
        // Set when the upload ends on the client side (subscriber cancel or content error).
        // gRPC then reports CANCELLED, which must not replace the real outcome.
        final AtomicBoolean terminated = new AtomicBoolean();

        final StreamObserver<DaprBinaryStoreProtos.SetBinaryFileResponseAlpha1> responseObserver =
            new StreamObserver<DaprBinaryStoreProtos.SetBinaryFileResponseAlpha1>() {
              @Override
              public void onNext(DaprBinaryStoreProtos.SetBinaryFileResponseAlpha1 response) {
                // The response has no fields.
              }

              @Override
              public void onError(Throwable t) {
                contentSubscription.dispose();
                if (!terminated.get()) {
                  sink.error(DaprException.propagate(t));
                }
              }

              @Override
              public void onCompleted() {
                sink.success();
              }
            };

        final StreamObserver<DaprBinaryStoreProtos.SetBinaryFileRequestAlpha1> requestObserver =
            grpcCalls.intercept(context).setBinaryFileAlpha1(responseObserver);

        // The sidecar must receive the initial request within 5 seconds, so send it before any content.
        requestObserver.onNext(initialMessage);

        sink.onCancel(() -> {
          terminated.set(true);
          contentSubscription.dispose();
          requestObserver.onError(
              Status.CANCELLED.withDescription("Subscriber cancelled the upload.").asRuntimeException());
        });

        final long[] sequenceNumber = {0};
        final BaseSubscriber<ByteString> contentSubscriber = new BaseSubscriber<ByteString>() {
          @Override
          protected void hookOnNext(ByteString chunk) {
            requestObserver.onNext(DaprBinaryStoreProtos.SetBinaryFileRequestAlpha1.newBuilder()
                .setPayload(CommonProtos.StreamPayload.newBuilder()
                    .setData(chunk)
                    .setSeq(sequenceNumber[0]++)
                    .build())
                .build());
          }

          @Override
          protected void hookOnError(Throwable error) {
            terminated.set(true);
            requestObserver.onError(error);
            sink.error(DaprException.propagate(error));
          }

          @Override
          protected void hookOnComplete() {
            requestObserver.onCompleted();
          }
        };
        // Register the subscriber before it receives content, so a server error can stop even a
        // synchronous content Flux.
        contentSubscription.update(contentSubscriber);
        request.getContent()
            .concatMapIterable(DaprBinaryFileClient::splitBinaryFileChunk)
            .subscribe(contentSubscriber);
      }));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * Splits one content chunk into parts of at most {@link #BINARY_FILE_CHUNK_SIZE} bytes.
   * An empty chunk gives no parts.
   */
  private static List<ByteString> splitBinaryFileChunk(byte[] chunk) {
    List<ByteString> parts = new ArrayList<>();
    for (int offset = 0; offset < chunk.length; offset += BINARY_FILE_CHUNK_SIZE) {
      int length = Math.min(BINARY_FILE_CHUNK_SIZE, chunk.length - offset);
      parts.add(ByteString.copyFrom(chunk, offset, length));
    }
    return parts;
  }
}
