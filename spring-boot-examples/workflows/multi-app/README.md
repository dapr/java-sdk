# Multi App workflow Example

This example demonstrate how you can create distributed workflows where the orchestrator doesn't host the workflow activities.

For more documentation about how Multi App Workflows work [check the official documentation here](https://v1-16.docs.dapr.io/developing-applications/building-blocks/workflow/workflow-multi-app/).

This example is composed by three Spring Boot applications: 
- `orchestrator`: The `orchestrator` app contains the Dapr Workflow definition and expose REST endpoints to create and raise events against workflow instances.
- `worker-one`: The `worker-one` app contains the `RegisterCustomerActivity` definition, which will be orchstrated by the `orchestrator` app.
- `worker-two`: The `worker-two` app contains the `CustomerFollowupActivity` definition, which will be orchstrated by the `orchestrator` app.

To start the applications you need to run the following commands on separate terminals, starting from the `remote-activities` directory. 
To start the `orchestrator` app run: 
```bash
cd orchestrator/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

The `orchestrator` application will run on port `8080`.

On a separate terminal, to start the `worker-one` app run:
```bash
cd worker-one/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

The `worker-one` application will run on port `8081`.

On a separate terminal, to start the `worker-two` app run:
```bash
cd worker-two/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

The `worker-two` application will run on port `8082`.

You can create new workflow instances of the `CustomerWorkflow` by calling the `/customers` endpoint of the `orchestrator` application.

```bash
curl -X POST localhost:8080/customers -H 'Content-Type: application/json' -d '{ "customerName": "salaboy" }'
```

The workflow definition [`CustomerWorkflow`](orchstrator/src/main/java/io/dapr/springboot/examples/orchestrator/CustomerWorkflow.java) that you can find inside the `orchestrator` app, 
performs the following orchestration when a new workflow instance is created:

- Call the `RegisterCustomerActivity` activity which can be found inside the `worker-one` application.  
  - You can find in the workflow definition the configuration to make reference to an Activity that is hosted by a different Dapr application.
    ```
            customer = ctx.callActivity("io.dapr.springboot.examples.workerone.RegisterCustomerActivity", 
                                            customer,
                                            new WorkflowTaskOptions("worker-one"), 
                                            Customer.class).
                                            await();
    ```
- Wait for an external event of type `CustomerReachOut` with a timeout of 5 minutes:  
  ```
  ctx.waitForExternalEvent("CustomerReachOut", Duration.ofMinutes(5), Customer.class).await();
  ```
  You can call the following endpoint on the `orchestrator` app to raise the external event: 
  ```
   curl -X POST localhost:8080/customers/followup -H 'Content-Type: application/json' -d '{ "customerName": "salaboy" }'
  ```
- When the event is received, the workflow move forward to the last activity called `CustomerFollowUpActivity`, that can be found on the `worker-two` app.
  ```
  customer = ctx.callActivity("io.dapr.springboot.examples.workertwo.CustomerFollowupActivity",
                                customer, 
                                new WorkflowTaskOptions("worker-two"), 
                                Customer.class).
                                await();
  ```
- The workflow completes by handing out the final version of the `Customer` object that has been modified the workflow activities. You can retrieve the `Customer` payload
  by running the following command: 
  ```
  curl -X POST localhost:8080/customers/output  -H 'Content-Type: application/json' -d '{ "customerName": "salaboy" }'
  ```

## Testing Multi App Workflows

Testing becomes a complex task when you are dealing with multiple Spring Boot applications. For testing this workflow, 
we rely on [Testcontainers](https://testcontainers.com) to create the entire setup which enable us to run the workflow end to end.

You can find the end-to-end test in the [OrchestratorAppTestsIT.java]() class inside the `orchestrator` application. 
This test interact with the application REST endpoints to validate their correct execution. 

But the magic behind the test can be located in the `DaprTestContainersConfig.class` which defines the configuration for 
all the Dapr containers and the `worker-one` and `worker-two` applications.




  