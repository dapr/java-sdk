## State management via Dapr's HTTP endpoint using the Java-SDK

This example shows how to write and read data on Dapr via HTTP. 

## Pre-requisites

* [Dapr and Dapr Cli](https://github.com/dapr/docs/blob/master/getting-started/environment-setup.md#environment-setup).
* Java JDK 11 (or greater): [Oracle JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11) or [OpenJDK](https://jdk.java.net/13/).
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

### Understanding the code

This example implements a service listening on port 3000, while using Dapr's state store via port 3500. Its API also offers two methods: `/order` and `/neworder`. Calls to '/order' will fetch the state from Dapr's state store:

```
        String stateUrl = String.format("http://localhost:%s/v1.0/state", daprPort);
        /// ...
        httpServer.createContext("/order").setHandler(e -> {
            out.println("Fetching order!");
            fetch(stateUrl + "/order").thenAccept(response -> {
                int resCode = response.statusCode() == 200 ? 200 : 500;
                String body = response.statusCode() == 200 ? response.body() : "Could not get state.";

                try {
                    e.sendResponseHeaders(resCode, body.getBytes().length);
                    OutputStream os = e.getResponseBody();
                    try {
                        os.write(body.getBytes());
                    } finally {
                        os.close();
                    }
                } catch (IOException ioerror) {
                    out.println(ioerror);
                }
            });
        });
```

Calls to `/neworder` will persist a new state do Dapr's state store:
```
        httpServer.createContext("/neworder").setHandler(e -> {
            try {
                out.println("Received new order ...");
                String json = readBody(e);
                JSONObject jsonObject = new JSONObject(json);
                JSONObject data = jsonObject.getJSONObject("data");
                String orderId = data.getString("orderId");
                out.printf("Got a new order! Order ID: %s\n", orderId);

                JSONObject item = new JSONObject();
                item.put("key", "order");
                item.put("value", data);
                JSONArray state = new JSONArray();
                state.put(item);
                out.printf("Writing to state: %s\n", state.toString());

                post(stateUrl, state.toString()).thenAccept(response -> {
                    int resCode = response.statusCode() == 200 ? 200 : 500;
                    String body = response.body();
                    try {
                        e.sendResponseHeaders(resCode, body.getBytes().length);
                        OutputStream os = e.getResponseBody();
                        try {
                            os.write(body.getBytes());
                        } finally {
                            os.close();
                        }
                    } catch (IOException ioerror) {
                        out.println(ioerror);
                    }
                });
            } catch (IOException ioerror) {
                out.println(ioerror);
            }
        });
```

### Running the example

Now, run this example with the following command:
```sh
dapr run --app-id orderapp --app-port 3000 --port 3500 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.state.http.OrderManager
```

Use Dapr cli to invoke the APIs for this service. Initially, save a new order:
```sh
dapr invoke --app-id orderapp --method neworder --payload "{\"data\": { \"orderId\": \"41\" } }"
```

See the last order by invoking the `/order` method:
```sh
dapr invoke --app-id orderapp --method order
```

Finally, change the values for `orderId` and see if it gets updated by invoking `/order` again.

Thanks for playing.