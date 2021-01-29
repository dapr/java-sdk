# Distributed Tracing Sample

In this sample, we'll create two java applications: a service application, which exposes two methods, and a client application which will invoke the methods from the service using Dapr.
This sample includes:

* TracingDemoService (Exposes the methods to be remotely accessed)
* TracingDemoServiceController (Implements two methods: `echo` and `sleep`)
* TracingDemoMiddleServiceController (Implements two methods: `proxy_echo` and `proxy_sleep`)
* InvokeClient (Invokes the exposed methods from TracingDemoService)

Also consider [getting started with observability in Dapr](https://github.com/dapr/quickstarts/tree/master/observability).
 
## Remote invocation using the Java-SDK

This sample uses the Client provided in Dapr Java SDK invoking a remote method and Zipkin to collect and display tracing data. 

## Pre-requisites

* [Dapr and Dapr CLI](https://docs.dapr.io/getting-started/install-dapr/).
* Java JDK 11 (or greater): [Oracle JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11) or [OpenJDK](https://jdk.java.net/13/).
* [Apache Maven](https://maven.apache.org/install.html) version 3.x.
* [Configure Redis](https://docs.dapr.io/getting-started/configure-redis/) as a state store for Dapr.

### Checking out the code

Clone this repository:

```sh
git clone https://github.com/dapr/java-sdk.git
cd java-sdk
```

Then build the Maven project:

```sh
# make sure you are in the `java-sdk` directory.
mvn install
```

Then get into the examples directory:

```sh
cd examples
```

### Verify Zipkin is running

Run `docker ps` to see if the container `dapr_zipkin` is running locally: 

```bash
CONTAINER ID        IMAGE                  COMMAND                  CREATED             STATUS              PORTS                              NAMES
24d043379da2        daprio/dapr            "./placement"            2 days ago          Up 32 hours         0.0.0.0:6050->50005/tcp            dapr_placement
5779a0268159        openzipkin/zipkin      "/busybox/sh run.sh"     2 days ago          Up 32 hours         9410/tcp, 0.0.0.0:9411->9411/tcp   dapr_zipkin
317fef6a8297        redis                  "docker-entrypoint.s…"   2 days ago          Up 32 hours         0.0.0.0:6379->6379/tcp             dapr_redis
```

If Zipkin is not working, [install the newest version of Dapr Cli and initialize it](https://github.com/dapr/cli#install-dapr-on-your-local-machine-self-hosted).

### Running the Demo service app

The Demo service application exposes two methods that can be remotely invoked. In this example, the service code has two parts:

In the `TracingDemoService.java` file, you will find the `TracingDemoService` class, containing the main method. The main method uses the Spring Boot´s DaprApplication class for initializing the `TracingDemoServiceController`. See the code snippet below:

```java
public class TracingDemoService {
  ///...
  public static void main(String[] args) throws Exception {
    ///...
    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    DaprApplication.start(port);
  }
}
```

`DaprApplication.start()` Method will run a Spring Boot application that registers the `TracingDemoServiceController`, which exposes the invoking actions as POST requests. The Dapr's sidecar is the one that performs the actual call to the controller, triggered by client invocations or [bindings](https://docs.dapr.io/developing-applications/building-blocks/bindings/bindings-overview/).

This Rest Controller exposes the `echo` and `sleep` methods. The `echo` method retrieves metadata from the headers and prints them along with the current date in console. The actual response from method is the formatted current date. See the code snippet below:

```java
@RestController
public class TracingDemoServiceController {
  ///...
  @PostMapping(path = "/echo")
  public Mono<String> handleMethod(@RequestBody(required = false) String body,
                                   @RequestHeader Map<String, String> headers) {
    return Mono.fromSupplier(() -> {
      try {
        String message = body == null ? "" : body;

        Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

        String metadataString = headers == null ? "" : OBJECT_MAPPER.writeValueAsString(headers);

        // Handles the request by printing message.
        System.out.println(
            "Server: " + message + " @ " + utcNowAsString + " and metadata: " + metadataString);

        return utcNowAsString;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
```

The `sleep` methods simply waits for one second to simulate a slow operation.
```java
@RestController
public class TracingDemoServiceController {
  ///...
  @PostMapping(path = "/sleep")
  public void sleep() throws Exception {
    // Simulate slow processing for metrics.
    Thread.sleep(1000);
  }
  //...
}
```

The instrumentation for the service happens via the `OpenTelemetryIterceptor` class. This class uses the [OpenTelemetrySDK](https://github.com/open-telemetry/opentelemetry-java) for Java.

Use the follow command to execute the service:

```sh
dapr run --app-id tracingdemo --app-port 3000 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.tracing.TracingDemoService -p 3000
```

Once running, the TracingDemoService is now ready to be invoked by Dapr.

### Running the Demo middle service app

This service will handle the `proxy_echo` and `proxy_sleep` methods and invoke the corresponding methods in the service above.
In the code below, the `opentelemetry-context` attribute is used to propagate the tracing context among service invocations in multiple layers.

```java
@RestController
public class TracingDemoMiddleServiceController {
  // ...
  @PostMapping(path = "/proxy_echo")
  public Mono<byte[]> echo(
      @RequestAttribute(name = "opentelemetry-context") Context context,
      @RequestBody(required = false) String body) {
    InvokeMethodRequestBuilder builder = new InvokeMethodRequestBuilder(INVOKE_APP_ID, "echo");
    InvokeMethodRequest request = builder
        .withBody(body)
        .withHttpExtension(HttpExtension.POST)
        .withContext(getReactorContext(context)).build();
    return client.invokeMethod(request, TypeRef.get(byte[].class)).map(r -> r.getObject());
  }
  // ...
  @PostMapping(path = "/proxy_sleep")
  public Mono<Void> sleep(@RequestAttribute(name = "opentelemetry-context") Context context) {
    InvokeMethodRequestBuilder builder = new InvokeMethodRequestBuilder(INVOKE_APP_ID, "sleep");
    InvokeMethodRequest request = builder
        .withHttpExtension(HttpExtension.POST)
        .withContext(getReactorContext(context)).build();
    return client.invokeMethod(request, TypeRef.get(byte[].class)).then();
  }
}
```

The request attribute `opentelemetry-context` in created by parsing the tracing headers in the [OpenTelemetryInterceptor](../OpenTelemetryInterceptor.java) class. See the code below:

```java
@Component
public class OpenTelemetryInterceptor implements HandlerInterceptor {
  // ...
  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    final TextMapPropagator textFormat = OpenTelemetry.getGlobalPropagators().getTextMapPropagator();
    // ...
    Context context = textFormat.extract(Context.current(), request, HTTP_SERVLET_REQUEST_GETTER);
    request.setAttribute("opentelemetry-context", context);
    return true;
  }
  // ...
}
```

Then, `getReactorContext()` method is used to convert the OpenTelemetry's context to Reactor's context in the [OpenTelemetryConfig](../OpenTelemetryConfig.java) class:

```java
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class OpenTelemetryConfig {
  // ...
  public static reactor.util.context.Context getReactorContext(Context context) {
    Map<String, String> map = new HashMap<>();
    TextMapPropagator.Setter<Map<String, String>> setter =
        (carrier, key, value) -> map.put(key, value);

    GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, map, setter);
    reactor.util.context.Context reactorContext = reactor.util.context.Context.empty();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      reactorContext = reactorContext.put(entry.getKey(), entry.getValue());
    }
    return reactorContext;
  }
  // ...
}
```

Use the follow command to execute the service:

```sh
dapr run --app-id tracingdemoproxy --app-port 3001   -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.tracing.TracingDemoService -p 3001
```

### Running the InvokeClient app

This sample code uses the Dapr SDK for invoking two remote methods (`proxy_echo` and `proxy_sleep`). It is also instrumented with OpenTelemetry. See the code snippet below:

```java
public class InvokeClient {

private static final String SERVICE_APP_ID = "tracingdemoproxy";
///...
  public static void main(String[] args) throws Exception {
    Tracer tracer = OpenTelemetryConfig.createTracer(InvokeClient.class.getCanonicalName());

    Span span = tracer.spanBuilder("Example's Main").setSpanKind(Span.Kind.CLIENT).startSpan();
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      for (String message : args) {
        try (Scope scope = tracer.withSpan(span)) {
          InvokeServiceRequestBuilder builder = new InvokeServiceRequestBuilder(SERVICE_APP_ID, "proxy_echo");
          InvokeServiceRequest request
              = builder.withBody(message).withHttpExtension(HttpExtension.POST).withContext(Context.current()).build();
          client.invokeService(request, TypeRef.get(byte[].class))
              .map(r -> {
                System.out.println(new String(r.getObject()));
                return r;
              })
              .flatMap(r -> {
                InvokeServiceRequest sleepRequest = new InvokeServiceRequestBuilder(SERVICE_APP_ID, "proxy_sleep")
                    .withHttpExtension(HttpExtension.POST)
                    .withContext(r.getContext()).build();
                return client.invokeMethod(sleepRequest, TypeRef.get(Void.class));
              }).block();
        }
      }

      // This is an example, so for simplicity we are just exiting here.
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done");
    }
    span.end();
    shutdown();
  }
///...
}
```

The class knows the app id for the remote application. It uses `invokeMethod` method to invoke API calls on the service endpoint. The request object includes an instance of `io.opentelemetry.context.Context` for the proper tracing headers to be propagated.
 
Execute the follow script in order to run the InvokeClient example, passing two messages for the remote method:
```sh
dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.tracing.InvokeClient "message one" "message two"
```
Once running, the output should display the messages sent from invoker in the demo service output as follows:

![exposeroutput](https://raw.githubusercontent.com/dapr/java-sdk/master/examples/src/main/resources/img/exposer-service.png)

Method have been remotely invoked and displaying the remote messages.

Now, open Zipkin on [http://localhost:9411/zipkin](http://localhost:9411/zipkin). You should see a screen like the one below:

![zipking-landing](https://raw.githubusercontent.com/dapr/java-sdk/master/examples/src/main/resources/img/zipkin-landing.png)

Click on the search icon to see the latest query results. You should see a tracing diagram similar to the one below:

![zipking-landing](https://raw.githubusercontent.com/dapr/java-sdk/master/examples/src/main/resources/img/zipkin-result.png)

Once you click on the tracing event, you will see the details of the call stack starting in the client and then showing the service API calls right below.

![zipking-details](https://raw.githubusercontent.com/dapr/java-sdk/master/examples/src/main/resources/img/zipkin-details.png)
