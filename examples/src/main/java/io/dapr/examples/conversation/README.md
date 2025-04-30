## Manage Dapr via the Conversation API

This example provides the different capabilities provided by Dapr Java SDK for Conversation. For further information about Conversation APIs please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/conversation/conversation-overview/)

### Using the Conversation API

The Java SDK exposes several methods for this -
* `client.converse(...)` for conversing with an LLM through Dapr.

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
# make sure you are in the `java-sdk` directory
mvn install
```

Then get into the examples directory:

```sh
cd examples
```

### Initialize Dapr

Run `dapr init` to initialize Dapr in Self-Hosted Mode if it's not already initialized.

### Running the example

This example uses the Java SDK Dapr client in order to **Converse** with an LLM.
`DemoConversationAI.java` is the example class demonstrating these features.
Kindly check [DaprPreviewClient.java](https://github.com/dapr/java-sdk/blob/master/sdk/src/main/java/io/dapr/client/DaprPreviewClient.java) for a detailed description of the supported APIs.

```java
public class DemoConversationAI {
  /**
   * The main method to start the client.
   *
   * @param args Input arguments (unused).
   */
  public static void main(String[] args) {
    try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
      System.out.println("Sending the following input to LLM: Hello How are you? This is the my number 672-123-4567");

      ConversationInput daprConversationInput = new ConversationInput("Hello How are you? "
              + "This is the my number 672-123-4567");

      // Component name is the name provided in the metadata block of the conversation.yaml file.
      Mono<ConversationResponse> responseMono = client.converse(new ConversationRequest("echo",
              List.of(daprConversationInput))
              .setContextId("contextId")
              .setScrubPii(true).setTemperature(1.1d));
      ConversationResponse response = responseMono.block();
      System.out.printf("Conversation output: %s", response.getConversationOutpus().get(0).getResult());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
```

Use the following command to run this example-

<!-- STEP
name: Run Demo Conversation Client example
expected_stdout_lines:
  - "== APP == Conversation output: Hello How are you? This is the my number <ISBN>"
background: true
output_match_mode: substring
sleep: 10
-->

```bash
dapr run --resources-path ./components/conversation --app-id myapp --app-port 8080 --dapr-http-port 3500 --dapr-grpc-port 51439  --log-level debug -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.conversation.DemoConversationAI
```

<!-- END_STEP -->

### Sample output
```
== APP == Conversation output: Hello How are you? This is the my number <ISBN>
```
### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP

name: Cleanup
-->

```bash
dapr stop --app-id myapp
```

<!-- END_STEP -->

