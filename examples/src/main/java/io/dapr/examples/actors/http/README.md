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
    ActorRuntime.getInstance().registerActor(DemoActorImpl.class);

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
}
```

This application uses `ActorRuntime.getInstance().registerActor()` in order to register `DemoActorImpl` as an actor in the Dapr Actor runtime. Internally, it is using `DefaultObjectSerializer` for two properties: `objectSerializer` is for Dapr's sent and received objects, and `stateSerializer` is for objects to be persisted.
 

`DaprApplication.start()` method will run the Spring Boot [DaprApplication](../../../springboot/DaprApplication.java), which registers the Dapr Spring Boot controller [DaprController](../../../springboot/DaprController.java). This controller contains all Actor methods implemented as endpoints. The Dapr's sidecar will call into the controller.

See [DemoActorImpl](DemoActorImpl.java) for details on the implementation of an actor:
```java
@ActorType(name = "DemoActor")
public class DemoActorImpl extends AbstractActor implements DemoActor, Remindable<Integer> {
  //...

  public DemoActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    super(runtimeContext, id);
    //...
  }

  @Override
  public void registerReminder() {
    //...
  }

  @Override
  public String say(String something) {
    //...
  }

  @Override
  public Mono<Integer> incrementAndGet(int delta) {
    //...
  }

  @Override
  public void clock(String message) {
    //...
  }

  @Override
  public Class<Integer> getStateType() {
    return Integer.class;
  }

  @Override
  public Mono<Void> receiveReminder(String reminderName, Integer state, Duration dueTime, Duration period) {
    //...
  }
}
```
An actor inherits from `AbstractActor` and implements the constructor to pass through `ActorRuntimeContext` and `ActorId`. By default, the actor's name will be the same as the class' name. Optionally, it can be annotated with `ActorType` and override the actor's name. The actor's methods can be synchronously or use [Project Reactor's Mono](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html) return type. Finally, state management is done via methods in `super.getActorStateManager()`.


Now, execute the following script in order to run DemoActorService:
```sh
cd to [repo-root]
dapr run --app-id demoactorservice --app-port 3000 --port 3005 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.actors.http.DemoActorService -Dexec.args="-p 3000"
```

### Debugging the Demo actor service

If you want to debug the `Subscriber`, you have to make sure to provide the port as an argument.

For VSCode you can find a sample launch.json which includes:
```json
...
{
    "type": "java",
    "name": "Debug (Launch)-DemoActorService<dapr-sdk-examples>",
    "request": "launch",
    "mainClass": "io.dapr.examples.actors.http.DemoActorService",
    "projectName": "dapr-sdk-examples",
    "args": "-port=3000"
},
...
```

Use the following command to run the Dapr sidecar:

```sh
dapr run --app-id demoactorservice --app-port 3000 --port 3005 --grpc-port 5001 -- waitfor FOREVER
```

### Running the Actor client

The actor client is a simple java class with a main method that uses the Dapr Actor capabilities in order to create the actors and execute the different methods based on the Actor pattern.

The `DemoActorClient.java` file contains the `DemoActorClient` class. See the code snippet below:

```java
public class DemoActorClient {

  private static final int NUM_ACTORS = 3;

  public static void main(String[] args) throws InterruptedException {
    ///...
    for (int i = 0; i < NUM_ACTORS; i++) {
      ActorProxy actor = builder.build(ActorId.createRandom());

      // Start a thread per actor.
      Thread thread = new Thread(() -> callActorForever(actor));
      thread.start();
      threads.add(thread);
    }
    ///...
  }

  private static final void callActorForever(ActorProxy actor) {
    // First, register reminder.
    actor.invokeActorMethod("registerReminder").block();
 
    // Now, we run until thread is interrupted.
    while (!Thread.currentThread().isInterrupted()) {
      // Invoke actor method to increment counter by 1, then build message.
      int messageNumber = actor.invokeActorMethod("incrementAndGet", 1, int.class).block();
      String message = String.format("Actor %s said message #%d", actor.getActorId().toString(), messageNumber);
   
      // Invoke the 'say' method in actor.
      String result = actor.invokeActorMethod("say", message, String.class).block();
      System.out.println(String.format("Actor %s got a reply: %s", actor.getActorId().toString(), result));
    
      try {
        // Waits for up to 1 second.
        Thread.sleep((long) (1000 * Math.random()));
      } catch (InterruptedException e) {
        // We have been interrupted, so we set the interrupted flag to exit gracefully.
        Thread.currentThread().interrupt();
      }
    }
  }
}
```

First, the client defines how many actors it is going to create. Then the main method declares a `ActorProxyBuilder` for the `DemoActor` class to create `ActorProxy` instances, which are the actor representation provided by the SDK. The code executes the `callActorForever` private method once per actor. This method triggers the DemoActor's implementation by using `actor.invokeActorMethod()`. Initially, it will invoke `registerReminder()`, which sets the due time and period for the reminder. Then, `incrementAndGet()` increments a counter, persists it and sends it back as response. Finally `say` method which will print a message containing the received string along with the formatted server time. 

Use the follow command to execute the DemoActorClient:

```sh
cd to [repo-root]
dapr run --app-id demoactorclient --port 3006 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.actors.http.DemoActorClient
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
