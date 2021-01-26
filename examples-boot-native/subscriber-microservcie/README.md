### Checking out the code

Clone this repository:

```shell
git clone https://github.com/dapr/java-sdk.git
cd java-sdk
```

Then build the Maven project:

```shell
# make sure you are in the `java-sdk` directory.
mvn install
```

Then get into the `examples-boot-native` directory:

```shell
cd examples-boot-native
```

The output jar is `subscriber-microservice/target/subscriber-microservice.jar`

### Running the subscriber

**In this demo, you should run subscriber-microservice first.**

running this microservice in dapr, e.g. using port 8090,

```shell
dapr run -a subscriber -p 8090 -- java -jar subscriber-microservice/target/subscriber-microservice.jar --server.port=8090 &
```

You can also create a file `application.properties` in `subscriber-microservice/src/main/resources`, with content:

```properties
server.port=8090
```

then rebuild and exec dapr CLI with

```shell
dapr run -a subscriber -p 8090 -- java -jar subscriber-microservice/target/subscriber-microservice.jar &
```

as `server.port` is the key used to set the port that spring boot applications listen on.

since subscriber ran first, you should get this in subscriber's terminal after publisher is running:

```
== APP == Published message: This is message #0

== APP == Subscriber got: {"id":"9abe67b7-6565-47bb-b2cc-234b8db40c0a","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #0"}

== APP == Published message: This is message #1

== APP == Subscriber got: {"id":"a14b1426-86c9-4f4a-aa38-3ac49a40f2d8","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #1"}

== APP == Published message: This is message #2

== APP == Subscriber got: {"id":"be83bab7-7d69-4ff3-8537-087ba56b158a","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #2"}

== APP == Published message: This is message #3

== APP == Subscriber got: {"id":"9e677a56-a3dd-4009-85f9-346ef39a4797","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #3"}

== APP == Published message: This is message #4

== APP == Subscriber got: {"id":"6c81e262-cc16-498f-928e-bed5ab2356a4","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #4"}

== APP == Published message: This is message #5

== APP == Subscriber got: {"id":"11eded9a-dd68-4a12-8560-6433124ffc68","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #5"}

== APP == Subscriber got: {"id":"c699e25b-1b02-48d0-8321-30a4cf11a972","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #6"}

== APP == Published message: This is message #6

== APP == Published message: This is message #7

== APP == Subscriber got: {"id":"01620c88-333d-4e25-ac4e-bb0a29b77e57","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #7"}

== APP == Subscriber got: {"id":"87bcfa69-2dbe-4e7f-b499-740ef381e8b1","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #8"}

== APP == Published message: This is message #8

== APP == Published message: This is message #9

== APP == Subscriber got: {"id":"9a1d3ba3-04a1-42bb-8d3b-8f753fc0a348","source":"pub","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #9"}
```

this demo uses dapr's default subpub.