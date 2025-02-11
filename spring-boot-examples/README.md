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
curl -X POST localhost:8080/orders -H 'Content-Type: application/json' -d '{ "item": "the mars volta EP", "amount": 1 }'
```

If you check the `producer-app` logs you should see the following lines: 

```bash
...
Storing Order: Order{id='null', item='the mars volta EP', amount=1}
Publishing Order Event: Order{id='d4f8ea15-b774-441e-bcd2-7a4208a80bec', item='the mars volta EP', amount=1}

```

If you check the `consumer-app` logs you should see the following lines, showing that the message 
published by the `producer-app` was correctly consumed by the `consumer-app`:

```bash
Order Event Received: Order{id='d4f8ea15-b774-441e-bcd2-7a4208a80bec', item='the mars volta EP', amount=1}
```

Next, you can  create a new customer to trigger the customer's tracking workflow: 

```bash
curl -X POST localhost:8080/customers -H 'Content-Type: application/json' -d '{ "customerName": "salaboy" }'
```
Notice that the request return the workflow instance id that was created for tracking this customer. 
You need to copy the Workflow Instance Id to execute the follow-up request.

You should see in the `producer-app` logs: 

```bash
Workflow instance <Workflow Instance Id> started
Let's register the customer: salaboy
Customer: salaboy registered.
Let's wait for the customer: salaboy to request a follow up.
```

Send an event simulating the customer request for a follow-up (copy the workflow instance id from the previous request):
```bash
curl -X POST localhost:8080/customers/followup -H 'Content-Type: application/json' \
          -d '{ "customerName": "salaboy", "workflowId": "<Workflow Instance Id>" }'
```

In the `producer-app` logs you should see that the workflow instance id moved forward to the Customer Follow Up activity: 

```bash
Customer follow-up requested: salaboy
Let's book a follow up for the customer: salaboy
Customer: salaboy follow-up done.
Congratulations the customer: salaboy is happy!
```

## Running on Kubernetes

You can run the same example on a Kubernetes cluster. [Check the Kubernetes tutorial here](kubernetes/README.md).
