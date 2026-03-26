# Baggage Propagation Example

This example demonstrates [W3C Baggage](https://www.w3.org/TR/baggage/) propagation using the Dapr Java SDK.

## Overview

Baggage allows you to propagate key-value pairs across service boundaries alongside distributed traces. This is useful for passing contextual information — such as user IDs, tenant IDs, or feature flags — without modifying request payloads.

The Dapr runtime supports baggage propagation as described in [Dapr PR #8649](https://github.com/dapr/dapr/pull/8649). The Java SDK propagates the `baggage` header via both gRPC metadata and HTTP headers automatically when the value is present in Reactor's context.

## How It Works

The SDK reads the `baggage` key from Reactor's `ContextView` and injects it into:
- **gRPC metadata** via `DaprBaggageInterceptor`
- **HTTP headers** via `DaprHttp` (added to the context-to-header allowlist)

To propagate baggage, add it to the Reactor context using `.contextWrite()`:

```java
import io.dapr.client.Headers;
import reactor.util.context.Context;

client.invokeMethod("target-service", "say", "hello", HttpExtension.POST, null, byte[].class)
    .contextWrite(Context.of(Headers.BAGGAGE, "userId=alice,tenantId=acme-corp"))
    .block();
```

The baggage value follows the [W3C Baggage header format](https://www.w3.org/TR/baggage/#header-content):
```
key1=value1,key2=value2
```

Each list-member can optionally include properties:
```
key1=value1;property1;property2,key2=value2
```

## Pre-requisites

* [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/)
* Java JDK 11 (or greater):
    * [Microsoft JDK 11](https://docs.microsoft.com/en-us/java/openjdk/download#openjdk-11)
    * [Oracle JDK 11](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11)
    * [OpenJDK 11](https://jdk.java.net/11/)
* [Apache Maven](https://maven.apache.org/install.html) version 3.x.

## Running the Example

### 1. Build and install jars

```sh
# From the java-sdk root directory
mvn clean install
```

### 2. Start the target service

In one terminal, start the demo service:

```sh
cd examples
dapr run --app-id target-service --app-port 3000 -- \
  java -jar target/dapr-java-sdk-examples-exec.jar \
  io.dapr.examples.invoke.http.DemoService -p 3000
```

### 3. Run the baggage client

In another terminal:

```sh
cd examples
dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar \
  io.dapr.examples.baggage.BaggageClient
```

You should see output like:

```
Invoking service with baggage: userId=alice,tenantId=acme-corp,featureFlag=new-ui
Response: ...
Done.
```

## Combining Baggage with Tracing

You can propagate both baggage and tracing context together by adding multiple entries to the Reactor context:

```java
client.invokeMethod("target-service", "say", "hello", HttpExtension.POST, null, byte[].class)
    .contextWrite(Context.of(Headers.BAGGAGE, "userId=alice")
        .put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01"))
    .block();
```

Both the `baggage` and `traceparent` headers will be propagated to downstream services via Dapr.
