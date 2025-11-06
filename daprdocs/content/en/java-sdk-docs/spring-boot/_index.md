---
type: docs
title: "Getting started with the Dapr and Spring Boot"
linkTitle: "Spring Boot Integration"
weight: 4000
description: How to get started with Dapr and Spring Boot  
---

By combining Dapr and Spring Boot, we can create infrastructure independent Java applications that can be deployed across different environments, supporting a wide range of on-premises and cloud provider services. 

First, we will start with a simple integration covering the `DaprClient` and the [Testcontainers](https://testcontainers.com/) integration, to then use Spring and Spring Boot mechanisms and programming model to leverage the Dapr APIs under the hood. This helps teams to remove dependencies such as clients and drivers required to connect to environment-specific infrastructure (databases, key-value stores, message brokers, configuration/secret stores, etc) 

{{% alert title="Note" color="primary" %}}
The Spring Boot integration requires Spring Boot 3.x+ to work. This will not work with Spring Boot 2.x.
The Spring Boot integration remains in alpha. We need your help and feedback to graduate it. 
Please join the [#java-sdk discord channel](https://discord.com/channels/778680217417809931/778749797242765342) discussion or open issues in the [dapr/java-sdk](https://github.com/dapr/java-sdk/issues).

{{% /alert %}}


## Adding the Dapr and Spring Boot integration to your project

If you already have a Spring Boot application, you can directly add the following dependencies to your project: 

```
	<dependency>
        <groupId>io.dapr.spring</groupId>
		<artifactId>dapr-spring-boot-starter</artifactId>
		<version>0.16.0</version>
	</dependency>
	<dependency>
		<groupId>io.dapr.spring</groupId>
		<artifactId>dapr-spring-boot-starter-test</artifactId>
		<version>0.16.0</version>
		<scope>test</scope>
	</dependency>
```

You can find the [latest released version here](https://central.sonatype.com/artifact/io.dapr.spring/dapr-spring-boot-starter). 

By adding these dependencies, you can: 
- Autowire a `DaprClient` to use inside your applications
- Use the Spring Data and Messaging abstractions and programming model that uses the Dapr APIs under the hood
- Improve your inner-development loop by relying on [Testcontainers](https://testcontainers.com/) to bootstrap Dapr Control plane services and default components

Once these dependencies are in your application, you can rely on Spring Boot autoconfiguration to autowire a `DaprClient` instance:

```java
@Autowired
private DaprClient daprClient;

```

This will connect to the default Dapr gRPC endpoint `localhost:50001`, requiring you to start Dapr outside of your application. 

{{% alert title="Note" color="primary" %}}
By default, the following properties are preconfigured for `DaprClient` and `DaprWorkflowClient`:
```properties
dapr.client.httpEndpoint=http://localhost
dapr.client.httpPort=3500
dapr.client.grpcEndpoint=localhost
dapr.client.grpcPort=50001
dapr.client.apiToken=<your remote api token>
```
These values are used by default, but you can override them in your `application.properties` file to suit your environment. Please note that both kebab case and camel case are supported.
{{% /alert %}}

You can use the `DaprClient` to interact with the Dapr APIs anywhere in your application, for example from inside a REST endpoint: 

```java 
@RestController
public class DemoRestController {
  @Autowired
  private DaprClient daprClient;

  @PostMapping("/store")
  public void storeOrder(@RequestBody Order order){
    daprClient.saveState("kvstore", order.orderId(), order).block();
  }
}

record Order(String orderId, Integer amount){}
```

If you want to avoid managing Dapr outside of your Spring Boot application, you can rely on [Testcontainers](https://testcontainers.com/) to bootstrap Dapr beside your application for development purposes. 
To do this we can create a test configuration that uses `Testcontainers` to bootstrap all we need to develop our applications using the Dapr APIs. 

Using [Testcontainers](https://testcontainers.com/) and Dapr integrations, we let the `@TestConfiguration` bootstrap Dapr for our applications. 
Notice that for this example, we are configuring Dapr with a Statestore component called `kvstore` that connects to an instance of `PostgreSQL` also bootstrapped by Testcontainers.

```java
@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {
  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network daprNetwork, PostgreSQLContainer<?> postgreSQLContainer){
    
    return new DaprContainer("daprio/daprd:1.16.0-rc.5")
            .withAppName("producer-app")
            .withNetwork(daprNetwork)
            .withComponent(new Component("kvstore", "state.postgresql", "v1", STATE_STORE_PROPERTIES))
            .withComponent(new Component("kvbinding", "bindings.postgresql", "v1", BINDING_PROPERTIES))
            .dependsOn(postgreSQLContainer);
  }
}
```

Inside the test classpath you can add a new Spring Boot Application that uses this configuration for tests: 

```java
@SpringBootApplication
public class TestProducerApplication {

  public static void main(String[] args) {

    SpringApplication
            .from(ProducerApplication::main)
            .with(DaprTestContainersConfig.class)
            .run(args);
  }
  
}
```

Now you can start your application with: 
```bash
mvn spring-boot:test-run
```

Running this command will start the application, using the provided test configuration that includes the Testcontainers and Dapr integration. In the logs you should be able to see that the `daprd` and the `placement` service containers were started for your application. 

Besides the previous configuration (`DaprTestContainersConfig`) your tests shouldn't be testing Dapr itself, just the REST endpoints that your application is exposing. 


## Leveraging Spring & Spring Boot programming model with Dapr

The Java SDK allows you to interface with all of the [Dapr building blocks]({{% ref building-blocks %}}). 
But if you want to leverage the Spring and Spring Boot programming model you can use the `dapr-spring-boot-starter` integration. 
This includes implementations of Spring Data (`KeyValueTemplate` and `CrudRepository`) as well as a `DaprMessagingTemplate` for producing and consuming messages 
(similar to [Spring Kafka](https://spring.io/projects/spring-kafka), [Spring Pulsar](https://spring.io/projects/spring-pulsar) and [Spring AMQP for RabbitMQ](https://spring.io/projects/spring-amqp)) and Dapr workflows. 

## Using Spring Data `CrudRepository` and `KeyValueTemplate`

You can use well known Spring Data constructs relying on a Dapr-based implementation. 
With Dapr, you don't need to add any infrastructure-related driver or client, making your Spring application lighter and decoupled from the environment where it is running. 

Under the hood these implementations use the Dapr Statestore and Binding APIs. 

### Configuration parameters

With Spring Data abstractions you can configure which statestore and bindings will be used by Dapr to connect to the available infrastructure. 
This can be done by setting the following properties:

```properties
dapr.statestore.name=kvstore
dapr.statestore.binding=kvbinding
```

Then you can `@Autowire` a `KeyValueTemplate` or a `CrudRepository` like this: 

```java
@RestController
@EnableDaprRepositories
public class OrdersRestController {
  @Autowired
  private OrderRepository repository;
  
  @PostMapping("/orders")
  public void storeOrder(@RequestBody Order order){
    repository.save(order);
  }

  @GetMapping("/orders")
  public Iterable<Order> getAll(){
    return repository.findAll();
  }


}
```

Where `OrderRepository` is defined in an interface that extends the Spring Data `CrudRepository` interface:

```java 
public interface OrderRepository extends CrudRepository<Order, String> {}
```

Notice that the `@EnableDaprRepositories` annotation does all the magic of wiring the Dapr APIs under the `CrudRespository` interface.
Because Dapr allow users to interact with different StateStores from the same application, as a user you need to provide the following beans as a Spring Boot `@Configuration`: 

```java
@Configuration
@EnableConfigurationProperties({DaprStateStoreProperties.class})
public class ProducerAppConfiguration {
  
  @Bean
  public KeyValueAdapterResolver keyValueAdapterResolver(DaprClient daprClient, ObjectMapper mapper, DaprStateStoreProperties daprStatestoreProperties) {
    String storeName = daprStatestoreProperties.getName();
    String bindingName = daprStatestoreProperties.getBinding();

    return new DaprKeyValueAdapterResolver(daprClient, mapper, storeName, bindingName);
  }

  @Bean
  public DaprKeyValueTemplate daprKeyValueTemplate(KeyValueAdapterResolver keyValueAdapterResolver) {
    return new DaprKeyValueTemplate(keyValueAdapterResolver);
  }
  
}
```

## Using Spring Messaging for producing and consuming events

Similar to Spring Kafka, Spring Pulsar and Spring AMQP you can use the `DaprMessagingTemplate` to publish messages to the configured infrastructure. To consume messages you can use the `@Topic` annotation (soon to be renamed to `@DaprListener`). 

To publish events/messages you can `@Autowired` the `DaprMessagingTemplate` in your Spring application. 
For this example we will be publishing `Order` events and we are sending messages to the topic named `topic`.

```java
@Autowired
private DaprMessagingTemplate<Order> messagingTemplate;

@PostMapping("/orders")
public void storeOrder(@RequestBody Order order){
  repository.save(order);
  messagingTemplate.send("topic", order);
}

```

Similarly to the `CrudRepository` we need to specify which PubSub broker do we want to use to publish and consume our messages.

```properties
dapr.pubsub.name=pubsub
```

Because with Dapr you can connect to multiple PubSub brokers you need to provide the following bean to let Dapr know which PubSub broker your `DaprMessagingTemplate` will use: 
```java
@Bean
public DaprMessagingTemplate<Order> messagingTemplate(DaprClient daprClient,
                                                             DaprPubSubProperties daprPubSubProperties) {
  return new DaprMessagingTemplate<>(daprClient, daprPubSubProperties.getName());
}
```

Finally, because Dapr PubSub requires a bidirectional connection between your application and Dapr you need to expand your Testcontainers configuration with a few parameters: 

```java
@Bean
@ServiceConnection
public DaprContainer daprContainer(Network daprNetwork, PostgreSQLContainer<?> postgreSQLContainer, RabbitMQContainer rabbitMQContainer){
    
    return new DaprContainer("daprio/daprd:1.16.0-rc.5")
            .withAppName("producer-app")
            .withNetwork(daprNetwork)
            .withComponent(new Component("kvstore", "state.postgresql", "v1", STATE_STORE_PROPERTIES))
            .withComponent(new Component("kvbinding", "bindings.postgresql", "v1", BINDING_PROPERTIES))
            .withComponent(new Component("pubsub", "pubsub.rabbitmq", "v1", rabbitMqProperties))
            .withAppPort(8080)
            .withAppChannelAddress("host.testcontainers.internal")
            .dependsOn(rabbitMQContainer)
            .dependsOn(postgreSQLContainer);
}
```

Now, in the Dapr configuration we have included a `pubsub` component that will connect to an instance of RabbitMQ started by Testcontainers. 
We have also set two important parameters `.withAppPort(8080)` and `.withAppChannelAddress("host.testcontainers.internal")` which allows Dapr to 
contact back to the application when a message is published in the broker.

To listen to events/messages you need to expose an endpoint in the application that will be responsible to receive the messages. 
If you expose a REST endpoint you can use the `@Topic` annotation to let Dapr know where it needs to forward the events/messages too: 

```java
@PostMapping("subscribe")
@Topic(pubsubName = "pubsub", name = "topic")
public void subscribe(@RequestBody CloudEvent<Order> cloudEvent){
    events.add(cloudEvent);
}
```

Upon bootstrapping your application, Dapr will register the subscription to messages to be forwarded to the `subscribe` endpoint exposed by your application. 

If you are writing tests for these subscribers you need to ensure that Testcontainers knows that your application will be running on port 8080, 
so containers started with Testcontainers know where your application is: 

```java
@BeforeAll
public static void setup(){
  org.testcontainers.Testcontainers.exposeHostPorts(8080);
}
```

You can check and run the [full example source code here](https://github.com/salaboy/dapr-spring-boot-docs-examples).

## Using Dapr Workflows with Spring Boot

Following the same approach that we used for Spring Data and Spring Messaging, the `dapr-spring-boot-starter` brings Dapr Workflow integration for Spring Boot users. 

To work with Dapr Workflows you need to define and implement your workflows using code. The Dapr Spring Boot Starter makes your life easier by managing `Workflow`s and `WorkflowActivity`s as Spring beans.

In order to enable the automatic bean discovery you can annotate your `@SpringBootApplication` with the `@EnableDaprWorkflows` annotation: 

```
@SpringBootApplication
@EnableDaprWorkflows
public class MySpringBootApplication {}
```

By adding this annotation, all the `WorkflowActivity`s will be automatically managed by Spring and registered to the workflow engine. 

By having all `WorkflowActivity`s as managed beans we can use Spring `@Autowired` mechanism to inject any bean that our workflow activity might need to implement its functionality, for example the `@RestTemplate`:

```
public class MyWorkflowActivity implements WorkflowActivity {

  @Autowired
  private RestTemplate restTemplate;
```

You can also `@Autowired` the `DaprWorkflowClient` to create new instances of your workflows. 

```
@Autowired
private DaprWorkflowClient daprWorkflowClient;
```

This enable applications to schedule new workflow instances and raise events.

```
String instanceId = daprWorkflowClient.scheduleNewWorkflow(MyWorkflow.class, payload);
```

and

```
daprWorkflowClient.raiseEvent(instanceId, "MyEvenet", event);
```

Check the [Dapr Workflow documentation](https://docs.dapr.io/developing-applications/building-blocks/workflow/workflow-overview/) for more information about how to work with Dapr Workflows.

## Using Dapr Cloud Config with Spring Boot

To enable dapr cloud config, you should add following properties in your application's config (properties for example):

```properties
# default enable is true, don't need to specify
dapr.cloudconfig.enabled = true
spring.config.import[0] = <schema>
spring.config.import[1] = <schema>
spring.config.import[2] = <schema>
#... keep going if you want to import more configs
```

There are other config of the dapr cloud config, listed below:

```properties
#enable dapr cloud config or not (default = true).
dapr.cloudconfig.enabled=true
#timeout for getting dapr config (include wait for dapr sidecar) (default = 2000).
dapr.cloudconfig.timeout=2000
#whether enable dapr client wait for sidecar, if no response, will throw IOException (default = false).
dapr.cloudconfig.wait-sidecar-enabled=false
#retries of dapr client wait for sidecar (default = 3).
dapr.cloudconfig.wait-sidecar-retries=3
```

In Dapr Cloud Config component, we support two ways to import config: Secret Store API and Configuration API.

Both of them have their schemas, listed below.

### Cloud Config Import Schemas

#### Secret Store Component

##### url structure

`dapr:secret:<store-name>[/<secret-name>][?<paramters>]`

###### paramters

|  parameter  | description | default | available |
|--------------------|--------------------|--------------------|--------------------|
| type | value type | `value` | `value`/`doc`|
| doc-type | type of doc | `properties` | `yaml`/`properties`/`or any file extensions you want`|

- when type = `value`, if `secret-name` is specified, will treat secret as the value of property, and `secret-name` as the key of property; if none `secret-name` is specified, will get bulk secret and treat every value of secret as the value of property, and every key of secret as the key of property.
- when type = `doc`, if `secret-name` is specified, will treat secret as a bunch of property, and load it with property or yaml loader; if none `secret-name` is specified, will get bulk secret and and treat every value of secret as bunches of property, and load them with property or yaml loader.
- secret store with multiValud = true must specify nestedSeparator = ".", and using type = `doc` is not recommanded

##### demo

###### multiValued = false:

####### store content(file secret store as example)

```json
{
	"dapr.spring.demo-config-secret.singlevalue": "testvalue",
	"multivalue-properties": "dapr.spring.demo-config-secret.multivalue.v1=spring\ndapr.spring.demo-config-secret.multivalue.v2=dapr",
	"multivalue-yaml": "dapr:\n  spring:\n    demo-config-secret:\n      multivalue:\n        v3: cloud"
}
```

####### valid demo url

- `dapr:secret:democonfig/multivalue-properties?type=doc&doc-type=properties`
- `dapr:secret:democonfig/multivalue-yaml?type=doc&doc-type=yaml`
- `dapr:secret:democonfig/dapr.spring.demo-config.singlevalue?type=value`
- `dapr:secret:democonfig?type=value`
- `dapr:secret:democonfig?type=doc`

###### multiValued = true, nestedSeparator = ".":

####### store content(file secret store as example)

```json
{
	"value1": {
		"dapr": {
			"spring": {
				"demo-config-secret": {
					"multivalue": {
						"v4": "config"
					}
				}
			}
		}
	}
}
```

will be read as

```json
{
	"value1": {
		"dapr.spring.demo-config-secret.multivalue.v4": "config"
	}
}
```

####### valid demo url
- `dapr:secret:demo-config-multi/value1?type=value`
- `dapr:secret:demo-config-multi?type=value`

#### Configuration Component

##### url structure

`dapr:config:<store-name>[/<key>][?<paramters>]`

###### paramters

|  parameter  | description | default | available |
|--------------------|--------------------|--------------------|--------------------|
| type | value type | `value` | `doc`/`value` |
| doc-type | type of doc | `properties` | `yaml`/`properties`/`or any file extensions you want`|
| subscribe | subscribe this configuration | `false` | `true`/`false` |

- when subscribe = `true`, will subscribe update for the configuration.
- when type = `value`, if `key` is specified, will treat config value as the value of property, and `key` as the key of property; if none `key` is specified, will get all key and value in the `config-name` and treat every config value as the value of property, and every `key` as the key of property.
- when type = `doc`, if `key` is specified, will treat config value as a bunch of property, and load it with property or yaml loader; if none `key` is specified, will get all key and value in the `config-name` and treat every config value as bunches of property, and load them with property or yaml loader.

##### Demo

###### store content(table as example)

| key | value |
|--------------------|--------------------|
| dapr.spring.demo-config-config.singlevalue | testvalue |
| multivalue-properties | `dapr.spring.demo-config-config.multivalue.v1=spring\ndapr.spring.demo-config-config.multivalue.v2=dapr` |
| multivalue-yaml | `dapr:\n  spring:\n    demo-config-config:\n      multivalue:\n        v3: cloud` |

###### valid demo url

- `dapr:config:democonfigconf/dapr.spring.demo-config-config.singlevalue?type=value`
- `dapr:config:democonfigconf/multivalue-properties?type=doc&doc-type=properties`
- `dapr:config:democonfigconf/multivalue-yaml?type=doc&doc-type=yaml`
- `dapr:config:democonfigconf?type=doc`
- `dapr:config:democonfigconf?type=value`

## Next steps

Learn more about the [Dapr Java SDK packages available to add to your Java applications](https://dapr.github.io/java-sdk/).

## Related links
- [Java SDK examples](https://github.com/dapr/java-sdk/tree/master/examples/src/main/java/io/dapr/examples)
