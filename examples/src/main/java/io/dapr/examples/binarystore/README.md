## Dapr Binary Store API Example (alpha)

This example shows how to store, read, replace, and delete files with the Dapr binary store API. The binary store API is an alpha API in Dapr 1.19. Its names and behavior can change in a later release.

### Using the Binary Store API

The Java SDK has these methods on `DaprPreviewClient`:
* `client.setBinaryFile(...)` stores a file from a byte array or from a `Flux<byte[]>` stream. Set `overwrite` to `true` to replace an existing file.
* `client.getBinaryFile(...)` reads a file as a `Flux<byte[]>` of chunks.
* `client.getBinaryFileBytes(...)` reads a whole file into a byte array.
* `client.deleteBinaryFile(...)` deletes a file.

## Pre-requisites

* [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/) and Dapr runtime 1.19 or later.
* Java JDK 17 (or greater):
    * [Microsoft JDK 17](https://docs.microsoft.com/en-us/java/openjdk/download#openjdk-17)
    * [Oracle JDK 17](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK17)
    * [OpenJDK 17](https://jdk.java.net/17/)
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

Then get into the examples directory:

```sh
cd examples
```

### Initialize Dapr

Run `dapr init` to initialize Dapr in Self-Hosted Mode if it's not already initialized.

### The binary store component

The example uses the in-memory binary store component in `./components/binarystore/inmemory.yaml`:

```yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: binarystore
spec:
  type: binarystore.in-memory
  version: v1
```

The in-memory store keeps files only in the sidecar's memory. Use a different binary store component, such as `binarystore.aws.s3` or `binarystore.azure.blobstorage`, to keep files.

### Running the Example

`BinaryStoreExample.java` stores a small file, reads it, replaces it, streams a 1 MiB file in chunks, reads it back, and deletes both files:

```java
try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
  byte[] content = "Hello from the Dapr binary store!".getBytes(StandardCharsets.UTF_8);
  client.setBinaryFile("binarystore", "hello.txt", content, false).block();

  byte[] stored = client.getBinaryFileBytes("binarystore", "hello.txt").block();

  client.setBinaryFile("binarystore", "hello.txt", "Updated content".getBytes(StandardCharsets.UTF_8), true)
      .block();

  Flux<byte[]> chunks = ...; // the file content as a stream of chunks
  client.setBinaryFile(new SetBinaryFileRequestAlpha1("binarystore", "large.bin", chunks).setOverwrite(true))
      .block();
  Flux<byte[]> received = client.getBinaryFile("binarystore", "large.bin");

  client.deleteBinaryFile("binarystore", "hello.txt").block();
}
```

The SDK sends upload content in chunks of at most 32 KiB.

Use the following command to run this example:

<!-- STEP
name: Run Binary Store Example
expected_stdout_lines:
  - "Read file content: Hello from the Dapr binary store!"
  - "Replaced file content: Updated content"
  - "Read large file: 1048576 bytes"
  - "Deleted files"
  - "SUCCESS: The binary store example finished."
background: true
output_match_mode: substring
sleep: 30
-->

```bash
dapr run --resources-path ./components/binarystore --app-id binarystore-app --dapr-http-port 3500 --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.binarystore.BinaryStoreExample
```

<!-- END_STEP -->

### Sample Output

```
=== Dapr Binary Store Example ===
Stored file: hello.txt
Read file content: Hello from the Dapr binary store!
Replaced file content: Updated content
Read large file: 1048576 bytes
Deleted files
SUCCESS: The binary store example finished.
```

### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id binarystore-app
```

<!-- END_STEP -->
