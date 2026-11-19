## Dapr Search API Example (alpha)

This example shows how to create a search index, store documents, search them, and delete them with the Dapr search API. The search API is an alpha API in Dapr 1.19. Its names and behavior can change in a later release.

### Using the Search API

The Java SDK has these methods on `DaprPreviewClient`:
* `client.createSearchIndex(...)`, `client.getSearchIndex(...)`, `client.listSearchIndexes(...)` and `client.deleteSearchIndex(...)` manage indexes.
* `client.indexSearchDocuments(...)` stores documents by ID, and `client.getSearchDocuments(...)` and `client.deleteSearchDocuments(...)` read and delete them.
* `client.search(...)` finds documents by full-text query, by a query in the store's own format, or by filter alone, and returns the hits one page at a time.

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

### The search store component

The example uses the in-memory search store component in `./components/search/inmemory.yaml`:

```yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: searchstore
spec:
  type: search.in-memory
  version: v1
```

The in-memory store keeps documents only in the sidecar's memory. Use a different search store component, such as `search.meilisearch`, to keep them.

### Running the Example

`SearchExample.java` creates a `books` index, stores three books, and then searches them. A document's content is a UTF-8 encoded JSON object:

```java
try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
  client.createSearchIndex("searchstore", "books").block();

  List<SearchDocument> books = List.of(
      new SearchDocument("dune",
          "{\"title\":\"Dune\",\"genre\":\"scifi\",\"year\":1965}".getBytes(StandardCharsets.UTF_8)),
      ...);
  client.indexSearchDocuments(new IndexSearchDocumentsRequestAlpha1("searchstore", "books", books)
      .setOptions(IndexingOptions.waitForCompletion(Duration.ofSeconds(5), IndexingWaitTimeoutAction.FAIL_REQUEST)))
      .block();

  SearchResponseAlpha1 text = client.search(
      new SearchRequestAlpha1("searchstore", "books", SearchQuery.byText("hobbit"))).block();

  SearchResponseAlpha1 scifi = client.search(new SearchRequestAlpha1("searchstore", "books",
      SearchQuery.matchAll()
          .setFilter(Map.of("genre", Map.of("$eq", "scifi")))
          .setSort(List.of(SortClause.asc("year"))))).block();
}
```

The write waits until the store has indexed the documents, so the searches that follow see them. Without indexing options, a write returns as soon as the store accepts it.

The example also reads every book in pages of two: each response has a continuation token until the last page, and the next request sends the same query with that token.

Use the following command to run this example:

<!-- STEP
name: Run Search Example
expected_stdout_lines:
  - "Created index: books"
  - "Stored books: 3, COMPLETED, failed: 0"
  - "Index books: 3 documents"
  - "Full-text search: [hobbit]"
  - "Science fiction, oldest first: [foundation, dune]"
  - "Page 1 of 3 books: [dune, foundation]"
  - "Page 2 of 3 books: [hobbit]"
  - "Read dune: {\"title\":\"Dune\",\"genre\":\"scifi\",\"year\":1965}"
  - "Deleted document dune: COMPLETED"
  - "Deleted index: books"
  - "SUCCESS: The search example finished."
background: true
output_match_mode: substring
sleep: 30
-->

```bash
dapr run --resources-path ./components/search --app-id search-app --dapr-http-port 3500 --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.search.SearchExample
```

<!-- END_STEP -->

### Sample Output

```
=== Dapr Search Example ===
Created index: books
Stored books: 3, COMPLETED, failed: 0
Index books: 3 documents
Full-text search: [hobbit]
Science fiction, oldest first: [foundation, dune]
Page 1 of 3 books: [dune, foundation]
Page 2 of 3 books: [hobbit]
Read dune: {"title":"Dune","genre":"scifi","year":1965}
Deleted document dune: COMPLETED
Deleted index: books
SUCCESS: The search example finished.
```

### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id search-app
```

<!-- END_STEP -->
