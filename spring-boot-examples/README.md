# Dapr Spring Boot and Testcontainers integration Example

This example consist on two applications:
- Producer App: 
  - Publish messages using a Spring Messaging approach 
  - Store and retrieve information using Spring Data CrudRepository 
  - Implements a Workflow with Dapr Workflows
- Consumer App:
  - Subscribe to messages
  - Expose endpoints for Workflows to call





## Running this examples from source code

To run this examples you will need: 
- Java SDK
- Maven
- Docker or a container runtime such as Podman

From the `spring-boot-examples/` directory you can start each service using the test configuration that uses 
[Testcontainers](https://testcontainers.com) to boostrap [Dapr](https://dapr.io) by running the following command: 

```bash
cd producer-app/
mvn spring-boot:test-run
```

This will start the `producer-app` with Dapr services and the infrastructure needed by the application to run, 
in this case RabbitMQ and PostgreSQL. The `producer-app` starts in port `8080` by default.

To run the `consumer-app` alongside the `producer-app` you need to change the test configuration slightly so both applications join the same 
container network. Check the `DaprTestContainersConfig` class inside the 
`test/java/io/dapr/springboot/examples/consumer` directory you need to uncomment the following line: 

```java
  @Bean
  public RabbitMQContainer rabbitMQContainer(Network daprNetwork) {
    return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"))
            .withExposedPorts(5672).withNetworkAliases("rabbitmq")
            //Uncomment to run this app alongside `producer-app` 
            //.withReuse(true)
            .withNetwork(daprNetwork);
  }
```

`.withReuse(true)` needs to be uncommented for the `consumer-app` to connect to the same RabbitMQ broker 
than the `producer-app`.

You also need to make sure that the Dapr sidecar can connect to the same placement service started by the `producer-app`.
Uncomment the following line: 

```java
 return new DaprContainer("daprio/daprd:1.14.4")
            .withAppName("consumer-app")
            .withNetwork(daprNetwork).withComponent(new Component("pubsub",
                    "pubsub.rabbitmq", "v1", rabbitMqProperties))
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withAppPort(8081).withAppChannelAddress("host.testcontainers.internal")
            //Uncomment to run this app alongside `producer-app`
            //.withReusablePlacement(true)
            .dependsOn(rabbitMQContainer);
```

Then run in a different terminal: 

```
cd consumer-app/
mvn spring-boot:test-run
```
The `consumer-app` starts in port `8081` by default.

## Interacting with the applications

Now that both applications are up you can place an order by sending a POST request to `:8080/orders/`
From the `spring-boot-examples/` directory you can use `curl` to send a POST request: 
```
curl -X POST localhost:8080/orders -H 'Content-Type: application/json' -d @new-order.json
```

If you check the `consumer-app` logs you should see the following lines, showing that the message 
published by the `producer-app` was correctly consumed by the `consumer-app`:

```
CONSUME +++++ io.dapr.client.domain.CloudEvent@ef5ff503
ORDER +++++ Order{id='69c1015e-c35f-4a06-bb56-3cca41b87701', item='mars volta EP', amount=1}
```


You can also create a new customer to trigger the customer's workflow: 

```bash
curl -X POST localhost:8080/customers -H 'Content-Type: application/json' -d @new-customer.json
```

Send an event simulating the customer interaction:
```bash
curl -X POST localhost:8080/customers/followup -H 'Content-Type: application/json' -d @followup-customer.json
```