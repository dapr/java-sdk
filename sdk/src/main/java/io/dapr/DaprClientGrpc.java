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
 * User Code definitions
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.25.0)",
    comments = "Source: daprclient.proto")
public final class DaprClientGrpc {

  private DaprClientGrpc() {}

  public static final String SERVICE_NAME = "daprclient.DaprClient";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.InvokeEnvelope,
      com.google.protobuf.Any> getOnInvokeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OnInvoke",
      requestType = io.dapr.DaprClientProtos.InvokeEnvelope.class,
      responseType = com.google.protobuf.Any.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.InvokeEnvelope,
      com.google.protobuf.Any> getOnInvokeMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.InvokeEnvelope, com.google.protobuf.Any> getOnInvokeMethod;
    if ((getOnInvokeMethod = DaprClientGrpc.getOnInvokeMethod) == null) {
      synchronized (DaprClientGrpc.class) {
        if ((getOnInvokeMethod = DaprClientGrpc.getOnInvokeMethod) == null) {
          DaprClientGrpc.getOnInvokeMethod = getOnInvokeMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprClientProtos.InvokeEnvelope, com.google.protobuf.Any>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OnInvoke"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprClientProtos.InvokeEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Any.getDefaultInstance()))
              .setSchemaDescriptor(new DaprClientMethodDescriptorSupplier("OnInvoke"))
              .build();
        }
      }
    }
    return getOnInvokeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope> getGetTopicSubscriptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTopicSubscriptions",
      requestType = com.google.protobuf.Empty.class,
      responseType = io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope> getGetTopicSubscriptionsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope> getGetTopicSubscriptionsMethod;
    if ((getGetTopicSubscriptionsMethod = DaprClientGrpc.getGetTopicSubscriptionsMethod) == null) {
      synchronized (DaprClientGrpc.class) {
        if ((getGetTopicSubscriptionsMethod = DaprClientGrpc.getGetTopicSubscriptionsMethod) == null) {
          DaprClientGrpc.getGetTopicSubscriptionsMethod = getGetTopicSubscriptionsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTopicSubscriptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope.getDefaultInstance()))
              .setSchemaDescriptor(new DaprClientMethodDescriptorSupplier("GetTopicSubscriptions"))
              .build();
        }
      }
    }
    return getGetTopicSubscriptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope> getGetBindingsSubscriptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBindingsSubscriptions",
      requestType = com.google.protobuf.Empty.class,
      responseType = io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope> getGetBindingsSubscriptionsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope> getGetBindingsSubscriptionsMethod;
    if ((getGetBindingsSubscriptionsMethod = DaprClientGrpc.getGetBindingsSubscriptionsMethod) == null) {
      synchronized (DaprClientGrpc.class) {
        if ((getGetBindingsSubscriptionsMethod = DaprClientGrpc.getGetBindingsSubscriptionsMethod) == null) {
          DaprClientGrpc.getGetBindingsSubscriptionsMethod = getGetBindingsSubscriptionsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBindingsSubscriptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope.getDefaultInstance()))
              .setSchemaDescriptor(new DaprClientMethodDescriptorSupplier("GetBindingsSubscriptions"))
              .build();
        }
      }
    }
    return getGetBindingsSubscriptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.BindingEventEnvelope,
      io.dapr.DaprClientProtos.BindingResponseEnvelope> getOnBindingEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OnBindingEvent",
      requestType = io.dapr.DaprClientProtos.BindingEventEnvelope.class,
      responseType = io.dapr.DaprClientProtos.BindingResponseEnvelope.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.BindingEventEnvelope,
      io.dapr.DaprClientProtos.BindingResponseEnvelope> getOnBindingEventMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.BindingEventEnvelope, io.dapr.DaprClientProtos.BindingResponseEnvelope> getOnBindingEventMethod;
    if ((getOnBindingEventMethod = DaprClientGrpc.getOnBindingEventMethod) == null) {
      synchronized (DaprClientGrpc.class) {
        if ((getOnBindingEventMethod = DaprClientGrpc.getOnBindingEventMethod) == null) {
          DaprClientGrpc.getOnBindingEventMethod = getOnBindingEventMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprClientProtos.BindingEventEnvelope, io.dapr.DaprClientProtos.BindingResponseEnvelope>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OnBindingEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprClientProtos.BindingEventEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprClientProtos.BindingResponseEnvelope.getDefaultInstance()))
              .setSchemaDescriptor(new DaprClientMethodDescriptorSupplier("OnBindingEvent"))
              .build();
        }
      }
    }
    return getOnBindingEventMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.CloudEventEnvelope,
      com.google.protobuf.Empty> getOnTopicEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OnTopicEvent",
      requestType = io.dapr.DaprClientProtos.CloudEventEnvelope.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.CloudEventEnvelope,
      com.google.protobuf.Empty> getOnTopicEventMethod() {
    io.grpc.MethodDescriptor<io.dapr.DaprClientProtos.CloudEventEnvelope, com.google.protobuf.Empty> getOnTopicEventMethod;
    if ((getOnTopicEventMethod = DaprClientGrpc.getOnTopicEventMethod) == null) {
      synchronized (DaprClientGrpc.class) {
        if ((getOnTopicEventMethod = DaprClientGrpc.getOnTopicEventMethod) == null) {
          DaprClientGrpc.getOnTopicEventMethod = getOnTopicEventMethod =
              io.grpc.MethodDescriptor.<io.dapr.DaprClientProtos.CloudEventEnvelope, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OnTopicEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.dapr.DaprClientProtos.CloudEventEnvelope.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new DaprClientMethodDescriptorSupplier("OnTopicEvent"))
              .build();
        }
      }
    }
    return getOnTopicEventMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DaprClientStub newStub(io.grpc.Channel channel) {
    return new DaprClientStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DaprClientBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new DaprClientBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DaprClientFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new DaprClientFutureStub(channel);
  }

  /**
   * <pre>
   * User Code definitions
   * </pre>
   */
  public static abstract class DaprClientImplBase implements io.grpc.BindableService {

    /**
     */
    public void onInvoke(io.dapr.DaprClientProtos.InvokeEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Any> responseObserver) {
      asyncUnimplementedUnaryCall(getOnInvokeMethod(), responseObserver);
    }

    /**
     */
    public void getTopicSubscriptions(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTopicSubscriptionsMethod(), responseObserver);
    }

    /**
     */
    public void getBindingsSubscriptions(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBindingsSubscriptionsMethod(), responseObserver);
    }

    /**
     */
    public void onBindingEvent(io.dapr.DaprClientProtos.BindingEventEnvelope request,
        io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.BindingResponseEnvelope> responseObserver) {
      asyncUnimplementedUnaryCall(getOnBindingEventMethod(), responseObserver);
    }

    /**
     */
    public void onTopicEvent(io.dapr.DaprClientProtos.CloudEventEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getOnTopicEventMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getOnInvokeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprClientProtos.InvokeEnvelope,
                com.google.protobuf.Any>(
                  this, METHODID_ON_INVOKE)))
          .addMethod(
            getGetTopicSubscriptionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope>(
                  this, METHODID_GET_TOPIC_SUBSCRIPTIONS)))
          .addMethod(
            getGetBindingsSubscriptionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope>(
                  this, METHODID_GET_BINDINGS_SUBSCRIPTIONS)))
          .addMethod(
            getOnBindingEventMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprClientProtos.BindingEventEnvelope,
                io.dapr.DaprClientProtos.BindingResponseEnvelope>(
                  this, METHODID_ON_BINDING_EVENT)))
          .addMethod(
            getOnTopicEventMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                io.dapr.DaprClientProtos.CloudEventEnvelope,
                com.google.protobuf.Empty>(
                  this, METHODID_ON_TOPIC_EVENT)))
          .build();
    }
  }

  /**
   * <pre>
   * User Code definitions
   * </pre>
   */
  public static final class DaprClientStub extends io.grpc.stub.AbstractStub<DaprClientStub> {
    private DaprClientStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DaprClientStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DaprClientStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DaprClientStub(channel, callOptions);
    }

    /**
     */
    public void onInvoke(io.dapr.DaprClientProtos.InvokeEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Any> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getOnInvokeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTopicSubscriptions(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTopicSubscriptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getBindingsSubscriptions(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBindingsSubscriptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void onBindingEvent(io.dapr.DaprClientProtos.BindingEventEnvelope request,
        io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.BindingResponseEnvelope> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getOnBindingEventMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void onTopicEvent(io.dapr.DaprClientProtos.CloudEventEnvelope request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getOnTopicEventMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * User Code definitions
   * </pre>
   */
  public static final class DaprClientBlockingStub extends io.grpc.stub.AbstractStub<DaprClientBlockingStub> {
    private DaprClientBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DaprClientBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DaprClientBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DaprClientBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.google.protobuf.Any onInvoke(io.dapr.DaprClientProtos.InvokeEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getOnInvokeMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope getTopicSubscriptions(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetTopicSubscriptionsMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope getBindingsSubscriptions(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetBindingsSubscriptionsMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.dapr.DaprClientProtos.BindingResponseEnvelope onBindingEvent(io.dapr.DaprClientProtos.BindingEventEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getOnBindingEventMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty onTopicEvent(io.dapr.DaprClientProtos.CloudEventEnvelope request) {
      return blockingUnaryCall(
          getChannel(), getOnTopicEventMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * User Code definitions
   * </pre>
   */
  public static final class DaprClientFutureStub extends io.grpc.stub.AbstractStub<DaprClientFutureStub> {
    private DaprClientFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DaprClientFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DaprClientFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DaprClientFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Any> onInvoke(
        io.dapr.DaprClientProtos.InvokeEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getOnInvokeMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope> getTopicSubscriptions(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTopicSubscriptionsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope> getBindingsSubscriptions(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBindingsSubscriptionsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.dapr.DaprClientProtos.BindingResponseEnvelope> onBindingEvent(
        io.dapr.DaprClientProtos.BindingEventEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getOnBindingEventMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> onTopicEvent(
        io.dapr.DaprClientProtos.CloudEventEnvelope request) {
      return futureUnaryCall(
          getChannel().newCall(getOnTopicEventMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ON_INVOKE = 0;
  private static final int METHODID_GET_TOPIC_SUBSCRIPTIONS = 1;
  private static final int METHODID_GET_BINDINGS_SUBSCRIPTIONS = 2;
  private static final int METHODID_ON_BINDING_EVENT = 3;
  private static final int METHODID_ON_TOPIC_EVENT = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DaprClientImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(DaprClientImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ON_INVOKE:
          serviceImpl.onInvoke((io.dapr.DaprClientProtos.InvokeEnvelope) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Any>) responseObserver);
          break;
        case METHODID_GET_TOPIC_SUBSCRIPTIONS:
          serviceImpl.getTopicSubscriptions((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.GetTopicSubscriptionsEnvelope>) responseObserver);
          break;
        case METHODID_GET_BINDINGS_SUBSCRIPTIONS:
          serviceImpl.getBindingsSubscriptions((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.GetBindingsSubscriptionsEnvelope>) responseObserver);
          break;
        case METHODID_ON_BINDING_EVENT:
          serviceImpl.onBindingEvent((io.dapr.DaprClientProtos.BindingEventEnvelope) request,
              (io.grpc.stub.StreamObserver<io.dapr.DaprClientProtos.BindingResponseEnvelope>) responseObserver);
          break;
        case METHODID_ON_TOPIC_EVENT:
          serviceImpl.onTopicEvent((io.dapr.DaprClientProtos.CloudEventEnvelope) request,
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

  private static abstract class DaprClientBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DaprClientBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.dapr.DaprClientProtos.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DaprClient");
    }
  }

  private static final class DaprClientFileDescriptorSupplier
      extends DaprClientBaseDescriptorSupplier {
    DaprClientFileDescriptorSupplier() {}
  }

  private static final class DaprClientMethodDescriptorSupplier
      extends DaprClientBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DaprClientMethodDescriptorSupplier(String methodName) {
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
      synchronized (DaprClientGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DaprClientFileDescriptorSupplier())
              .addMethod(getOnInvokeMethod())
              .addMethod(getGetTopicSubscriptionsMethod())
              .addMethod(getGetBindingsSubscriptionsMethod())
              .addMethod(getOnBindingEventMethod())
              .addMethod(getOnTopicEventMethod())
              .build();
        }
      }
    }
    return result;
  }
}
