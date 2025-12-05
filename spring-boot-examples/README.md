# Dapr Spring Boot and Testcontainers integration Example

This example consists of three applications:
- Producer App: 
  - Publish messages using a Spring Messaging approach 
  - Store and retrieve information using Spring Data CrudRepository 
  - Implements a Workflow with Dapr Workflows
- Consumer App:
  - Subscribe to messages
- Cloud Config Demo:
  - Import and use configs

## Running these examples from source code

To run these examples you will need: 
- Java SDK
- Maven
- Docker or a container runtime such as Podman

From the `spring-boot-examples/` directory you can start each service using the test configuration that uses 
[Testcontainers](https://testcontainers.com) to boostrap [Dapr](https://dapr.io) by running the following command: 

<!-- STEP
name: Run Demo Producer Service
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'Started ProducerApplication'
background: true
expected_return_code: 143
sleep: 30
timeout_seconds: 75
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

```sh
cd producer-app/
../../mvnw -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```

<!-- END_STEP -->

This will start the `producer-app` with Dapr services and the infrastructure needed by the application to run, 
in this case RabbitMQ and PostgreSQL. The `producer-app` starts on port `8080` by default.

The `-Dspring-boot.run.arguments="--reuse=true"` flag helps the application to connect to an existing shared 
infrastructure if it already exists. For development purposes, and to connect both applications we will set the flag
in both. For more details check the `DaprTestContainersConfig.java` classes in both, the `producer-app` and the `consumer-app`.

Then run in a different terminal:

<!-- STEP
name: Run Demo Consumer Service
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'Started ConsumerApplication'
background: true
expected_return_code: 143
sleep: 30
timeout_seconds: 45
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

```sh
cd consumer-app/
../../mvnw -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```

<!-- END_STEP -->
The `consumer-app` starts in port `8081` by default.

To run `cloud-config-demo`, you should run in a terminal (`cloud-config-demo` doesn't depends on two applications above):
<!-- STEP
name: Run Cloud Config Demo Service
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'Started CloudConfigApplication'
background: true
expected_return_code: 143
sleep: 30
timeout_seconds: 45
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

```sh
cd cloud-config-demo/
../../mvnw -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```

<!-- END_STEP -->

The `cloud-config-demo` starts in port `8082` by default.

It will work and gain secrets from secret store. you can also uncomment the lines in application.yaml to enable more configuration imports.

## Interacting with the applications

Now that both applications are up you can place an order by sending a POST request to `:8080/orders/`
You can use `curl` to send a POST request to the `producer-app`: 


<!-- STEP
name: Send POST request to Producer App
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'Order Stored and Event Published'
background: true
sleep: 1
timeout_seconds: 2
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

```sh
curl -X POST localhost:8080/orders -H 'Content-Type: application/json' -d '{ "item": "the mars volta EP", "amount": 1 }'
```

<!-- END_STEP -->


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

Next, you can create a new customer to trigger the customer's tracking workflow: 

<!-- STEP
name: Start Customer Workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'New Workflow Instance created for Customer'
background: true
sleep: 1
timeout_seconds: 2
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

```sh
curl -X POST localhost:8080/customers -H 'Content-Type: application/json' -d '{ "customerName": "salaboy" }'
```

<!-- END_STEP -->

 
A new Workflow Instance was created to track the customers interactions. Now, the workflow instance
is waiting for the customer to request a follow-up. 

You should see in the `producer-app` logs: 

```bash
Workflow instance <Workflow Instance Id> started
Let's register the customer: salaboy
Customer: salaboy registered.
Let's wait for the customer: salaboy to request a follow up.
```

Send an event simulating the customer request for a follow-up:

<!-- STEP
name: Emit Customer Follow-up event
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'Customer Follow-up requested'
background: true
sleep: 1
timeout_seconds: 5
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

```sh
curl -X POST localhost:8080/customers/followup -H 'Content-Type: application/json' -d '{ "customerName": "salaboy" }'
```

<!-- END_STEP -->

In the `producer-app` logs you should see that the workflow instance id moved forward to the Customer Follow Up activity: 

```bash
Customer follow-up requested: salaboy
Let's book a follow up for the customer: salaboy
Customer: salaboy follow-up done.
Congratulations the customer: salaboy is happy!
```

You can check the config in CloudConfig app, just run:

<!-- STEP
name: Send GET request to CloudConfig App
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'testvalue'
background: true
sleep: 1
timeout_seconds: 2
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

```sh
curl -X GET localhost:8082/config
```

<!-- END_STEP -->

You will get `testvalue` in terminal stdout.

## Running on Kubernetes

You can run the same example on a Kubernetes cluster. [Check the Kubernetes tutorial here](kubernetes/README.md).
