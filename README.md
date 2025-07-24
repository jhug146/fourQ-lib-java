# fourQ-lib-java
A library that implements the high-security, high-performance elliptic curve fourQ. Useful cryptographic functions are provided which include:
* fourqj.api.SchnorrQ message signing
* fourqj.api.SchnorrQ signature verification
* Public key generation from a private key
* Public-private key pair generation

# Note on Endianness

This Java implementation of FourQ maintains compatibility with the original C reference implementation, which uses **little-endian** byte ordering. However, Java's `BigInteger` class and most Java operations naturally work with **big-endian** byte ordering.

To bridge this compatibility gap, the library performs byte array reversals at specific points in the cryptographic operations. This ensures that:

- **Input/Output Compatibility**: Keys and signatures are compatible with other FourQ implementations
- **Internal Consistency**: Java's big-endian arithmetic works correctly with the curve parameters
- **Cross-Platform Interoperability**: Data can be exchanged with C/C++ FourQ implementations

## Example: Byte Array Reversal

The library includes utility functions for handling endianness conversions:

```java
import fourqj.utils.ByteArrayUtils;
import fourqj.utils.ByteArrayReverseMode;

import java.util.Optional;

// Example: Converting between little-endian and big-endian representations

public static byte[] reverseForCompatibility(byte[] data) {
    return ByteArrayUtils.reverseByteArray(data, Optional.empty());
}

        // Usage example
        byte[] littleEndianData = {0x01, 0x02, 0x03, 0x04};
        byte[] bigEndianData = reverseForCompatibility(littleEndianData);
// Result: {0x04, 0x03, 0x02, 0x01}
```

**Note**: Users of the library typically don't need to handle these conversions manually, as they are performed automatically within the cryptographic operations.
**Secondary Note**: Reversals are often compounded with padding fixes that can be seen in some util classes. These padding arise due to BigInteger's natural zero appending patterns to the front of byte arrays, that can cause errors when reversed.


# Dependency

## Gradle
Add the following dependency to your `build.gradle` file:

```gradle
implementation("com.namanmalhotra:fourQ:1.0.2")
```

## Maven
Add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>com.namanmalhotra</groupId>
    <artifactId>fourQ</artifactId>
    <version>1.0.2</version>
</dependency>
```

## Import
After adding the dependency, import the main class in your Java code:

```java
import fourqj.api.SchnorrQ;
```


## Examples
### Public Key Generation
```java
final fourqj.api.SchnorrQ schnorrQ = new fourqj.api.SchnorrQ();
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
final fourqj.api.SchnorrQ schnorrQ = new fourqj.api.SchnorrQ();
try {
    final Pair<BigInteger, BigInteger> keyPair = schnorrQ.schnorrQFullKeyGeneration();
    final BigInteger privateKey = keyPair.first;
    final BigInteger publicKey = keyPair.second;
} catch (EncryptionException e) {
    System.err.println("Error generating key pair: " + e.getMessage());
}
```

### fourqj.api.SchnorrQ Message Signing
```java
final fourqj.api.SchnorrQ schnorrQ = new fourqj.api.SchnorrQ();
final String message = "The quick brown fox jumps over the lazy dog";
final byte[] messageBytes = message.getBytes();
try {
    final BigInteger signature = schnorrQ.schnorrQSign(privateKey, publicKey, messageBytes);
} catch (EncryptionException e) {
    System.err.println("Error signing message: " + e.getMessage());
}
```

### fourqj.api.SchnorrQ Signature Verification
```java
final fourqj.api.SchnorrQ schnorrQ = new fourqj.api.SchnorrQ();
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