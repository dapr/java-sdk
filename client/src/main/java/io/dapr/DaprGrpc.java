package io.dapr;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * Dapr definitions
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.24.0)",
    comments = "Source: dapr/dapr.proto")
public final class DaprGrpc {

  private DaprGrpc() {}

  public static final String SERVICE_NAME = "dapr.Dapr";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprProtos.PublishEventEnvelope,
      com.google.protobuf.Empty> getPublishEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishEvent",
      requestType = io.dapr.DaprProtos.PublishEventEnvelope.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprProtos.PublishEventEnvelope,
      com.google.protobuf.Empty> getPublishEventMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprProtos.PublishEventEnvelope, com.google.protobuf.Empty> getPublishEventMethod;
    if ((getPublishEventMethod = DaprGrpc.getPublishEventMethod) == null) {
      synchronized (DaprGrpc.class) {
        if ((getPublishEventMethod = DaprGrpc.getPublishEventMethod) == null) {
          DaprGrpc.getPublishEventMethod = getPublishEventMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprProtos.PublishEventEnvelope, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprProtos.PublishEventEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new DaprMethodDescriptorSupplier("PublishEvent"))
              .build();
        }
      }
    }
    return getPublishEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprProtos.InvokeServiceEnvelope,
      io.dapr.DaprProtos.InvokeServiceResponseEnvelope> getInvokeServiceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InvokeService",
      requestType = io.dapr.DaprProtos.InvokeServiceEnvelope.class,
      responseType = io.dapr.DaprProtos.InvokeServiceResponseEnvelope.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprProtos.InvokeServiceEnvelope,
      io.dapr.DaprProtos.InvokeServiceResponseEnvelope> getInvokeServiceMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprProtos.InvokeServiceEnvelope, io.dapr.DaprProtos.InvokeServiceResponseEnvelope> getInvokeServiceMethod;
    if ((getInvokeServiceMethod = DaprGrpc.getInvokeServiceMethod) == null) {
      synchronized (DaprGrpc.class) {
        if ((getInvokeServiceMethod = DaprGrpc.getInvokeServiceMethod) == null) {
          DaprGrpc.getInvokeServiceMethod = getInvokeServiceMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprProtos.InvokeServiceEnvelope, io.dapr.DaprProtos.InvokeServiceResponseEnvelope>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InvokeService"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprProtos.InvokeServiceEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprProtos.InvokeServiceResponseEnvelope.getDefaultInstance()))
              .setSchemaDescriptor(new DaprMethodDescriptorSupplier("InvokeService"))
              .build();
        }
      }
    }
    return getInvokeServiceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprProtos.InvokeBindingEnvelope,
      com.google.protobuf.Empty> getInvokeBindingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InvokeBinding",
      requestType = io.dapr.DaprProtos.InvokeBindingEnvelope.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprProtos.InvokeBindingEnvelope,
      com.google.protobuf.Empty> getInvokeBindingMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprProtos.InvokeBindingEnvelope, com.google.protobuf.Empty> getInvokeBindingMethod;
    if ((getInvokeBindingMethod = DaprGrpc.getInvokeBindingMethod) == null) {
      synchronized (DaprGrpc.class) {
        if ((getInvokeBindingMethod = DaprGrpc.getInvokeBindingMethod) == null) {
          DaprGrpc.getInvokeBindingMethod = getInvokeBindingMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprProtos.InvokeBindingEnvelope, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InvokeBinding"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprProtos.InvokeBindingEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new DaprMethodDescriptorSupplier("InvokeBinding"))
              .build();
        }
      }
    }
    return getInvokeBindingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprProtos.GetStateEnvelope,
      io.dapr.DaprProtos.GetStateResponseEnvelope> getGetStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetState",
      requestType = io.dapr.DaprProtos.GetStateEnvelope.class,
      responseType = io.dapr.DaprProtos.GetStateResponseEnvelope.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprProtos.GetStateEnvelope,
      io.dapr.DaprProtos.GetStateResponseEnvelope> getGetStateMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprProtos.GetStateEnvelope, io.dapr.DaprProtos.GetStateResponseEnvelope> getGetStateMethod;
    if ((getGetStateMethod = DaprGrpc.getGetStateMethod) == null) {
      synchronized (DaprGrpc.class) {
        if ((getGetStateMethod = DaprGrpc.getGetStateMethod) == null) {
          DaprGrpc.getGetStateMethod = getGetStateMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprProtos.GetStateEnvelope, io.dapr.DaprProtos.GetStateResponseEnvelope>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprProtos.GetStateEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprProtos.GetStateResponseEnvelope.getDefaultInstance()))
              .setSchemaDescriptor(new DaprMethodDescriptorSupplier("GetState"))
              .build();
        }
      }
    }
    return getGetStateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprProtos.SaveStateEnvelope,
      com.google.protobuf.Empty> getSaveStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SaveState",
      requestType = io.dapr.DaprProtos.SaveStateEnvelope.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprProtos.SaveStateEnvelope,
      com.google.protobuf.Empty> getSaveStateMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprProtos.SaveStateEnvelope, com.google.protobuf.Empty> getSaveStateMethod;
    if ((getSaveStateMethod = DaprGrpc.getSaveStateMethod) == null) {
      synchronized (DaprGrpc.class) {
        if ((getSaveStateMethod = DaprGrpc.getSaveStateMethod) == null) {
          DaprGrpc.getSaveStateMethod = getSaveStateMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprProtos.SaveStateEnvelope, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SaveState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprProtos.SaveStateEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new DaprMethodDescriptorSupplier("SaveState"))
              .build();
        }
      }
    }
    return getSaveStateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprProtos.DeleteStateEnvelope,
      com.google.protobuf.Empty> getDeleteStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteState",
      requestType = io.dapr.DaprProtos.DeleteStateEnvelope.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprProtos.DeleteStateEnvelope,
      com.google.protobuf.Empty> getDeleteStateMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprProtos.DeleteStateEnvelope, com.google.protobuf.Empty> getDeleteStateMethod;
    if ((getDeleteStateMethod = DaprGrpc.getDeleteStateMethod) == null) {
      synchronized (DaprGrpc.class) {
        if ((getDeleteStateMethod = DaprGrpc.getDeleteStateMethod) == null) {
          DaprGrpc.getDeleteStateMethod = getDeleteStateMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprProtos.DeleteStateEnvelope, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprProtos.DeleteStateEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new DaprMethodDescriptorSupplier("DeleteState"))
              .build();
        }
      }
    }
    return getDeleteStateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DaprStub newStub(io.grpc.Channel channel) {
    return new DaprStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DaprBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new DaprBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DaprFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new DaprFutureStub(channel);
  }

  /**
   * <pre>
   * Dapr definitions
   * </pre>
   */
  public static abstract class DaprImplBase implements io.grpc.BindableService {

    /**
     */
    public void publishEvent(io.dapr.DaprProtos.PublishEventEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getPublishEventMethod(), responseObserver);
    }

    /**
     */
    public void invokeService(io.dapr.DaprProtos.InvokeServiceEnvelope request,
        io.grpc.stub.StreamObserver<io.dapr.DaprProtos.InvokeServiceResponseEnvelope> responseObserver) {
      asyncUnimplementedUnaryCall(getInvokeServiceMethod(), responseObserver);
    }

    /**
     */
    public void invokeBinding(io.dapr.DaprProtos.InvokeBindingEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getInvokeBindingMethod(), responseObserver);
    }

    /**
     */
    public void getState(io.dapr.DaprProtos.GetStateEnvelope request,
        io.grpc.stub.StreamObserver<io.dapr.DaprProtos.GetStateResponseEnvelope> responseObserver) {
      asyncUnimplementedUnaryCall(getGetStateMethod(), responseObserver);
    }

    /**
     */
    public void saveState(io.dapr.DaprProtos.SaveStateEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSaveStateMethod(), responseObserver);
    }

    /**
     */
    public void deleteState(io.dapr.DaprProtos.DeleteStateEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteStateMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPublishEventMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprProtos.PublishEventEnvelope,
                com.google.protobuf.Empty>(
                  this, METHODID_PUBLISH_EVENT)))
          .addMethod(
            getInvokeServiceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprProtos.InvokeServiceEnvelope,
                io.dapr.DaprProtos.InvokeServiceResponseEnvelope>(
                  this, METHODID_INVOKE_SERVICE)))
          .addMethod(
            getInvokeBindingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprProtos.InvokeBindingEnvelope,
                com.google.protobuf.Empty>(
                  this, METHODID_INVOKE_BINDING)))
          .addMethod(
            getGetStateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprProtos.GetStateEnvelope,
                io.dapr.DaprProtos.GetStateResponseEnvelope>(
                  this, METHODID_GET_STATE)))
          .addMethod(
            getSaveStateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprProtos.SaveStateEnvelope,
                com.google.protobuf.Empty>(
                  this, METHODID_SAVE_STATE)))
          .addMethod(
            getDeleteStateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprProtos.DeleteStateEnvelope,
                com.google.protobuf.Empty>(
                  this, METHODID_DELETE_STATE)))
          .build();
    }
  }

  /**
   * <pre>
   * Dapr definitions
   * </pre>
   */
  public static final class DaprStub extends io.grpc.stub.AbstractStub<DaprStub> {
    private DaprStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DaprStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DaprStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DaprStub(channel, callOptions);
    }

    /**
     */
    public void publishEvent(io.dapr.DaprProtos.PublishEventEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPublishEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void invokeService(io.dapr.DaprProtos.InvokeServiceEnvelope request,
        io.grpc.stub.StreamObserver<io.dapr.DaprProtos.InvokeServiceResponseEnvelope> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInvokeServiceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void invokeBinding(io.dapr.DaprProtos.InvokeBindingEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInvokeBindingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getState(io.dapr.DaprProtos.GetStateEnvelope request,
        io.grpc.stub.StreamObserver<io.dapr.DaprProtos.GetStateResponseEnvelope> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetStateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void saveState(io.dapr.DaprProtos.SaveStateEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSaveStateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteState(io.dapr.DaprProtos.DeleteStateEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteStateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Dapr definitions
   * </pre>
   */
  public static final class DaprBlockingStub extends io.grpc.stub.AbstractStub<DaprBlockingStub> {
    private DaprBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DaprBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DaprBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DaprBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.google.protobuf.Empty publishEvent(io.dapr.DaprProtos.PublishEventEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getPublishEventMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.dapr.DaprProtos.InvokeServiceResponseEnvelope invokeService(io.dapr.DaprProtos.InvokeServiceEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getInvokeServiceMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty invokeBinding(io.dapr.DaprProtos.InvokeBindingEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getInvokeBindingMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.dapr.DaprProtos.GetStateResponseEnvelope getState(io.dapr.DaprProtos.GetStateEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getGetStateMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty saveState(io.dapr.DaprProtos.SaveStateEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getSaveStateMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty deleteState(io.dapr.DaprProtos.DeleteStateEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getDeleteStateMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Dapr definitions
   * </pre>
   */
  public static final class DaprFutureStub extends io.grpc.stub.AbstractStub<DaprFutureStub> {
    private DaprFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DaprFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DaprFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DaprFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> publishEvent(
        io.dapr.DaprProtos.PublishEventEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getPublishEventMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.dapr.DaprProtos.InvokeServiceResponseEnvelope> invokeService(
        io.dapr.DaprProtos.InvokeServiceEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getInvokeServiceMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> invokeBinding(
        io.dapr.DaprProtos.InvokeBindingEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getInvokeBindingMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.dapr.DaprProtos.GetStateResponseEnvelope> getState(
        io.dapr.DaprProtos.GetStateEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getGetStateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> saveState(
        io.dapr.DaprProtos.SaveStateEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getSaveStateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> deleteState(
        io.dapr.DaprProtos.DeleteStateEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteStateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PUBLISH_EVENT = 0;
  private static final int METHODID_INVOKE_SERVICE = 1;
  private static final int METHODID_INVOKE_BINDING = 2;
  private static final int METHODID_GET_STATE = 3;
  private static final int METHODID_SAVE_STATE = 4;
  private static final int METHODID_DELETE_STATE = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DaprImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(DaprImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUBLISH_EVENT:
          serviceImpl.publishEvent((io.dapr.DaprProtos.PublishEventEnvelope) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_INVOKE_SERVICE:
          serviceImpl.invokeService((io.dapr.DaprProtos.InvokeServiceEnvelope) request,
              (io.grpc.stub.StreamObserver<io.dapr.DaprProtos.InvokeServiceResponseEnvelope>) responseObserver);
          break;
        case METHODID_INVOKE_BINDING:
          serviceImpl.invokeBinding((io.dapr.DaprProtos.InvokeBindingEnvelope) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_GET_STATE:
          serviceImpl.getState((io.dapr.DaprProtos.GetStateEnvelope) request,
              (io.grpc.stub.StreamObserver<io.dapr.DaprProtos.GetStateResponseEnvelope>) responseObserver);
          break;
        case METHODID_SAVE_STATE:
          serviceImpl.saveState((io.dapr.DaprProtos.SaveStateEnvelope) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DELETE_STATE:
          serviceImpl.deleteState((io.dapr.DaprProtos.DeleteStateEnvelope) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class DaprBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DaprBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.dapr.DaprProtos.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Dapr");
    }
  }

  private static final class DaprFileDescriptorSupplier
      extends DaprBaseDescriptorSupplier {
    DaprFileDescriptorSupplier() {}
  }

  private static final class DaprMethodDescriptorSupplier
      extends DaprBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DaprMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (DaprGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DaprFileDescriptorSupplier())
              .addMethod(getPublishEventMethod())
              .addMethod(getInvokeServiceMethod())
              .addMethod(getInvokeBindingMethod())
              .addMethod(getGetStateMethod())
              .addMethod(getSaveStateMethod())
              .addMethod(getDeleteStateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
