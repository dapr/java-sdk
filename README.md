## Dapr SDK for Java

This is the Dapr SDK for Java, based on the auto-generated proto client.<br>

For more info on Dapr and gRPC, visit [this link](https://github.com/dapr/docs/tree/master/howto/create-grpc-app).


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
