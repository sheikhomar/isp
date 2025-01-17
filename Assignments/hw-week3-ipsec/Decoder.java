/*
 * To compile and run this program, you need "bouncycastle.jar", which can be obtained
 * from https://www.bouncycastle.org/latest_releases.html
 * For convenience, we have already included bcprov-jdk15on-155.jar, which is a recent version.
 * Compiling: javac -classpath bcprov-jdk15on-155.jar Decoder.java
 * Running:   java -cp bcprov-jdk15on-155.jar:. Decoder
 */
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Decoder {
  public static final int ESP_START = 34;
  public static final int ESP_SPI_START = ESP_START;
  public static final int ESP_SPI_END = ESP_SPI_START + 4;
  public static final int ESP_SEQ_START = ESP_SPI_END;
  public static final int ESP_SEQ_END = ESP_SEQ_START + 4;

  public static final int AES_BLOCK_SIZE = 16;

  public static final int CAMELLIA_BLOCK_SIZE = 16;

  /**
   Source: https://tools.ietf.org/html/rfc4868
   Block size:  the size of the data block the underlying hash algorithm
   operates upon.  For SHA-256, this is 512 bits, for SHA-384 and
   SHA-512, this is 1024 bits.

   Output length:  the size of the hash value produced by the underlying
   hash algorithm.  For SHA-256, this is 256 bits, for SHA-384 this
   is 384 bits, and for SHA-512, this is 512 bits.

   Authenticator length:  the size of the "authenticator" in bits.  This
   only applies to authentication/integrity related algorithms, and
   refers to the bit length remaining after truncation.  In this
   specification, this is always half the output length of the
   underlying hash algorithm.

    256 bits / 8 = 32 bytes. Half of it is 16 bytes.
   However, 16 bytes does not work but 12 bytes work!
  */
  public static final int HMAC_SHA2_256_LEN = 12;

  /**
   HMAC-MD5-96 produces a 128-bit authenticator value.  This 128-bit
   value can be truncated as described in RFC 2104.  For use with either
   ESP or AH, a truncated value using the first 96 bits MUST be
   supported.
   96 bits / 8 = 12 bytes!
  */
  public static final int HMAC_MD5_96_LEN = 12;

  public static void main(String[] args) throws UnsupportedEncodingException {
    Security.addProvider(new BouncyCastleProvider());

    byte[] key = "YELLOW SUBMARINE".getBytes("UTF-8");

    //System.out.println("\n\n===========================\nTry to decrypt packet 1\n");
    extractSecretMessage(espPacket1, key, "AES", HMAC_MD5_96_LEN);

    //System.out.println("\n\n===========================\nTry to decrypt packet 2\n");
    //extractSecretMessage(espPacket2, key, "AES", HMAC_MD5_96_LEN);

    extractSecretMessage(camelliaEncodedPacket, key, "Camellia", HMAC_SHA2_256_LEN);
  }

  private static void extractSecretMessage(byte[] packet, byte[] key, String encryptionMethod, int authenticationDataLength) throws UnsupportedEncodingException {
    System.out.println("Packet ("+packet.length+" bytes): " + toHex(packet));

    byte[] spi = Arrays.copyOfRange(packet, ESP_SPI_START, ESP_SPI_END);
    System.out.println("SPI:   " + toHex(spi));

    byte[] seqNo = Arrays.copyOfRange(packet, ESP_SEQ_START, ESP_SEQ_END);
    System.out.println("SeqNo: " + toHex(seqNo));

    byte[] authData = Arrays.copyOfRange(packet, packet.length - authenticationDataLength, packet.length);
    System.out.println("Authentication Data ("+authData.length+" bytes): " + toHex(authData));

    System.out.println("Key:   " + toHex(key));

    int encryptedDataStart = ESP_SEQ_END;
    int encryptedDataEnd = packet.length - authData.length;
    byte[] encryptedData = Arrays.copyOfRange(packet, encryptedDataStart, encryptedDataEnd);
    System.out.println("Encrypted Data ("+encryptedData.length+" bytes):   " + toHex(encryptedData));

    byte[] decryptedData = decrypt(encryptedData, key, encryptionMethod);

    printOutSecret(decryptedData);
  }

  private static byte[] decrypt(byte[] encryptedData, byte[] key, String encryptionMethod) {
    try {
      Cipher cipher = null;

      if ("Camellia".equalsIgnoreCase(encryptionMethod)) {
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, CAMELLIA_BLOCK_SIZE);
        System.out.println("IV:    " + toHex(iv));
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        encryptedData = Arrays.copyOfRange(encryptedData, iv.length, encryptedData.length);

        cipher = Cipher.getInstance("Camellia/CBC/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "Camellia");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

      } else if ("AES".equalsIgnoreCase(encryptionMethod)) {
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, AES_BLOCK_SIZE);
        System.out.println("IV:    " + toHex(iv));
        encryptedData = Arrays.copyOfRange(encryptedData, iv.length, encryptedData.length);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec secretKeySpecy = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpecy, ivParameterSpec);
      } else {
        throw new IllegalArgumentException("Method '"+encryptionMethod+"' is unknown.");
      }
      System.out.println("Decrypting data:    " + toHex(encryptedData));

      return cipher.doFinal(encryptedData);
    } catch (Exception e) {
      System.out.println("Failed to decrypt:");
      e.printStackTrace();
    }

    return null;
  }

  private static void printOutSecret(byte[] decryptedData) {
    if (decryptedData == null) return;

    System.out.println("Decrypted Data ("+decryptedData.length+" bytes): " + toHex(decryptedData));

    byte nextHeader = decryptedData[decryptedData.length-1];
    System.out.println("Next header: " + toHex(new byte[] { nextHeader}));

    byte padLen = decryptedData[decryptedData.length-2];
    System.out.println("Padding length ("+padLen+" bytes): " + toHex(new byte[] { padLen}));

    int paddingFrom = decryptedData.length - ((int)padLen) - 2;
    int paddingTo = decryptedData.length - 2;
    byte[] paddingData = Arrays.copyOfRange(decryptedData, paddingFrom, paddingTo);;
    System.out.println("Padding Data: " + toHex(paddingData));


    byte[] secretMessage = Arrays.copyOfRange(decryptedData, 28, decryptedData.length - paddingData.length - 2);
    System.out.println("Secret Message: '" + new String(secretMessage) + "'");

  }

  private static String toHex(byte...input){
    if (input == null)
      return "<null>";

    String prefix = "\n  ";
    String result = prefix;
    int counter = 1;
    for(byte b:input){
      result += String.format("%02x ", b);

      if (counter % 8 == 0) {
        result += "  ";
      }

      if (counter % 16 == 0) {
        result += prefix;
      }
      counter++;
    }

    if (result.endsWith("  "))
      result = result.substring(0, result.length()-3);
    result += "\n";
    return result;
  }

  private static byte[] espPacket1 = {
    (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0x03, (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0x02, (byte)0x08, (byte)0x00, (byte)0x45, (byte)0x00,
    (byte)0x00, (byte)0x78, (byte)0x00, (byte)0x42, (byte)0x40, (byte)0x00, (byte)0x42, (byte)0x32, (byte)0x20, (byte)0x0e, (byte)0x0a, (byte)0x00, (byte)0x02, (byte)0x03, (byte)0x0a, (byte)0x00,
    (byte)0x02, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x05, (byte)0x39, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x0e, (byte)0xbc, (byte)0x21, (byte)0x67, (byte)0xcd, (byte)0x4c,
    (byte)0xd1, (byte)0xd4, (byte)0xac, (byte)0x47, (byte)0x3f, (byte)0x9e, (byte)0xdd, (byte)0x71, (byte)0x67, (byte)0x93, (byte)0xcc, (byte)0x19, (byte)0xfe, (byte)0x80, (byte)0x0d, (byte)0x57,
    (byte)0x50, (byte)0xf2, (byte)0x64, (byte)0x9e, (byte)0xfe, (byte)0xac, (byte)0xaf, (byte)0xe6, (byte)0x1a, (byte)0x9e, (byte)0x5e, (byte)0xb6, (byte)0x47, (byte)0x5c, (byte)0x6e, (byte)0x07,
    (byte)0x77, (byte)0x44, (byte)0xf2, (byte)0x46, (byte)0xf2, (byte)0x74, (byte)0x3d, (byte)0xc4, (byte)0xbc, (byte)0x91, (byte)0x38, (byte)0xc6, (byte)0x71, (byte)0x70, (byte)0xe4, (byte)0xfb,
    (byte)0x07, (byte)0x8e, (byte)0xa6, (byte)0x81, (byte)0x65, (byte)0xad, (byte)0xdd, (byte)0x0a, (byte)0x4e, (byte)0x3c, (byte)0x11, (byte)0x59, (byte)0x75, (byte)0x48, (byte)0x20, (byte)0xf6,
    (byte)0x0f, (byte)0x5d, (byte)0xff, (byte)0x28, (byte)0x33, (byte)0x27, (byte)0x9e, (byte)0x3d, (byte)0x4d, (byte)0x15, (byte)0x3f, (byte)0xd8, (byte)0xf5, (byte)0xc9, (byte)0x14, (byte)0xd7,
    (byte)0x33, (byte)0x6d, (byte)0xb4, (byte)0x72, (byte)0x34, (byte)0xf6
  };

  private static byte[] espPacket2 = {
    (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0x02, (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0x03, (byte)0x08, (byte)0x00, (byte)0x45, (byte)0x00,
    (byte)0x00, (byte)0x78, (byte)0x00, (byte)0x42, (byte)0x40, (byte)0x00, (byte)0x42, (byte)0x32, (byte)0x20, (byte)0x0e, (byte)0x0a, (byte)0x00, (byte)0x02, (byte)0x02, (byte)0x0a, (byte)0x00,
    (byte)0x02, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x13, (byte)0x37, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0xc4, (byte)0xd2, (byte)0x78, (byte)0x9b, (byte)0xee, (byte)0x3a,
    (byte)0xca, (byte)0xcb, (byte)0x8b, (byte)0x2e, (byte)0x47, (byte)0x0e, (byte)0x44, (byte)0x7c, (byte)0x49, (byte)0x82, (byte)0x1b, (byte)0x7f, (byte)0x4f, (byte)0x5d, (byte)0x07, (byte)0x53,
    (byte)0xd3, (byte)0x41, (byte)0x26, (byte)0x54, (byte)0xb7, (byte)0x01, (byte)0xf7, (byte)0xb5, (byte)0xf4, (byte)0xd9, (byte)0xee, (byte)0x3b, (byte)0x19, (byte)0x94, (byte)0xea, (byte)0x2a,
    (byte)0xae, (byte)0xa4, (byte)0x9c, (byte)0xba, (byte)0xbd, (byte)0xba, (byte)0xe9, (byte)0x92, (byte)0x07, (byte)0xe3, (byte)0x65, (byte)0xa8, (byte)0x83, (byte)0xc7, (byte)0x9d, (byte)0xa5,
    (byte)0x76, (byte)0x43, (byte)0x7c, (byte)0xbf, (byte)0x3f, (byte)0x61, (byte)0x1f, (byte)0x2d, (byte)0x6e, (byte)0x69, (byte)0xb7, (byte)0xff, (byte)0x42, (byte)0xc5, (byte)0x9c, (byte)0x6e,
    (byte)0x77, (byte)0x7c, (byte)0xfb, (byte)0xe1, (byte)0x75, (byte)0x6c, (byte)0xfb, (byte)0xbf, (byte)0x68, (byte)0x5a, (byte)0xb3, (byte)0xb6, (byte)0x6c, (byte)0x32, (byte)0x37, (byte)0xfb,
    (byte)0x8e, (byte)0x6b, (byte)0x58, (byte)0xbe, (byte)0x55, (byte)0x01
  };

  private static byte[] camelliaEncodedPacket = {
    (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0x03, (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0x02, (byte)0x08, (byte)0x00, (byte)0x45, (byte)0x00,
    (byte)0x00, (byte)0x78, (byte)0x00, (byte)0x42, (byte)0x40, (byte)0x00, (byte)0x42, (byte)0x32, (byte)0x20, (byte)0x0e, (byte)0x0a, (byte)0x00, (byte)0x02, (byte)0x03, (byte)0x0a, (byte)0x00,
    (byte)0x02, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x05, (byte)0x39, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0xa7, (byte)0x82, (byte)0x3b, (byte)0xa0, (byte)0xf9, (byte)0xe5,
    (byte)0x0d, (byte)0xb2, (byte)0xce, (byte)0x4e, (byte)0x17, (byte)0xbe, (byte)0x16, (byte)0xde, (byte)0xcc, (byte)0xb0, (byte)0x0e, (byte)0x80, (byte)0x3a, (byte)0x8c, (byte)0x62, (byte)0x8a,
    (byte)0x73, (byte)0x88, (byte)0x01, (byte)0x4d, (byte)0x11, (byte)0xbc, (byte)0xab, (byte)0x7d, (byte)0x7e, (byte)0x35, (byte)0x04, (byte)0x5e, (byte)0x74, (byte)0x79, (byte)0x3d, (byte)0x5a,
    (byte)0x1a, (byte)0x1d, (byte)0x4e, (byte)0x58, (byte)0xec, (byte)0x84, (byte)0x18, (byte)0x2b, (byte)0x71, (byte)0x9f, (byte)0xbe, (byte)0x51, (byte)0xff, (byte)0x5e, (byte)0x51, (byte)0x81,
    (byte)0xab, (byte)0x70, (byte)0xaa, (byte)0x1a, (byte)0x99, (byte)0x6f, (byte)0x2e, (byte)0x87, (byte)0xb1, (byte)0x99, (byte)0xc4, (byte)0xe3, (byte)0x61, (byte)0x16, (byte)0x87, (byte)0xe3,
    (byte)0x7f, (byte)0xc4, (byte)0x4d, (byte)0xaa, (byte)0xb5, (byte)0x8d, (byte)0xdd, (byte)0xa7, (byte)0x84, (byte)0x0a, (byte)0x7e, (byte)0x33, (byte)0xfd, (byte)0x4a, (byte)0xf6, (byte)0x07,
    (byte)0x5c, (byte)0xc4, (byte)0xfc, (byte)0x55, (byte)0x15, (byte)0x4e
  };
}