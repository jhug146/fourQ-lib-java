# fourQ-lib-java
A library that implements the high-security, high-performance elliptic curve fourQ. Useful cryptographic functions are provided which include:
* SchnorrQ message signing
* SchnorrQ signature verification
* Public key generation from a private key
* Public-private key pair generation

# Dependency

## Gradle
Add the following dependency to your `build.gradle` file:

```gradle
implementation("com.namanmalhotra:fourQ:1.0.1")
```

## Maven
Add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>com.namanmalhotra</groupId>
    <artifactId>fourQ</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Import
After adding the dependency, import the main class in your Java code:

```java
import api.SchnorrQ;
```


## Examples
### Public Key Generation
```java
final SchnorrQ schnorrQ = new SchnorrQ();
final int HEX_RADIX = 16;
final BigInteger privateKey = new BigInteger("F510847AAB323", HEX_RADIX);
try {
    final BigInteger publicKey = schnorrQ.schnorrQKeyGeneration(privateKey);
} catch (EncryptionException e) {
    System.err.println("Error generating public key: " + e.getMessage());
}
```

### Public-Private Key Pair Generation
```java
final SchnorrQ schnorrQ = new SchnorrQ();
try {
    final Pair<BigInteger, BigInteger> keyPair = schnorrQ.schnorrQFullKeyGeneration();
    final BigInteger privateKey = keyPair.first;
    final BigInteger publicKey = keyPair.second;
} catch (EncryptionException e) {
    System.err.println("Error generating key pair: " + e.getMessage());
}
```

### SchnorrQ Message Signing
```java
final SchnorrQ schnorrQ = new SchnorrQ();
final String message = "The quick brown fox jumps over the lazy dog";
final byte[] messageBytes = message.getBytes();
try {
    final BigInteger signature = schnorrQ.schnorrQSign(privateKey, publicKey, messageBytes);
} catch (EncryptionException e) {
    System.err.println("Error signing message: " + e.getMessage());
}
```

### SchnorrQ Signature Verification
```java
final SchnorrQ schnorrQ = new SchnorrQ();
try {
    if (schnorrQ.schnorrQVerify(publicKey, signature, messageBytes)) {
        System.out.println("Signature is valid");
    } else {
        System.out.println("Signature is invalid");
    }
} catch (EncryptionException e) {
    System.err.println("Error verifying signature: " + e.getMessage());
}
```

### Test Project
For more reference on implementation and use cases refer to the following [project](https://github.com/malhotranaman/FourQDependencyTest.git).
Noting however that this assumes version 1.0.1, but subsequent versions maintain parity.