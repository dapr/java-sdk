# Dapr's Secret Store Sample

In this sample, we'll see how to retrieve a secret using Dapr's Java SDK. 
This sample includes two files:

* docker-compose-vault.yml (Starts Hashicorp's Vault as a container)
* SecretClient.java (Reads a secret from Dapr's Secret Store)
* Existing Dapr component file in `< repo dir >/examples/components/hashicorp_vault.yaml`
* Existing token file in `< repo dir >/examples/.hashicorp_vault_token` (Consumed by `daprd`'s vault component above)

Visit [this](https://github.com/dapr/docs/tree/master/concepts/secrets) link for more information about secret stores in Dapr.
 
## Secret store sample using the Java-SDK

In this example, the component used is Hashicorp Vault, but others are also available.

Visit [this](https://github.com/dapr/components-contrib/tree/master/secretstores) link for more information about secret stores implementations.


## Pre-requisites

* [Dapr and Dapr Cli](https://github.com/dapr/docs/blob/master/getting-started/environment-setup.md#environment-setup).
* Java JDK 11 (or greater): [Oracle JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11) or [OpenJDK](https://jdk.java.net/13/).
* [Apache Maven](https://maven.apache.org/install.html) version 3.x.
* Hashicorp's vault client [installed](https://www.vaultproject.io/docs/install/).

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
### Setting Vault locally

Before getting into the application code, follow these steps in order to setup a local instance of Vault. This is needed for the local instances. Steps are:

1. navigate to the [examples] with `cd examples`
2. Run `docker-compose -f ./src/main/java/io/dapr/examples/secrets/docker-compose-vault.yml up -d` to run the container locally
3. Run `docker ps` to see the container running locally: 

```bash
342d3522ca14        vault                      "docker-entrypoint.s…"         34 seconds ago        Up About
a minute   0.0.0.0:8200->8200/tcp                               secrets_hashicorp_vault_1
```
Click [here](https://hub.docker.com/_/vault/) for more information about the container image for Hashicorp's Vault.

### Create a secret in Vault
Dapr's API for secret store only support read operations. For this sample to run, we will first create a secret via the Vault's cli commands:

1. Login:
```bash
vault login myroot
```

> Note: If you get `http: server gave HTTP response to HTTPS client` make sure the local vault address is set `export VAULT_ADDR=http://127.0.0.1:8200/`

2. Create secret (replace `[my favorite movie]` with a title of our choice):
```bash
vault kv put secret/dapr/movie title="[my favorite movie]"
```

In the command above, `secret` means the secret engine in Hashicorp's Vault.
Then, `dapr` is the prefix as defined in `< repo dir >/examples/components/hashicorp_vault.yaml`.
Finally, `movie` is the secret name and then a `key=value` pair.

A secret in dapr is a dictionary. In this sample, only one key-value pair is used but more can be added as an exercise for the reader.

### Running the secret store sample

The example's main function is in `SecretClient.java`.

```java
public class SecretClient {
    /**
     * Identifier in Dapr for the secret store.
     */
    private static final String SECRET_STORE_NAME = "vault";
  
    /**
     * JSON Serializer to print output.
     */
    private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();
  
  ///...

  public static void main(String[] args) throws Exception {
      if (args.length != 1) {
        throw new IllegalArgumentException("Use one argument: secret's key to be retrieved.");
      }
  
      String secretKey = args[0];
      try (DaprClient client = (new DaprClientBuilder()).build()) {
        Map<String, String> secret = client.getSecret(SECRET_STORE_NAME, secretKey).block();
        System.out.println(JSON_SERIALIZER.writeValueAsString(secret));
      }
    }
///...
}
```
The program receives one and only one argument: the secret's key to be fetched.
After identifying the key to be fetched, it will retrieve it from the pre-defined secret store: `vault`.
The secret store's name **must** match the component's name defined in `< repo dir >/examples/components/hashicorp_vault.yaml`.
The Dapr client is also within a try-with-resource block to properly close the client at the end.

 Execute the follow script in order to run the example:
```sh
cd to [repo-root]/examples
dapr run --components-path ./components  -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.secrets.SecretClient movie
```

Once running, the program should print the output as follows:

```
== APP == {"title":"[my favorite movie]"}
```

To close the app, press CTRL+c.

To cleanup and bring the vault container down, run
```sh
docker-compose -f ./src/main/java/io/dapr/examples/secrets/docker-compose-vault.yml down
```

Thanks for playing.
