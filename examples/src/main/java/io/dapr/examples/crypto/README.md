## Dapr Cryptography API Examples

This example provides the different capabilities provided by Dapr Java SDK for Cryptography. For further information about Cryptography APIs please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/cryptography/cryptography-overview/)

### Using the Cryptography API

The Java SDK exposes several methods for this -
* `client.encrypt(...)` for encrypting data using a cryptography component.
* `client.decrypt(...)` for decrypting data using a cryptography component.

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

### Setting Up the Cryptography Component

Before running the examples, you need to set up a cryptography component. This example uses the local storage crypto component.

1. Create a directory for your keys:

```bash
mkdir -p ~/.dapr/keys
```

2. Generate an RSA key pair (you can use OpenSSL):

```bash
# Generate a 4096-bit RSA private key
openssl genrsa -out ~/.dapr/keys/mykey 4096

# Extract the public key
openssl rsa -in ~/.dapr/keys/mykey -pubout -out ~/.dapr/keys/mykey.pub
```

The component configuration file is already provided in `./components/crypto/localstorage.yaml`.

### Running the Example

This example uses the Java SDK Dapr client to **Encrypt and Decrypt** data.

#### Example 1: Basic Crypto Example

`CryptoExample.java` demonstrates basic encryption and decryption of a simple message.

```java
public class CryptoExample {
  private static final String CRYPTO_COMPONENT_NAME = "localstoragecrypto";
  private static final String KEY_NAME = "mykey";
  private static final String KEY_WRAP_ALGORITHM = "RSA-OAEP-256";

  public static void main(String[] args) {
    try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
      
      String originalMessage = "Hello, Dapr Cryptography!";
      byte[] plainText = originalMessage.getBytes(StandardCharsets.UTF_8);

      // Encrypt the message
      EncryptRequestAlpha1 encryptRequest = new EncryptRequestAlpha1(
          CRYPTO_COMPONENT_NAME,
          Flux.just(plainText),
          KEY_NAME,
          KEY_WRAP_ALGORITHM
      );
      
      byte[] encryptedData = client.encrypt(encryptRequest)
          .collectList()
          .map(chunks -> /* combine chunks */)
          .block();

      // Decrypt the message
      DecryptRequestAlpha1 decryptRequest = new DecryptRequestAlpha1(
          CRYPTO_COMPONENT_NAME,
          Flux.just(encryptedData)
      );
      
      byte[] decryptedData = client.decrypt(decryptRequest)
          .collectList()
          .map(chunks -> /* combine chunks */)
          .block();
    }
  }
}
```

Use the following command to run this example:

<!-- STEP
name: Run Crypto Example
expected_stdout_lines:
  - "== APP == ✓ Success! The decrypted message matches the original."
background: true
output_match_mode: substring
sleep: 10
-->

```bash
dapr run --resources-path ./components/crypto --app-id crypto-app --dapr-http-port 3500 --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.crypto.CryptoExample
```

<!-- END_STEP -->

#### Example 2: Streaming Crypto Example

`StreamingCryptoExample.java` demonstrates advanced scenarios including:
- Multi-chunk data encryption
- Large data encryption (100KB+)
- Custom encryption ciphers

```bash
dapr run --resources-path ./components/crypto --app-id crypto-app --dapr-http-port 3500 --dapr-grpc-port 50001 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.crypto.StreamingCryptoExample
```

### Sample Output

```
=== Dapr Cryptography Example ===
Original message: Hello, Dapr Cryptography! This is a secret message.

Encrypting message...
Encryption successful!
Encrypted data length: 512 bytes

Decrypting message...
Decryption successful!
Decrypted message: Hello, Dapr Cryptography! This is a secret message.

✓ Success! The decrypted message matches the original.
```

### Supported Key Wrap Algorithms

The following key wrap algorithms are supported:
- `A256KW` (alias: `AES`) - AES key wrap
- `A128CBC`, `A192CBC`, `A256CBC` - AES CBC modes
- `RSA-OAEP-256` (alias: `RSA`) - RSA OAEP with SHA-256

### Supported Data Encryption Ciphers

Optional data encryption ciphers:
- `aes-gcm` (default) - AES in GCM mode
- `chacha20-poly1305` - ChaCha20-Poly1305 cipher

### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id crypto-app
```

<!-- END_STEP -->
