# Dapr's Secret Store Sample

In this sample, we'll see how to retrieve a secret using Dapr's Java SDK.
This sample includes two files:

* SecretClient.java (Reads a secret from Dapr's Secret Store)
* Existing Dapr component file in `< repo dir >/examples/components/local_file.yaml`

Visit [this](https://docs.dapr.io/developing-applications/building-blocks/secrets/secrets-overview/) link for more information about secret stores in Dapr.

## Secret store sample using the Java-SDK

In this example, the component used is a local file (not recommended for production use), but others are also available.

Visit [this](https://github.com/dapr/components-contrib/tree/master/secretstores) link for more information about secret store implementations.


## Pre-requisites

* [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/).
* Java JDK 11 (or greater):
  * [Microsoft JDK 11](https://docs.microsoft.com/en-us/java/openjdk/download#openjdk-11)
  * [Oracle JDK 11](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11)
  * [OpenJDK 11](https://jdk.java.net/11/)
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

### Creating a JSON secret file locally

Dapr's API for secret store only support read operations. For this sample to run, we will first create a secret file with a JSON string that contains two keys: `redisPassword` and `randomKey`.

<!-- STEP
name: create local file 
-->

```bash
echo '{"redisPassword":"root123","randomKey":"value"}' > ./components/secrets/secret.json
```

<!-- END_STEP -->

### Running the secret store sample

The example's main function is in `SecretClient.java`.

```java
public class SecretClient {

    /**
     * JSON Serializer to print output.
     */
    private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

    /**
     * Client to read a secret.
     *
     * @param args Unused arguments.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Required two argument at least: "
                    + "one's the secret store name, and the others are secret keys.");
        }

        final String secretStoreName = args[0];
        try (DaprClient client = (new DaprClientBuilder()).build()) {

            for (int i = 1; i < args.length; i++) {
                String secretKey = args[i];

                try {
                    Map<String, String> secret = client.getSecret(secretStoreName, secretKey).block();
                    System.out.println(JSON_SERIALIZER.writeValueAsString(secret));
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
```
The program receives two arguments at least: one's the secret store name and the others are secret's keys to be fetched.
After identifying the secret store name that's created and the keys to be fetched, it will retrieve them from the pre-defined secret store: `< repo dir >/examples/components/secrets/secret.json`.
The secret store's name **must** match the component's name defined in `< repo dir >/examples/components/secrets/local_file.yaml`.
The Dapr client is also within a try-with-resource block to properly close the client at the end.

Execute the following script in order to run the example:

<!-- STEP
name: Validate normal run
expected_stdout_lines:
  - '== APP == {"redisPassword":"root123"}'
  - '== APP == {"randomKey":"value"}'
background: true
sleep: 5
-->

```bash
dapr run --components-path ./components/secrets --app-id secrets1 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.secrets.SecretClient localSecretStore redisPassword randomKey
```

<!-- END_STEP -->

Once running, the program should print the output as follows:

```
== APP == {"redisPassword":"root123"}
== APP == {"randomKey":"value"}
```

To close the app either press `CTRL+C` or run

<!-- STEP
name: Cleanup first app
-->

```bash
dapr stop --app-id secrets1
```

<!-- END_STEP -->


The example's `config.yaml` is as follows:
```yaml
apiVersion: dapr.io/v1alpha1
kind: Configuration
metadata:
  name: daprConfig
spec:
  secrets:
    scopes:
      - storeName: "localSecretStore"
        defaultAccess: "deny"
        allowedSecrets: ["redisPassword",]
```

The configuration defines, that the only allowed secret is `redisPassword` and all other secrets are denied.

Execute the following script in order to run this example with additional secret scoping:

<!-- STEP
name: Validate error on querying random secret
expected_stdout_lines:
  - '== APP == {"redisPassword":"root123"}'
  - '== APP == PERMISSION_DENIED: access denied by policy to get "randomKey" from "localSecretStore"'
background: true
sleep: 5
-->

```sh
dapr run --components-path ./components/secrets --config ./src/main/java/io/dapr/examples/secrets/config.yaml --app-id secrets2 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.secrets.SecretClient localSecretStore redisPassword randomKey
```

<!-- END_STEP --> 

Once running, the program should print the output as follows:

```
== APP == {"redisPassword":"root123"}
== APP == PERMISSION_DENIED: access denied by policy to get "randomKey" from "localSecretStore"
``` 

To close the app either press `CTRL+C` or run

<!-- STEP
name: Cleanup second app
-->

```bash
dapr stop --app-id secrets2
```

<!-- END_STEP -->


To clean up the local secret file

<!-- STEP
name: Cleanup local secret file
-->

```sh
rm -rf ./components/secrets/secret.json
```

<!-- END_STEP -->

Thanks for playing.
