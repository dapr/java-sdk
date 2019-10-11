## Java client for dapr.

### Installing
```sh
# make sure you are in the `java-sdk` directory.
mvn package install
```

### Running an example
```sh
cd examples/
mvn compile
dapr run --protocol grpc --grpc-port 50001 -- mvn exec:java -Dexec.mainClass=io.dapr.examples.Example
```
