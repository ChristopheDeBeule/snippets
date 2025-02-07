import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class AES256 {
  static String ALGORITHM = "AES"

  // Generate a random 256-bit (32-byte) AES key
  static String generateKey() {
    KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM)
    keyGen.init(256) // Set key size to 256 bits
    SecretKey secretKey = keyGen.generateKey()
    return Base64.getEncoder().encodeToString(secretKey.encoded) // Encode as Base64
  }

  static SecretKeySpec getKeyFromBase64(String base64Key) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key) // Decode Base64 to bytes
    return new SecretKeySpec(keyBytes, ALGORITHM)
  }

  static String encrypt(String data, String base64Key) {
    SecretKeySpec secretKey = getKeyFromBase64(base64Key)
    Cipher cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"))
    return Base64.getEncoder().encodeToString(encryptedBytes)
  }

  static String decrypt(String encryptedData, String base64Key) {
    SecretKeySpec secretKey = getKeyFromBase64(base64Key)
    Cipher cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    byte[] decodedBytes = Base64.getDecoder().decode(encryptedData)
    return new String(cipher.doFinal(decodedBytes), "UTF-8")
  }
}

String yourStringField = "This is a random string"

// Generate a random key (Make sure you use the same key to encrypt and decrypt the data).
String encryptionKey = AES256.generateKey()

// Encrypt the data, enter your plain text here as the parameter and the key.
def encryptedText = AES256.encrypt(yourStringField, encryptionKey)


// Enter the encrypted Text here as the parameter and the key.
def decryptedText = AES256.decrypt(encryptedText, encryptionKey)
