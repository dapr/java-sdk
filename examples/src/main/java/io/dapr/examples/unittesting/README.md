## Unit testing sample

This sample illustrates how applications can write unit testing with Dapr's Java SDK, JUnit 5 and Mockito.

## Pre-requisites

* [Dapr and Dapr Cli](https://docs.dapr.io/getting-started/install-dapr/).
* Java JDK 11 (or greater): [Oracle JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11) or [OpenJDK](https://jdk.java.net/13/).
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

### Understanding the code
This example will simulate an application code via the App class:

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
Then, it contains two methods: `getState()` will retrieve a state from `DaprClient`, while `invokeActor()` will create an instance of Actor proxy for `MyActor` interface and invoke a method on it.

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


### Running the example
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