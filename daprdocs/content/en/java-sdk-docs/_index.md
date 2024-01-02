---
type: docs
title: "Dapr Java SDK"
linkTitle: "Java"
weight: 1000
description: Java SDK packages for developing Dapr applications
cascade:
  github_repo: https://github.com/dapr/java-sdk
  github_subdir: daprdocs/content/en/java-sdk-docs
  path_base_for_github_subdir: content/en/developing-applications/sdks/java/
  github_branch: master
---

## Prerequisites

- [Dapr CLI]({{< ref install-dapr-cli.md >}}) installed
- Initialized [Dapr environment]({{< ref install-dapr-selfhost.md >}})
- JDK 11 or above - the published jars are compatible with Java 8:
    - [AdoptOpenJDK 11 - LTS](https://adoptopenjdk.net/)
    - [Oracle's JDK 15](https://www.oracle.com/java/technologies/javase-downloads.html)
    - [Oracle's JDK 11 - LTS](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
    - [OpenJDK](https://openjdk.java.net/)
- Install one of the following build tools for Java:
    - [Maven 3.x](https://maven.apache.org/install.html)
    - [Gradle 6.x](https://gradle.org/install/)

## Import Dapr's Java SDK

Next, import the Java SDK packages to get started. Select your preferred build tool to learn how to import.

{{< tabs Maven Gradle >}}

{{% codetab %}}
<!--Maven-->

For a Maven project, add the following to your `pom.xml` file: 

```xml
<project>
  ...
  <dependencies>
    ...
    <!-- Dapr's core SDK with all features, except Actors. -->
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk</artifactId>
      <version>1.10.0</version>
    </dependency>
    <!-- Dapr's SDK for Actors (optional). -->
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk-actors</artifactId>
      <version>1.10.0</version>
    </dependency>
    <!-- Dapr's SDK integration with SpringBoot (optional). -->
    <dependency>
      <groupId>io.dapr</groupId>
      <artifactId>dapr-sdk-springboot</artifactId>
      <version>1.10.0</version>
    </dependency>
    ...
  </dependencies>
  ...
</project>
```
{{% /codetab %}}

{{% codetab %}}
<!--Gradle-->

For a Gradle project, add the following to your `build.gradle` file:

```java
dependencies {
...
    // Dapr's core SDK with all features, except Actors.
    compile('io.dapr:dapr-sdk:1.10.0')
    // Dapr's SDK for Actors (optional).
    compile('io.dapr:dapr-sdk-actors:1.10.0')
    // Dapr's SDK integration with SpringBoot (optional).
    compile('io.dapr:dapr-sdk-springboot:1.10.0')
}
```

{{% /codetab %}}

{{< /tabs >}}

If you are also using Spring Boot, you may run into a common issue where the `OkHttp` version that the Dapr SDK uses conflicts with the one specified in the Spring Boot _Bill of Materials_.

You can fix this by specifying a compatible `OkHttp` version in your project to match the version that the Dapr SDK uses:

```xml
<dependency>
  <groupId>com.squareup.okhttp3</groupId>
  <artifactId>okhttp</artifactId>
  <version>1.10.0</version>
</dependency>
```

## Try it out

Put the Dapr Java SDK to the test. Walk through the Java quickstarts and tutorials to see Dapr in action:

| SDK samples | Description |
| ----------- | ----------- |
| [Quickstarts]({{< ref quickstarts >}}) | Experience Dapr's API building blocks in just a few minutes using the Java SDK. |
| [SDK samples](https://github.com/dapr/java-sdk/tree/master/examples) | Clone the SDK repo to try out some examples and get started. |

```java
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

try (DaprClient client = (new DaprClientBuilder()).build()) {
  // sending a class with message; BINDING_OPERATION="create"
  client.invokeBinding(BINDING_NAME, BINDING_OPERATION, myClass).block();

  // sending a plain string
  client.invokeBinding(BINDING_NAME, BINDING_OPERATION, message).block();
}
```

- For a full guide on output bindings visit [How-To: Output bindings]({{< ref howto-bindings.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/bindings/http) for code samples and instructions to try out output bindings.

### Interact with input bindings

```java
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/")
public class myClass {
    private static final Logger log = LoggerFactory.getLogger(myClass);
        @PostMapping(path = "/checkout")
        public Mono<String> getCheckout(@RequestBody(required = false) byte[] body) {
            return Mono.fromRunnable(() ->
                    log.info("Received Message: " + new String(body)));
        }
}
```

- For a full guide on input bindings, visit [How-To: Input bindings]({{< ref howto-triggers >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/bindings/http) for code samples and instructions to try out input bindings.

### Retrieve secrets

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import java.util.Map;

try (DaprClient client = (new DaprClientBuilder()).build()) {
  Map<String, String> secret = client.getSecret(SECRET_STORE_NAME, secretKey).block();
  System.out.println(JSON_SERIALIZER.writeValueAsString(secret));
}
```

- For a full guide on secrets visit [How-To: Retrieve secrets]({{< ref howto-secrets.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/secrets) for code samples and instructions to try out retrieving secrets

### Actors
An actor is an isolated, independent unit of compute and state with single-threaded execution. Dapr provides an actor implementation based on the [Virtual Actor pattern](https://www.microsoft.com/en-us/research/project/orleans-virtual-actors/), which provides a single-threaded programming model and where actors are garbage collected when not in use. With Dapr's implementaiton, you write your Dapr actors according to the Actor model, and Dapr leverages the scalability and reliability that the underlying platform provides. 

```java
import io.dapr.actors.ActorMethod;
import io.dapr.actors.ActorType;
import reactor.core.publisher.Mono;

@ActorType(name = "DemoActor")
public interface DemoActor {

  void registerReminder();

  @ActorMethod(name = "echo_message")
  String say(String something);

  void clock(String message);

  @ActorMethod(returns = Integer.class)
  Mono<Integer> incrementAndGet(int delta);
}
```

- For a full guide on actors visit [How-To: Use virtual actors in Dapr]({{< ref howto-actors.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/actors) for code samples and instructions to try actors

### Get & Subscribe to application configurations

> Note this is a preview API and thus will only be accessible via the DaprPreviewClient interface and not the normal DaprClient interface

```java
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

try (DaprClient client = (new DaprClientBuilder()).build()) {
  // Get configuration for a single key
  Mono<ConfigurationItem> item = client.getConfiguration(CONFIG_STORE_NAME, CONFIG_KEY).block();

  // Get configurations for multiple keys
  Mono<Map<String, ConfigurationItem>> items =
          client.getConfiguration(CONFIG_STORE_NAME, CONFIG_KEY_1, CONFIG_KEY_2);

  // Subscribe to configuration changes
  Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(CONFIG_STORE_NAME, CONFIG_KEY_1, CONFIG_KEY_2);
  outFlux.subscribe(configItems -> configItems.forEach(...));

  // Unsubscribe from configuration changes
  Mono<UnsubscribeConfigurationResponse> unsubscribe = client.unsubscribeConfiguration(SUBSCRIPTION_ID, CONFIG_STORE_NAME)
}
```

- For a full list of configuration operations visit [How-To: Manage configuration from a store]({{< ref howto-manage-configuration.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/configuration) for code samples and instructions to try out different configuration operations.

### Query saved state

> Note this is a preview API and thus will only be accessible via the DaprPreviewClient interface and not the normal DaprClient interface

```java
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.query.Query;
import io.dapr.client.domain.query.Sorting;
import io.dapr.client.domain.query.filters.EqFilter;

try (DaprClient client = builder.build(); DaprPreviewClient previewClient = builder.buildPreviewClient()) {
        String searchVal = args.length == 0 ? "searchValue" : args[0];
        
        // Create JSON data
        Listing first = new Listing();
        first.setPropertyType("apartment");
        first.setId("1000");
        ...
        Listing second = new Listing();
        second.setPropertyType("row-house");
        second.setId("1002");
        ...
        Listing third = new Listing();
        third.setPropertyType("apartment");
        third.setId("1003");
        ...
        Listing fourth = new Listing();
        fourth.setPropertyType("apartment");
        fourth.setId("1001");
        ...
        Map<String, String> meta = new HashMap<>();
        meta.put("contentType", "application/json");

        // Save state
        SaveStateRequest request = new SaveStateRequest(STATE_STORE_NAME).setStates(
          new State<>("1", first, null, meta, null),
          new State<>("2", second, null, meta, null),
          new State<>("3", third, null, meta, null),
          new State<>("4", fourth, null, meta, null)
        );
        client.saveBulkState(request).block();
        
        
        // Create query and query state request

        Query query = new Query()
          .setFilter(new EqFilter<>("propertyType", "apartment"))
          .setSort(Arrays.asList(new Sorting("id", Sorting.Order.DESC)));
        QueryStateRequest request = new QueryStateRequest(STATE_STORE_NAME)
          .setQuery(query);

        // Use preview client to call query state API
        QueryStateResponse<MyData> result = previewClient.queryState(request, MyData.class).block();
        
        // View Query state response 
        System.out.println("Found " + result.getResults().size() + " items.");
        for (QueryStateItem<Listing> item : result.getResults()) {
          System.out.println("Key: " + item.getKey());
          System.out.println("Data: " + item.getValue());
        }
}

```
- For a full list of configuration operations visit [How-To: Query state]({{< ref howto-state-query-api.md >}}).
- Visit [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples/querystate) for complete code sample.

Learn more about the [Dapr Java SDK packages available to add to your Java applications](https://dapr.github.io/java-sdk/).