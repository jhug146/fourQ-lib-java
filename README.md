# fourQ-lib-java
A library that implements the high-security, high-performance elliptic curve fourQ. Useful cryptographic functions are provided which include:
* SchnorrQ message signing
* SchnorrQ signature verification
* Public key generation from a private key
* Public-private key pair generation

## Examples
### Public Key Generation
```
final int HEX_RADIX = 16
final BigInteger privateKey = new BigInteger("F510847AAB323", HEX_RADIX);
final BigInteger publicKey = SchnorrQ.schnorrQKeyGeneration(privateKey);
```

### Public-Private Key Pair Generation
```
final Pair<BigInteger, BigInteger> keyPair = SchnorrQ.schnorrQFullKeyGeneration();
```

### SchnorrQ Message Signing
```
final String message = "The quick brown fox jumps over the lazy dog";
try {
    final BigInteger signature = SchnorrQ.schnorrQSign(privateKey, publicKey, message);
} catch (EncryptionException e) {
    println("Error signing message");
}
```

### SchnorrQ Signature Verification
```
if (SchnorrQ.schnorrQVerify(publicKey, signature, message)) {
    println("Signature is correct");
}
```