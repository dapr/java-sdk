# Dapr Actors Sample

In this example, we'll use Dapr to test the actor pattern capabilities such as concurrency, state, life-cycle management for actor activation/deactivation and timers and reminders to wake-up actors.

Visit [this](https://github.com/dapr/docs/blob/master/concepts/actor/actor_overview.md) link for more information about the Actor pattern.

This example contains the follow classes:

* DemoActor: The interface for the actor. Exposes the different actor features.
* DemoActorImpl: The implementation for the DemoActor interface. Handles the logic behind the different actor features.
* DemoActorService: A Spring Boot application service that registers the actor into the Dapr actor runtime.
* DemoActorClient: This class will create and execute actors and its capabilities by using Dapr.
 
## Pre-requisites

* [Dapr and Dapr Cli](https://github.com/dapr/docs/blob/master/getting-started/environment-setup.md#environment-setup).
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

### Running the Demo actor service

The first element is to run is `DemoActorService`. Its job is registering the `DemoActor` implementation in the Dapr's Actor runtime. In `DemoActorService.java` file, you will find the `DemoActorService` class and the `main` method. See the code snippet below:

```java
@SpringBootApplication
public class DemoActorService {

  public static void main(String[] args) throws Exception {
	///...
    // Register the Actor class.
    ActorRuntime.getInstance().registerActor(
      DemoActorImpl.class, new DefaultObjectSerializer(), new DefaultObjectSerializer());

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);

    // Start application's endpoint.
    SpringApplication.run(DemoActorService.class);
  }
}
```

This application uses `ActorRuntime.getInstance().registerActor()` in order to register `DemoActorImpl` as an actor in the Dapr Actor runtime. Notice that this call passes in two serializer implementations: one is for Dapr's sent and received object and the other is for objects to be persisted.
 

`DaprApplication.start()` method will run the Spring Boot [DaprApplication](../../../springboot/DaprApplication.java), which registers the Dapr Spring Boot controller [DaprController](../../springboot/DaprController.java). This controller contains all Actor methods implemented as endpoints. The Dapr's sidecar will call into the controller. At the end of the main method, this class uses `SpringApplication.run()` to boostrap itself a an Spring application. 

Execute the follow script in order to run the DemoActorService:
```sh
cd to [repo-root]
dapr run --app-id demoactorservice --app-port 3000 --port 3005 -- mvn exec:java -pl=examples -D exec.mainClass=io.dapr.examples.actors.http.DemoActorService -D exec.args="-p 3000"
```

### Running the Actor client

The actor client is a simple java class with a main method that uses the Dapr Actor capabilities in order to create the actors and execute the different methods based on the Actor pattern.

The `DemoActorClient.java` file contains the `DemoActorClient` class. See the code snippet below:

```java
public class DemoActorClient {

  private static final int NUM_ACTORS = 3;
  private static final int NUM_MESSAGES_PER_ACTOR = 10;

  private static final ExecutorService POOL = Executors.newFixedThreadPool(NUM_ACTORS);

  public static void main(String[] args) throws Exception {
    ///...
    for (int i = 0; i < NUM_ACTORS; i++) {
      ActorProxy actor = builder.build(ActorId.createRandom());
      futures.add(callActorNTimes(actor));
    }
    ///...

  private static final CompletableFuture<Void> callActorNTimes(ActorProxy actor) {
    return CompletableFuture.runAsync(() -> {
      actor.invokeActorMethod("registerReminder").block();
      for (int i = 0; i < NUM_MESSAGES_PER_ACTOR; i++) {
        //Invoking the "incrementAndGet" method:
        actor.invokeActorMethod("incrementAndGet", 1).block();
        //Invoking "say" method
        String result = actor.invokeActorMethod("say",
                String.format("Actor %s said message #%d", actor.getActorId().toString(), i), String.class).block();
        System.out.println(String.format("Actor %s got a reply: %s", actor.getActorId().toString(), result));
        ///...
      }
      System.out.println(
              "Messages sent: " + actor.invokeActorMethod("incrementAndGet", 0, int.class).block());
    }, POOL);
  }
  }
}
```

First, The client defines how many actors it is going to create, as well as how many invocation calls it will perform per actor. Then the main method declares a `ActorProxyBuilder` for the `DemoActor` class for creating `ActorProxy` instances, which are the actor representation provided by the SDK. The code executes the `callActorNTimes` private method once per actor. This method executes functionality for the DemoActor implementation using `actor.invokeActorMethod()` in the follow order: `registerReminder()` which sets the due time and period for the reminder, `incrementAndGet()` which increments a counter, persists it and sends it back as response, and finally `say` method wich will print a message containing the received string along with the formatted server time. See [DemoActorImpl](DemoActorImpl.java) for details on the implementation of these methods. 

Use the follow command to execute the DemoActorClient:

```sh
cd to [repo-root]
dapr run --app-id demoactorclient --port 3006 -- mvn exec:java -pl=examples -D exec.mainClass=io.dapr.examples.actors.http.DemoActorClient
```

Once running, the `DemoActorClient` logs will start displaying the different steps: 
First, we can see actors being activated:
![actordemo1](../../../../../../resources/img/demo-actor-client1.png)

Then we can see the `registerReminder` in action. `DemoActorClient` console displays the actors handling reminders:
![actordemo2](../../../../../../resources/img/demo-actor-client2.png)

After invoking `incrementAndGet`, the code invokes `say` method (you'll see these messages 10 times per each of the 3 actors):
![actordemo2](../../../../../../resources/img/demo-actor-client3.png)

On the other hand, the console for `DemoActorService` is also responding to the remote invocations:
![actordemo2](../../../../../../resources/img/demo-actor-service.png)


For more details on Dapr SpringBoot integration, please refer to [Dapr Spring Boot](../../springboot/DaprApplication.java)  Application implementation.
