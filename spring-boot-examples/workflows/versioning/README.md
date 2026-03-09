# Versioning Workflows example

This example shows how to create different versions for a workflow. 
Dapr supports two types of versioning: Full workflow version and Patch workflow version.

- Full version workflow refers to the case when the whole workflow definition is replaced by a new one. 
- Patch version workflow refers to the case when you want to introduce a small change in the current workflow definition.

## Full workflow version

This example is composed by three Spring Boot applications:
- `version-orchestrator`: The `version-orchestrator` app exposes REST endpoints to create, continue and get the status of the workflow.
- `full-version-worker-1`: The `full-version-worker-1` app contains version 1 of the workflow and is triggered by the `version-orchestrator` app. 
This app is only able tu run version 1 of the workflow.
The definition of the workflow is:
    `Activity1` → wait for event `followup` → `Activity2`

  You can find the implementation in the [`RegisterV1Components.java`](full-version-worker-one/src/main/java/io/dapr/springboot/examples/workerone/RegisterV1Components.java) class.
- `full-version-worker-2`: The `full-version-worker-2` app contains version 1 and version 2 of the workflow and is triggered by the `version-orchestrator` app.
This app is able to run version 1 and version 2 of the workflow depending on the version that the workflow has started.
  The definition of the workflow is:
  `Activity3` → wait for event `followup` → `Activity4`

  You can find the implementation in the [`RegisterV2Components.java`](full-version-worker-two/src/main/java/io/dapr/springboot/examples/workertwo/RegisterV2Components.java) class.

### Start workflow version v1
To start the applications you need to run the following commands on separate terminals, starting from the `versioning` directory.
To start the `version-orchestrator` app run:
```bash
cd version-orchestrator/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

The `version-orchestrator` application will run on port `8080`.

On a separate terminal, to start the `full-version-worker-1` app run:
```bash
cd full-version-worker-one/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

You can create new workflow instances the endpoint.

```bash
curl -X POST localhost:8083/version/v1/full
```

This request will return something similar to:

`New Workflow Instance created with instanceId: 11111111-1111-1111-1111-111111111111`

With this instanceId you can raise an event with the following command:

```bash
curl -X POST localhost:8080/version/v1/full/followup/11111111-1111-1111-1111-111111111111
```

Once the event is raised, the workflow will continue and the workflow app will be completed. You can check it with:

```bash
curl -X POST localhost:8080/version/v1/full/status/11111111-1111-1111-1111-111111111111
```

The result will be:
` Workflow for instanceId: 11111111-1111-1111-1111-111111111111 is completed[COMPLETED] with Activity1, Activity2`

### Start workflow version v2
To start the applications you need to run the following commands on separate terminals, starting from the `versioning` directory.
To start the `version-orchestrator` app run:
```bash
cd version-orchestrator/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

The `version-orchestrator` application will run on port `8080`.

On a separate terminal, to start the `full-version-worker-2` app run:
```bash
cd full-version-worker-two/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

You can create new workflow instances the endpoint.

```bash
curl -X POST localhost:8083/version/v2/full
```

This request will return something similar to:

`New Workflow Instance created with instanceId: 22222222-2222-2222-2222-222222222222`

With this instanceId you can raise an event with the following command:

```bash
curl -X POST localhost:8083/version/v2/full/followup/22222222-2222-2222-2222-222222222222
```

Once the event is raised, the workflow will continue and the workflow app will be completed. You can check it with:

```bash
curl -X POST localhost:8083/version/v2/full/status/22222222-2222-2222-2222-222222222222
```

The result will be:
` Workflow for instanceId: 22222222-2222-2222-2222-222222222222 is completed[COMPLETED] with Activity3, Activity4`

### Mix version 1 and version 2

To start the applications you need to run the following commands on separate terminals, starting from the `versioning` directory.
To start the `version-orchestrator` app run:
```bash
cd version-orchestrator/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

The `version-orchestrator` application will run on port `8083`.

On a separate terminal, to start the `full-version-worker-1` app run:
```bash
cd full-version-worker-one/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

You can create new workflow instances the endpoint.

```bash
curl -X POST localhost:8083/version/v1/full
```

This request will return something similar to:

`New Workflow Instance created with instanceId: 11111111-1111-1111-1111-111111111111`

Now you can stop the `full-version-worker-1` app and start the `full-version-worker-2` app.
```bash
cd full-version-worker-two/
mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

Now you can create new workflow instances with:
You can create new workflow instances the endpoint.

```bash
curl -X POST localhost:8083/version/v2/full
```
This will return something similar to:

`New Workflow Instance created with instanceId: 22222222-2222-2222-2222-222222222222`

This means you have two workflow instances running at the same time,
one created with version 1 and the other with version 2.

If you complete both instances:

```bash
curl -X POST localhost:8083/version/v2/full/followup/11111111-1111-1111-1111-111111111111
curl -X POST localhost:8083/version/v2/full/followup/22222222-2222-2222-2222-222222222222
```

And get the status of the workflow instances:

```bash
curl -X POST localhost:8083/version/v2/full/status/11111111-1111-1111-1111-111111111111
curl -X POST localhost:8083/version/v2/full/status/22222222-2222-2222-2222-222222222222
```

The result will be:
` Workflow for instanceId: 11111111-1111-1111-1111-111111111111 is completed[COMPLETED] with Activity1, Activity2`
` Workflow for instanceId: 22222222-2222-2222-2222-222222222222 is completed[COMPLETED] with Activity3, Activity4`

## Patch workflow version

This example is composed by three Spring Boot applications:
- `version-orchestrator`: The `version-orchestrator` app exposes REST endpoints to create, continue and get the status of the workflow.
- `patch-version-worker-1`: The `patch-version-worker-1` app contains version 1 of the workflow and is triggered by the `version-orchestrator` app.
  This app runs the initial workflow definition.
  The definition of the workflow is:
  `Activity1` → wait for event `followup` → `Activity2`

  You can find the implementation in the [`RegisterComponents.java`](patch-version-worker-one/src/main/java/io/dapr/springboot/examples/workerone/RegisterComponents.java) class.
- `patch-version-worker-2`: The `patch-version-worker-2` app contains the patched version and is triggered by the `version-orchestrator` app.
  This app is able to run both initial and patched versions of the workflow depending on the version that the workflow has started.
  The definition of the workflow is :
  `Activity1` → wait for event `followup` → `Activity3`  → `Activity4` → `Activity2`

  You can find the implementation in the [`RegisterPatchedComponents.java`](patch-version-worker-two/src/main/java/io/dapr/springboot/examples/workertwo/RegisterPatchedComponents.java) class.

### Start workflow with initial definition
To start the applications you need to run the following commands on separate terminals, starting from the `versioning` directory.

In two terminals run:
```bash
cd version-orchestrator/ && mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```
and
```bash
cd patch-version-worker-one/ && mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

then you create, trigger the event and check the status:

```bash
curl -X POST localhost:8083/version/v1/patch
curl -X POST localhost:8083/version/v1/patch/followup/11111111-1111-1111-1111-111111111111
curl -X POST localhost:8083/version/v1/patch/status/11111111-1111-1111-1111-111111111111
```

This will return:
`Workflow for instanceId: 11111111-1111-1111-1111-111111111111 is completed[COMPLETED] with Activity1, Activity2`

Now start a new workflow but do not complete it:

```bash
curl -X POST localhost:8083/version/v1/patch
```
Save the instanceId and complete the workflow in the next step. Lets assume the instanceId is `33333333-3333-3333-3333-333333333333`.

### Start workflow with patched definition

Stop the `patch-version-worker-1` app and start the `patch-version-worker-2` app.

```bash
cd patch-version-worker-two/ && mvn -Dspring-boot.run.arguments="--reuse=true" clean spring-boot:test-run
```

Now you can create new workflow instances with:
```bash
curl -X POST localhost:8083/version/v2/patch
curl -X POST localhost:8083/version/v2/patch/followup/22222222-2222-2222-2222-222222222222
curl -X POST localhost:8083/version/v2/patch/status/22222222-2222-2222-2222-222222222222
```

This will return:
`Workflow for instanceId: 22222222-2222-2222-2222-222222222222 is completed[COMPLETED] with Activity1, Activity3, Activity4, Activity2`

And if we continue the instance with id `33333333-3333-3333-3333-333333333333` we will get:
`Workflow for instanceId: 33333333-3333-3333-3333-333333333333 is completed[COMPLETED] with Activity1, Activity3, Activity4, Activity2`'