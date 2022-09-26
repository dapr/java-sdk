```bash
dapr run --components-path ./components/pubsub --app-id pubsub-test --log-level debug -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.CloudEventBulkPublisher
```
java -Ddapr.grpc.port="50010" -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.CloudEventBulkPublisher
java -Ddapr.grpc.port="50010" -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.BulkPublisher
java -Ddapr.grpc.port="50010" -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.BinaryBulkPublisher

java -Ddapr.api.protocol="http" -Ddapr.http.port="3500" -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.BulkPublisher
java -Ddapr.api.protocol="http" -Ddapr.http.port="3500" -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.CloudEventBulkPublisher
java -Ddapr.api.protocol="http" -Ddapr.http.port="3500" -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.BinaryBulkPublisher
