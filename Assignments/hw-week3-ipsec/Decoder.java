/*
 * To compile and run this program, you need "bouncycastle.jar", which can be obtained
 * from https://www.bouncycastle.org/latest_releases.html
 * For convenience, we have already included bcprov-jdk15on-155.jar, which is a recent version.
 * Compiling: javac -classpath <bouncycastle.jar> Example-aes.java
 *            javac -classpath bcprov-jdk15on-155.jar Example-aes.java
 * Running: java -cp <bouncycastle.jar>:. Example-aes
 *          java -cp bcprov-jdk15on-155.jar:. Example-aes
 *  
 *  In this example we encrypt "abcdefghijklmnopabcdefghijklmnop" with key:
 *   [0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f]
 *   and iv:
 *   [0x0f, 0x0e, 0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00]
 *   using aes in cbc mode without padding.
 *  Than we decrypt the obtained ciphertext.
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
  public static final int ESP_HEADER_SIZE = 8;
  public static final int ESP_SPI_SIZE = 8;
  public static final int SHA_2_256_BLOCK_SIZE = 512 / 8;
  public static final int SHA_2_256_RESULT_SIZE = 256 / 8;
  public static final int MD5_RESULT_SIZE = 16;
  public static final int AES_BLOCK_SIZE = 16;

  public static void main(String[] args) throws UnsupportedEncodingException {
    System.out.println("\n\n===========================\nTry to decrypt packet 1\n");
    decryptWithAES(espPacket1);

    System.out.println("\n\n===========================\nTry to decrypt packet 2\n");
    decryptWithAES(espPacket2);
  }

  public static void test3() {
    System.out.println("Data:    " + toHex(espPacket1));
  }

  public static void decryptWithAES(byte[] packet) throws UnsupportedEncodingException {
    System.out.println("Packet ("+packet.length+" bytes): " + toHex(packet));

    int espStartPos = 34;

    byte[] spi = Arrays.copyOfRange(packet, espStartPos, espStartPos+4);
    System.out.println("SPI:   " + toHex(spi));

    byte[] seqNo = Arrays.copyOfRange(packet, espStartPos+4, espStartPos+8);
    System.out.println("SeqNo: " + toHex(seqNo));

    // IV for AES must be 16 bytes long.
    // Assume that IV is comes after the Sequence number.
    byte[] iv = Arrays.copyOfRange(packet, espStartPos+8, espStartPos+8+AES_BLOCK_SIZE);
    System.out.println("IV:    " + toHex(iv));

    byte[] key = "YELLOW SUBMARINE".getBytes("UTF-8");
    System.out.println("Key:   " + toHex(key));

    int sizeOfAuth = 12;
    byte[] authData = Arrays.copyOfRange(packet, packet.length - 12, packet.length);
    System.out.println("Authentication Data: " + toHex(authData));

    int espPayLoadDataPos = espStartPos+8+AES_BLOCK_SIZE;
    int len = packet.length - espPayLoadDataPos - authData.length;

    byte[] decryptedData = tryDecrypt(packet, key, iv, espPayLoadDataPos, len);
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

  public static byte[] tryDecrypt(byte[] packet, byte[] key, byte[] iv, int espPayLoadDataPos, int len) {
    try {
      // Create cipher aes in cbc mode without padding
      Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
      
      // Initiate the cipher in decryption mode with the correct iv and key
      SecretKeySpec secretKeySpecy = new SecretKeySpec(key, "AES");
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      aes.init(Cipher.DECRYPT_MODE, secretKeySpecy, ivParameterSpec);

      byte[] cipherText = Arrays.copyOfRange(packet, espPayLoadDataPos, espPayLoadDataPos+len);

      System.out.println("Trying to decrypt byte array of size "+ len + ": " + toHex(cipherText));

      byte[] decryptedPlaintext = aes.doFinal(cipherText);
      return decryptedPlaintext;
    } catch (Exception e) {
      System.out.println("Failed to encrypt:");
      e.printStackTrace();
      return null;
    }
  }

  public static String toHex(byte...input){
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

  public static String toASCII(byte[] bytes) {
    StringBuffer buf = new StringBuffer();
    String prefix = "\n  ";
    buf.append(prefix);
    for (int i = 0; i < bytes.length; i++) {
      char ch = (char)bytes[i];

      if (isAsciiPrintable(ch)) {
        buf.append(ch);
      } else {
        buf.append(".");
      }

      if (i != 0 && i % 16 == 0) {
        buf.append(prefix);
      }
    }
    return buf.toString();
  }

  public static boolean isAsciiPrintable(final char ch) {
    return ch >= 32 && ch < 127;
  }

  public static byte[] espPacket1 = {
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

  public static byte[] espPacket2 = {
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
}