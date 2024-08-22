---
type: docs
title: "Getting started with the Dapr and Spring Boot"
linkTitle: "Spring Boot Integration"
weight: 4000
description: How to get started with Dapr and Spring Boot  
---

By combining Dapr and Spring Boot, we can create infrastructure independent Java applications that can be deployed across different environments, supporting a wide range of on-premises and cloud provider services. 

First, we will start with a simple integration covering the DaprClient and the Testcontainers integration, to then use Spring and Spring Boot mechanisms and programming model to leverage the Dapr APIs under the hood. This help teams to remove dependencies such as clients and drivers required to connect to environment specific infrastructure (databases, key-value stores, message brokers, configuration/secret stores, etc.) 

{{% alert title="Note" color="primary" %}}
@TODO: If you haven't already, [try out one of the quickstarts]({{< ref quickstarts >}}) for a quick walk-through on how to use the Dapr and Spring Boot.

{{% /alert %}}


## Adding the Dapr and Spring Boot integration to your project

If you already have a Spring Boot application (Spring Boot 3.x+), you can directly add the following dependencies to your project: 


```
  <dependency>
    <groupId>io.dapr</groupId>
    <artifactId>dapr-spring-boot-starter</artifactId>
    <version>${dapr-java-sdk.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>io.dapr</groupId>
    <artifactId>testcontainers-dapr</artifactId>
    <version>${dapr-java-sdk.version}</version>
    <scope>test</scope>
  </dependency>
```

By adding these dependencies you can: 
- Autowire a DaprClient to use inside your applications
- Use the Spring Data and Messaging abstractions and programming model that uses the Dapr APIs under the hood
- Improve your inner-development loop by relying on Testcontainers to bootstrap Dapr Control plane services and default components
- (Work in progress) Wire up workflow activities using the Spring programming model

Once this dependencies are in your application, you can rely on Spring Boot autoconfiguration to autowire a `DaprClient` instance:

```java
@Autowired
private DaprClient daprClient;

```

This will connect to the default Dapr gRPC endpoint `localhost:50001`, requiring you to start Dapr outside of your application. 

You can use the DaprClient to interact with the Dapr APIs anywhere in your application, for example from inside a REST endpoint: 

```java 
@RestController
public class DemoRestController {
  @Autowired
  private DaprClient daprClient;

  @PostMapping("/store")
  public void storeOrder(@RequestBody Order order){
    daprClient.saveState("kvstore", order.orderId(), order).block();
  }
}

record Order(String orderId, Integer amount){}
```

If you want to avoid managing Dapr outside of your Spring Boot application, you can rely on Testcontainers to bootstrap Dapr besides your application for development purposes. To do this we can create a test configuration that uses Testcontainers to bootstrap all we need to develop our applications using the Dapr APIs. 

Using Testcontaniners and Dapr integrations, we let the test configuration to bootstrap Dapr for our applications. 

```java
  @Configuration
  interface MyTestConfiguration {

    @Container
    DaprContainer dapr = new DaprContainer("daprio/daprd:1.13.2")
            .withAppName("local-dapr-app")
            //Enable Workflows
            .withComponent(new Component("kvstore", "state.in-memory", "v1",
                    Collections.singletonMap("actorStateStore", "true")))
            .withComponent(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()))
            .withAppPort(8080)
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withAppChannelAddress("host.testcontainers.internal");

    /**
     * Expose the Dapr ports to the host.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void daprProperties(DynamicPropertyRegistry registry) {
      Testcontainers.exposeHostPorts(8080);
      dapr.start();
      registry.add()
      registry.add()
    }
  }
```

Now you can start your application with: 
```bash
mvn spring-boot:test-run
```

Running this command will start the application, using the provided test configuration that includes the Testcontainers and Dapr integration. In the logs you should be able to see that the daprd and the placement service containers were started for your application. 


## Leveraging Spring & Spring Boot programming model with Dapr

The Java SDK allows you to interface with all of the [Dapr building blocks]({{< ref building-blocks >}}). But if you want to leverage the Spring and Spring Boot programming model you can use the `dapr-spring-boot-starter` integration. This includes implementations of Spring Data (KeyValueTemplate and CrudRepository) as well as a Messaging Template for producing and consuming messages (similar to Spring Kafka, Spring Pulsar and Spring AMQP).

## Using Spring Data CrudRepository and KeyValueTemplate

You can use well known Spring Data constructs relying on a Dapr-based implementation. With Dapr, you don't need to add any infrastructure-related driver or client, making your Spring application lighter and decoupled from the environment where it is running. 

Under the hood these implementations uses Dapr Statestore and Binding APIs. 

### Configuration parameters

With Spring Data abstractions you can configure which statestore and bindings will be used by Dapr to connect to the available infrastructure. This can be done by setting the following properties: 

## Using Spring Messaging for producing and consuming events

Similar to Spring Kafka, Spring Pulsar and Spring AMQP you can use the DaprMessagingTemplate to publish messages to the configured infrastructure. To consume messages you can use the `@DaprListener` annotation. 

To publish events/messages you can `@Autowire` the `DaprMessagingTemplate` in your Spring application.

```
```

To listen to events/messages you need to expose an endpoint in the application that will be resposible to receive the messages. If you expose a REST endpoint you can use the `@DaprListener` annotation to let Dapr know where it needs to forward the events/messages too: 

```
```

Check the following example to see how, leveraging the Testcontainers integration, you can test Spring Boot applications that publish and consume messages/events without the need of including any drivers or clients or managing connections. 




### Configuration parameters

With Spring Kafka, Pulsar, and AMQP abstractions you can configure which pubsub will be used by Dapr to connect to the available infrastructure. This can be done by setting the following properties:

### Comparison with Spring Kafka, Spring RabbitMQ and Spring AMQP



## Next steps



Learn more about the [Dapr Java SDK packages available to add to your Java applications](https://dapr.github.io/java-sdk/).

## Related links
- [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples)
