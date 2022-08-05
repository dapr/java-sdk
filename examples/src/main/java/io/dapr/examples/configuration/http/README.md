
### Run Configuration Subscriber
dapr run --components-path ./components/configuration --app-id subscriber --app-port 3000 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.configuration.grpc.ConfigurationSubscriber -p 3009

### Run CongurationClient

dapr run --components-path ./components/configuration --app-id test --log-level debug --app-port 3009 --dapr-http-port 3500 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.configuration.grpc.ConfigurationClient