## Unit testing sample

This sample illustrates how applications can write unit tests with Dapr's Java SDK, JUnit 5 and Mockito.

## Pre-requisites

* [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/).
* Java JDK 11 (or greater):
    * [Microsoft JDK 11](https://docs.microsoft.com/en-us/java/openjdk/download#openjdk-11)
    * [Oracle JDK 11](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11)
    * [OpenJDK 11](https://jdk.java.net/11/)
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

Then change into the `examples` directory:
```sh
cd examples
```

### Initialize Dapr

Run `dapr init` to initialize Dapr in Self-Hosted Mode if it's not already initialized.

### Understanding the code

#### Example App Test
This example, found in `DaprExampleTest.java`, will simulate an application code via the App class:

```java
  private static final class MyApp {

    private final DaprClient daprClient;

    private final Function<ActorId, MyActor> actorProxyFactory;

    /**
     * Example of constructor that can be used for production code.
     * @param client Dapr client.
     * @param actorClient Dapr Actor client.
     */
    public MyApp(DaprClient client, ActorClient actorClient) {
      this.daprClient = client;
      this.actorProxyFactory = (actorId) -> new ActorProxyBuilder<>(MyActor.class, actorClient).build(actorId);
    }

    /**
     * Example of constructor that can be used for test code.
     * @param client Dapr client.
     * @param actorProxyFactory Factory method to create actor proxy instances.
     */
    public MyApp(DaprClient client, Function<ActorId, MyActor> actorProxyFactory) {
      this.daprClient = client;
      this.actorProxyFactory = actorProxyFactory;
    }

    public String getState() {
      return daprClient.getState("appid", "statekey", String.class).block().getValue();
    }

    public String invokeActor() {
      MyActor proxy = actorProxyFactory.apply(new ActorId("myactorId"));
      return proxy.hello();
    }
  }
```

This class has two constructors. The first one can be used by production code by passing the proper instances of `DaprClient` and `ActorClient`.
The second, contains two methods: `getState()` will retrieve a state from the `DaprClient`, while `invokeActor()` will create an instance of the Actor proxy for `MyActor` interface and invoke a method on it.

```java
  @ActorType(name = "MyActor")
  public interface MyActor {
    String hello();
  }
```

The first test validates the `getState()` method while mocking `DaprClient`:
```java
  @Test
  public void testGetState() {
    DaprClient daprClient = Mockito.mock(DaprClient.class);
    Mockito.when(daprClient.getState("appid", "statekey", String.class)).thenReturn(
        Mono.just(new State<>("statekey", "myvalue", "1")));

    MyApp app = new MyApp(daprClient, (ActorClient) null);

    String value = app.getState();

    assertEquals("myvalue", value);
  }
```

The second test uses a mock implementation of the factory method and checks the actor invocation by mocking the `MyActor` interface:
```java
  @Test
  public void testInvokeActor() {
    MyActor actorMock = Mockito.mock(MyActor.class);
    Mockito.when(actorMock.hello()).thenReturn("hello world");

    MyApp app = new MyApp(null, actorId -> actorMock);

    String value = app.invokeActor();

    assertEquals("hello world", value);
  }
```


#### Running the example
<!-- STEP
name: Check state example
expected_stdout_lines:
  - "[         2 tests found           ]"
  - "[         0 tests skipped         ]"
  - "[         2 tests started         ]"
  - "[         0 tests aborted         ]"
  - "[         2 tests successful      ]"
  - "[         0 tests failed          ]"
background: true
sleep: 5
-->

Run this example with the following command:
```bash
java -jar target/dapr-java-sdk-examples-exec.jar org.junit.platform.console.ConsoleLauncher --select-class=io.dapr.examples.unittesting.DaprExampleTest
```

<!-- END_STEP -->

After running, Junit should print the output as follows:

```txt
╷
├─ JUnit Jupiter ✔
│  └─ DaprExampleTest ✔
│     ├─ testGetState() ✔
│     └─ testInvokeActor() ✔
└─ JUnit Vintage ✔

Test run finished after 1210 ms
[         3 containers found      ]
[         0 containers skipped    ]
[         3 containers started    ]
[         0 containers aborted    ]
[         3 containers successful ]
[         0 containers failed     ]
[         2 tests found           ]
[         0 tests skipped         ]
[         2 tests started         ]
[         0 tests aborted         ]
[         2 tests successful      ]
[         0 tests failed          ]
```

#### Example Workflow Test
This example, found in `DaprWorkflowExampleTest.java`, shows how to mock and test a dapr workflow:

```java
private class DemoWorkflow extends Workflow {

  @Override
  public void run(WorkflowContext ctx) {
    String name = ctx.getName();
    String id = ctx.getInstanceId();
    try {
      ctx.waitForExternalEvent("myEvent", Duration.ofSeconds(10)).await();
    } catch (TaskCanceledException e) {
      ctx.getLogger().warn("Timed out");
    }
    String output = name + ":" + id;
    ctx.complete(output);
  }
}
```

The example provides its own workflow, but for a production system you would want to import and use your own workflow. The goal of unit testing a workflow is to ensure that the business logic functions as expected.  For our example, that involves the following two sections:

```java
String output = name + ":" + id;
```

```java
} catch (TaskCanceledException e) {
    ctx.getLogger().warn("Timed out");
}
```


The first test validates the output of our workflow by mocking the `WorkflowContext` and verifying the `.complete` method:
```java
  @Test
  public void testWorkflow() {
    WorkflowContext mockContext = Mockito.mock(WorkflowContext.class);
    String name = "DemoWorkflow";
    String id = "my-workflow-123";
  
    Mockito.when(mockContext.getName()).thenReturn(name);
    Mockito.when(mockContext.getInstanceId()).thenReturn(id);
  
    new DemoWorkflow().run(mockContext);
  
    String expectedOutput = name + ":" + id;
    Mockito.verify(mockContext, times(1)).complete(expectedOutput);
  }
```

The second test validates the `catch` block of our workflow by throwing the expected exception from our mock and verifying the expected call on our mock `Logger`:
```java
  @Test
  public void testWorkflowWaitForEventTimeout() {
    WorkflowContext mockContext = Mockito.mock(WorkflowContext.class);
    Logger mockLogger = Mockito.mock(Logger.class);
  
    Mockito.when(mockContext.getLogger()).thenReturn(mockLogger);
    Mockito.when(mockContext.waitForExternalEvent(anyString(),any(Duration.class)))
    .thenThrow(TaskCanceledException.class);
  
    new DemoWorkflow().run(mockContext);
  
    Mockito.verify(mockLogger, times(1)).warn("Timed out");
}
```

The third test is similar but validates the inverse of the test above, ensuring that a code path we do not expect to be triggered is indeed avoided:
```java
  @Test
  public void testWorkflowWaitForEventNoTimeout() {
    WorkflowContext mockContext = Mockito.mock(WorkflowContext.class);
    Logger mockLogger = Mockito.mock(Logger.class);

    Mockito.when(mockContext.getLogger()).thenReturn(mockLogger);

    new DemoWorkflow().run(mockContext);

    Mockito.verify(mockLogger, times(0)).warn(anyString());
}
```


#### Running the example
<!-- STEP
name: Check state example
expected_stdout_lines:
  - "[         3 tests found           ]"
  - "[         0 tests skipped         ]"
  - "[         3 tests started         ]"
  - "[         0 tests aborted         ]"
  - "[         3 tests successful      ]"
  - "[         0 tests failed          ]"
background: true
sleep: 5
-->

Run this example with the following command:
```bash
java -jar target/dapr-java-sdk-examples-exec.jar org.junit.platform.console.ConsoleLauncher --select-class=io.dapr.examples.unittesting.DaprWorkflowExampleTest
```

<!-- END_STEP -->

After running, Junit should print the output as follows:

```txt
╷
├─ JUnit Jupiter ✔
│  └─ DaprWorkflowExampleTest ✔
│     ├─ testWorkflowWaitForEventTimeout() ✔
│     ├─ testWorkflowWaitForEventNoTimeout() ✔
│     └─ testWorkflow() ✔
└─ JUnit Vintage ✔

Test run finished after 815 ms
[         3 containers found      ]
[         0 containers skipped    ]
[         3 containers started    ]
[         0 containers aborted    ]
[         3 containers successful ]
[         0 containers failed     ]
[         3 tests found           ]
[         0 tests skipped         ]
[         3 tests started         ]
[         0 tests aborted         ]
[         3 tests successful      ]
[         0 tests failed          ]

```