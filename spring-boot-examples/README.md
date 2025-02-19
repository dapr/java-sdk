# Dapr Spring Boot and Testcontainers integration Example

This example consist on two applications:
- Producer App: 
  - Publish messages using a Spring Messaging approach 
  - Store and retrieve information using Spring Data CrudRepository 
  - Implements a Workflow with Dapr Workflows
- Consumer App:
  - Subscribe to messages

## Running these examples from source code

To run these examples you will need: 
- Java SDK
- Maven
- Docker or a container runtime such as Podman

From the `spring-boot-examples/` directory you can start each service using the test configuration that uses 
[Testcontainers](https://testcontainers.com) to boostrap [Dapr](https://dapr.io) by running the following command: 

```bash
cd producer-app/
mvn -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```

This will start the `producer-app` with Dapr services and the infrastructure needed by the application to run, 
in this case RabbitMQ and PostgreSQL. The `producer-app` starts on port `8080` by default.

The `-Dspring-boot.run.arguments="--reuse=true"` flag helps the application to connect to an existing shared 
infrastructure if it already exists. For development purposes, and to connect both applications we will set the flag
in both. For more details check the `DaprTestContainersConfig.java` classes in both, the `producer-app` and the `consumer-app`.

Then run in a different terminal: 

```
cd consumer-app/
mvn -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
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
Notice that the request returns the workflow instance id that was created for tracking this customer. 
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
