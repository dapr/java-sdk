## Dapr Vector API Example (alpha)

This example shows how to create a vector collection, store vectors, query them by similarity, and delete them with the Dapr vector API. The vector API is an alpha API in Dapr 1.19. Its names and behavior can change in a later release.

### Using the Vector API

The Java SDK has these methods on `DaprPreviewClient`:
* `client.createVectorCollection(...)`, `client.getVectorCollection(...)`, `client.listVectorCollections(...)` and `client.deleteVectorCollection(...)` manage collections.
* `client.upsertVectors(...)` stores records by ID, and `client.getVectors(...)` and `client.deleteVectors(...)` read and delete them.
* `client.queryVectors(...)` finds the records closest to a vector or to a stored record, optionally filtered on metadata.
* `client.batchQueryVectors(...)` runs several queries in one call. Each query succeeds or fails on its own.

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

### The vector store component

The example uses the in-memory vector store component in `./components/vector/inmemory.yaml`:

```yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: vectorstore
spec:
  type: vector.in-memory
  version: v1
```

The in-memory store keeps vectors only in the sidecar's memory. Use a different vector store component, such as `vector.meilisearch`, to keep them.

### Running the Example

`VectorExample.java` creates a `books` collection with 3 dimensions and the cosine metric, stores three books, and then queries them:

```java
try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
  client.createVectorCollection(new CreateVectorCollectionRequestAlpha1("vectorstore", "books", 3)
      .setMetric(DistanceMetric.COSINE)).block();

  List<VectorRecord> books = List.of(
      new VectorRecord("dune", new float[] {0.9f, 0.1f, 0.0f}).setMetadata(Map.of("genre", "scifi")),
      ...);
  client.upsertVectors(new UpsertVectorsRequestAlpha1("vectorstore", "books", books)
      .setOptions(IndexingOptions.waitForCompletion(Duration.ofSeconds(5), IndexingWaitTimeoutAction.FAIL_REQUEST)))
      .block();

  QueryVectorsResponseAlpha1 closest = client.queryVectors(new QueryVectorsRequestAlpha1("vectorstore", "books",
      VectorQuery.byVector(new float[] {0.85f, 0.15f, 0.05f}).setTopK(2))).block();

  QueryVectorsResponseAlpha1 fantasy = client.queryVectors(new QueryVectorsRequestAlpha1("vectorstore", "books",
      VectorQuery.byVector(new float[] {0.85f, 0.15f, 0.05f})
          .setFilter(Map.of("genre", Map.of("$eq", "fantasy"))))).block();
}
```

The upsert waits until the store has indexed the records, so the queries that follow see them. Without indexing options, a write returns as soon as the store accepts it.

The example also runs a batch of two queries, one of which asks for a record that does not exist. That query fails with `NOT_FOUND` on its own, and the other query still returns its matches.

Use the following command to run this example:

<!-- STEP
name: Run Vector Example
expected_stdout_lines:
  - "Created collection: books"
  - "Stored books: 3, COMPLETED, failed: 0"
  - "Collection books: 3 dimensions, COSINE"
  - "Closest two books: [dune, foundation]"
  - "Closest fantasy book: [hobbit]"
  - "Batch query 0: [hobbit]"
  - "Batch query 1 failed: NOT_FOUND"
  - "Read records: [dune]"
  - "Deleted record dune: COMPLETED"
  - "Deleted collection: books"
  - "SUCCESS: The vector example finished."
background: true
output_match_mode: substring
sleep: 30
-->

```bash
dapr run --resources-path ./components/vector --app-id vector-app --dapr-http-port 3500 --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.vector.VectorExample
```

<!-- END_STEP -->

### Sample Output

```
=== Dapr Vector Example ===
Created collection: books
Stored books: 3, COMPLETED, failed: 0
Collection books: 3 dimensions, COSINE
Closest two books: [dune, foundation]
Closest fantasy book: [hobbit]
Batch query 0: [hobbit]
Batch query 1 failed: NOT_FOUND
Read records: [dune]
Deleted record dune: COMPLETED
Deleted collection: books
SUCCESS: The vector example finished.
```

### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id vector-app
```

<!-- END_STEP -->
