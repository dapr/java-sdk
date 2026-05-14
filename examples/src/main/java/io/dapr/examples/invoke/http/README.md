# Method invocation Sample

In this sample, we'll create two java applications: a service application which exposes a method and a client application which will invoke the method from the service using Dapr.
This sample includes:

* DemoService (Exposes the method to be remotely accessed)
* InvokeClient (Invokes the exposed method from DemoService)

Visit [this](https://docs.dapr.io/developing-applications/building-blocks/service-invocation/service-invocation-overview/) link for more information about Dapr and service invocation.
 
## Remote invocation using a native HTTP client

This sample invokes a method on another Dapr-enabled application via the Dapr sidecar using `java.net.http.HttpClient`. The previous SDK-provided `DaprClient.invokeMethod` wrappers are deprecated; calling the sidecar directly is the recommended approach.

Two equivalent approaches are demonstrated:

1. `DaprClient.invokeHttpClient(appId)` — an SDK-provided wrapper that returns a pre-configured `HttpClient` bound to the sidecar's `/v1.0/invoke/<app-id>/method/` prefix, with the `dapr-api-token` header attached when configured.
2. A raw `java.net.http.HttpClient` sending the request to the sidecar's base URL with a `dapr-app-id` header identifying the target app — no SDK helper required.

## Pre-requisites

* [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/).
* Java JDK 17 (or greater):
    * [Microsoft JDK 17](https://docs.microsoft.com/en-us/java/openjdk/download#openjdk-17)
    * [Oracle JDK 17](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK17)
    * [OpenJDK 17](https://jdk.java.net/17/)
* [Apache Maven](https://maven.apache.org/install.html) version 3.x.

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

### Initialize Dapr

Initialize Dapr in Self-Hosted Mode by running: `dapr init`

### Running the Demo service sample

The Demo service application is meant to expose a method that can be remotely invoked. In this example, the service code has two parts:

In the `DemoService.java` file, you will find the `DemoService` class, containing the main method. The main method uses the Spring Boot´s DaprApplication class for initializing the `ExposerServiceController`. See the code snippet below:

```java
public class DemoService {
  ///...
  public static void main(String[] args) throws Exception {
    ///...
    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    DaprApplication.start(port);
  }
}
```

`DaprApplication.start()` Method will run an Spring Boot application that registers the `DemoServiceController`, which exposes the invoking action as a POST request. The Dapr's sidecar is the one that performs the actual call to the controller, triggered by client invocations or [bindings](https://docs.dapr.io/developing-applications/building-blocks/bindings/bindings-overview/).

This Spring Controller exposes the `say` method. The method retrieves metadata from the headers and prints them along with the current date in console. The actual response from method is the formatted current date. See the code snippet below:

```java
@RestController
public class DemoServiceController {
  ///...
  @PostMapping(path = "/say")
  public Mono<String> handleMethod(@RequestBody(required = false) byte[] body,
                                   @RequestHeader Map<String, String> headers) {
    return Mono.fromSupplier(() -> {
      try {
        String message = body == null ? "" : new String(body, StandardCharsets.UTF_8);

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

Use the following command to execute the demo service example:

<!-- STEP
name: Run demo service
expected_stdout_lines: 
  - 'Server: "message one"'
  - 'Server: "message two"'
background: true
sleep: 5
-->

```sh
dapr run --app-id invokedemo --app-port 3000 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.invoke.http.DemoService -p 3000
```

<!-- END_STEP -->

Once running, the ExposerService is now ready to be invoked by Dapr.


### Running the InvokeClient sample

The Invoke client sample calls the remote method through the Dapr sidecar using two equivalent approaches:

1. `DaprClient.invokeHttpClient(appId)` — an SDK-provided wrapper around `java.net.http.HttpClient` pre-bound to `/v1.0/invoke/<app-id>/method/`.
2. A raw `java.net.http.HttpClient` against the sidecar's base URL with a `dapr-app-id` header.

In `InvokeClient.java` file, you will find the `InvokeClient` class and the `main` method. See the code snippet below:

```java
public class InvokeClient {

  private static final String SERVICE_APP_ID = "invokedemo";
  private static final String METHOD = "say";

  public static void main(String[] args) throws Exception {
    try (DaprClient daprClient = new DaprClientBuilder().build()) {
      DaprInvokeHttpClient invoker = daprClient.invokeHttpClient(SERVICE_APP_ID);

      int port = Properties.HTTP_PORT.get();
      String sidecarBase = "http://localhost:" + port;
      HttpClient rawHttpClient = HttpClient.newHttpClient();

      for (String message : args) {
        // Form 1: SDK helper — paths resolve against /v1.0/invoke/<app-id>/method/.
        HttpRequest sdkRequest = invoker.newRequestBuilder(METHOD)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(message))
            .build();
        HttpResponse<byte[]> sdkResponse =
            invoker.send(sdkRequest, HttpResponse.BodyHandlers.ofByteArray());
        System.out.println(new String(sdkResponse.body()));

        // Form 2: raw HttpClient + dapr-app-id header against the sidecar's base URL.
        HttpRequest headerRequest = HttpRequest.newBuilder()
            .uri(URI.create(sidecarBase + "/" + METHOD))
            .header("Content-Type", "application/json")
            .header("dapr-app-id", SERVICE_APP_ID)
            .POST(HttpRequest.BodyPublishers.ofString(message))
            .build();
        HttpResponse<byte[]> headerResponse =
            rawHttpClient.send(headerRequest, HttpResponse.BodyHandlers.ofByteArray());
        System.out.println(new String(headerResponse.body()));
      }
    }

    System.out.println("Done");
  }
}
```

Form 1 uses `DaprClient.invokeHttpClient(SERVICE_APP_ID)` to obtain an HTTP client whose base URI already targets the desired app via the sidecar's invoke API. Form 2 sends the request directly to the sidecar's base URL and uses the `dapr-app-id` header to identify the target app. Both forms call the remote `say` method and print its response.

Execute the follow script in order to run the InvokeClient example, passing two messages for the remote method:

<!-- STEP
name: Run demo client
expected_stdout_lines:
  - 'Done'
background: true
sleep: 5
-->

```sh
dapr run --app-id invokeclient -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.invoke.http.InvokeClient "message one" "message two"
```

<!-- END_STEP -->

Finally, the console for `invokeclient` should output two timestamps per message — one from each URL form — followed by `Done`. The exact timestamps come from the `say` method on `DemoService`. For example:

```text
2026-05-12 13:45:00.123
2026-05-12 13:45:00.456
2026-05-12 13:45:00.789
2026-05-12 13:45:01.012
Done
```

For more details on Dapr Spring Boot integration, please refer to [Dapr Spring Boot](../../DaprApplication.java) Application implementation.

### Cleanup

To stop the apps run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id invokedemo
dapr stop --app-id invokeclient
```

<!-- END_STEP -->
