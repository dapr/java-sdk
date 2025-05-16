# Dapr Spring Boot Workflow Examples

This application allows you to run different workflow patterns including: 
- Chained Activities
- Parent/Child Workflows
- Continue workflow by sending External Events
- Fan Out/In activities for parallel execution

## Running these examples from source code

To run these examples you will need:
- Java SDK
- Maven
- Docker or a container runtime such as Podman

From the `spring-boot-examples/workflows` directory you can start the service using the test configuration that uses
[Testcontainers](https://testcontainers.com) to boostrap [Dapr](https://dapr.io) by running the following command:

<!-- STEP
name: Run Demo Workflow Application
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'Started WorkflowPatternsApplication'
background: true
expected_return_code: 143
sleep: 30
timeout_seconds: 45
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

```sh
cd workflows/
../../mvnw spring-boot:test-run
```

<!-- END_STEP -->

Once the application is running you can trigger the different patterns by sending the following requests: 

### Chaining Activities Workflow example

The `io.dapr.springboot.examples.wfp.chain.ChainWorkflow` executes three chained activities. For this example the 
`ToUpperCaseActivity.java` is used to transform to upper case three strings from an array.

```mermaid
graph LR
   SW((Start
   Workflow))
   A1[Activity1]
   A2[Activity2]
   A3[Activity3]
   EW((End
   Workflow))
   SW --> A1
   A1 --> A2
   A2 --> A3
   A3 --> EW
```

<!-- STEP
name: Start Chain Activities Workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
- 'TOKYO, LONDON, SEATTLE'
background: true
sleep: 1
timeout_seconds: 2
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

To start the workflow with the three chained activities you can run: 

```sh
curl -X POST localhost:8080/wfp/chain -H 'Content-Type: application/json' 
```

<!-- END_STEP -->


As result from executing the request you should see: 

```bash
TOKYO, LONDON, SEATTLE
```

In the application output you should see the workflow activities being executed. 

```bash
2025-05-16T09:59:22.176+01:00  INFO 8360 --- [pool-3-thread-1] io.dapr.workflows.WorkflowContext        : Starting Workflow: io.dapr.springboot.examples.wfp.chain.ChainWorkflow
2025-05-16T09:59:22.194+01:00  INFO 8360 --- [nio-8080-exec-2] i.d.s.e.w.WorkflowPatternsRestController : Workflow instance 7625b4af-8c04-408a-93dc-bad753466e43 started
2025-05-16T09:59:22.195+01:00  INFO 8360 --- [pool-3-thread-2] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Starting Activity: io.dapr.springboot.examples.wfp.chain.ToUpperCaseActivity
2025-05-16T09:59:22.196+01:00  INFO 8360 --- [pool-3-thread-2] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Message Received from input: Tokyo
2025-05-16T09:59:22.197+01:00  INFO 8360 --- [pool-3-thread-2] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Sending message to output: TOKYO
2025-05-16T09:59:22.210+01:00  INFO 8360 --- [pool-3-thread-1] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Starting Activity: io.dapr.springboot.examples.wfp.chain.ToUpperCaseActivity
2025-05-16T09:59:22.210+01:00  INFO 8360 --- [pool-3-thread-1] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Message Received from input: London
2025-05-16T09:59:22.210+01:00  INFO 8360 --- [pool-3-thread-1] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Sending message to output: LONDON
2025-05-16T09:59:22.216+01:00  INFO 8360 --- [pool-3-thread-3] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Starting Activity: io.dapr.springboot.examples.wfp.chain.ToUpperCaseActivity
2025-05-16T09:59:22.216+01:00  INFO 8360 --- [pool-3-thread-3] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Message Received from input: Seattle
2025-05-16T09:59:22.216+01:00  INFO 8360 --- [pool-3-thread-3] i.d.s.e.wfp.chain.ToUpperCaseActivity    : Sending message to output: SEATTLE
2025-05-16T09:59:22.219+01:00  INFO 8360 --- [pool-3-thread-1] io.dapr.workflows.WorkflowContext        : Workflow finished with result: TOKYO, LONDON, SEATTLE
```

### Parent / Child Workflows example

In this example we start a Parent workflow that calls a child workflow that execute one activity that reverse the . 

The Parent workflow looks like this: 

```mermaid
graph LR
   SW((Start
   Workflow))
   subgraph for each word in the input
    GWL[Call child workflow]
   end
   EW((End
   Workflow))
   SW --> GWL
   GWL --> EW
```

The Child workflow looks like this:

```mermaid
graph LR
   SW((Start
   Workflow))
   A1[Activity1]
   EW((End
   Workflow))
   SW --> A1
   A1 --> EW
```

To start the parent workflow you can run:


<!-- STEP
name: Start Parent/Child Workflow
match_order: none
output_match_mode: substring
expected_stdout_lines:
- '!wolfkroW rpaD olleH'
background: true
sleep: 1
timeout_seconds: 2
-->
<!-- Timeout for above service must be more than sleep + timeout for the client-->

To start the workflow with the three chained activities you can run:

```sh
curl -X POST localhost:8080/wfp/child -H 'Content-Type: application/json' 
```

<!-- END_STEP -->


As result from executing the request you should see:

```bash
!wolfkroW rpaD olleH
```

In the application output you should see the workflow activities being executed.

```bash
2025-05-16T10:27:01.845+01:00  INFO 9855 --- [pool-3-thread-1] io.dapr.workflows.WorkflowContext        : Starting Workflow: io.dapr.springboot.examples.wfp.child.ParentWorkflow
2025-05-16T10:27:01.845+01:00  INFO 9855 --- [pool-3-thread-1] io.dapr.workflows.WorkflowContext        : calling childworkflow with input: Hello Dapr Workflow!
2025-05-16T10:27:01.865+01:00  INFO 9855 --- [nio-8080-exec-1] i.d.s.e.w.WorkflowPatternsRestController : Workflow instance f3ec9566-a0fc-4d28-8912-3f3ded3cd8a9 started
2025-05-16T10:27:01.866+01:00  INFO 9855 --- [pool-3-thread-2] io.dapr.workflows.WorkflowContext        : Starting ChildWorkflow: io.dapr.springboot.examples.wfp.child.ChildWorkflow
2025-05-16T10:27:01.868+01:00  INFO 9855 --- [pool-3-thread-2] io.dapr.workflows.WorkflowContext        : ChildWorkflow received input: Hello Dapr Workflow!
2025-05-16T10:27:01.868+01:00  INFO 9855 --- [pool-3-thread-2] io.dapr.workflows.WorkflowContext        : ChildWorkflow is calling Activity: io.dapr.springboot.examples.wfp.child.ReverseActivity
2025-05-16T10:27:01.874+01:00  INFO 9855 --- [pool-3-thread-3] i.d.s.e.wfp.child.ReverseActivity        : Starting Activity: io.dapr.springboot.examples.wfp.child.ReverseActivity
2025-05-16T10:27:01.874+01:00  INFO 9855 --- [pool-3-thread-3] i.d.s.e.wfp.child.ReverseActivity        : Message Received from input: Hello Dapr Workflow!
2025-05-16T10:27:01.874+01:00  INFO 9855 --- [pool-3-thread-3] i.d.s.e.wfp.child.ReverseActivity        : Sending message to output: !wolfkroW rpaD olleH
2025-05-16T10:27:01.882+01:00  INFO 9855 --- [pool-3-thread-1] io.dapr.workflows.WorkflowContext        : ChildWorkflow finished with: !wolfkroW rpaD olleH
2025-05-16T10:27:01.892+01:00  INFO 9855 --- [pool-3-thread-2] io.dapr.workflows.WorkflowContext        : childworkflow finished with: !wolfkroW rpaD olleH
```

### ContinueAsNew Workflows example


### External Event Workflow example


### Fan Out/In Workflow example


## Testing workflow executions

Workflow execution can be tested using Testcontainers and you can find all the tests for the patterns covered in this 
application [here](test/java/io/dapr/springboot/examples/wfp/TestWorkflowPatternsApplication.java).