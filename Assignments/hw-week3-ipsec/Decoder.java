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

	public static void main(String[] args) {
    test2();

	}

  public static void test2() {
    System.out.println("Packet: " + convertToHex(espPacket1));

    int espStartPos = 68;
    byte[] spi = Arrays.copyOfRange(espPacket1, espStartPos, espStartPos+8);
    System.out.println("SPI: " + convertToHex(spi));

    byte[] seqNo = Arrays.copyOfRange(espPacket1, espStartPos+8, espStartPos+16);
    System.out.println("Sequence Number: " + convertToHex(seqNo));

    // IV for AES must be 16 bytes long.
    // Assume that IV is comes after the Sequence number.
    byte[] iv = Arrays.copyOfRange(espPacket1, espStartPos+16, espStartPos+16+AES_BLOCK_SIZE);
    System.out.println("IV: " + convertToHex(iv));

    int espPayLoadDataPos = espStartPos+16;

    byte[] key = "YELLOW SUBMARINE".getBytes();

    int len = espPacket1.length - espPayLoadDataPos - 8 - MD5_RESULT_SIZE;
    tryDecrypt(key, iv, espPayLoadDataPos, len);

    //while(true) {
    //  if (len < 32) break;
    //  tryDecrypt(key, iv, espPayLoadDataPos, len);
    //  len -= 8;
    //}
  }

  public static boolean tryDecrypt(byte[] key, byte[] iv, int espPayLoadDataPos, int len) {
    System.out.println("\n\nTry with length: " + len);
    try {
      // Create cipher aes in cbc mode without padding
      Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
      
      // Initiate the cipher in decryption mode with the correct iv and key
      SecretKeySpec secretKeySpecy = new SecretKeySpec(key, "AES");
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      aes.init(Cipher.DECRYPT_MODE, secretKeySpecy, ivParameterSpec);


      byte[] cipherText = Arrays.copyOfRange(espPacket1, espPayLoadDataPos, espPayLoadDataPos+len);

      System.out.println("Encrypted: " + convertToHex(cipherText));


      byte[] decryptedPlaintext = aes.doFinal(cipherText);
      //System.out.println("Decrypted text: " + new String(decryptedPlaintext));
      System.out.println("Decrypted text: " + convertToHex(decryptedPlaintext));
      return true;
      
    } catch (Exception e) {
      System.out.println("Failed to encrypt:");
      e.printStackTrace();
      return false;
    }
  }

	public static String convertToHex(byte[] bytes){
		String result = "";
    for (int i = 0; i < bytes.length-1; i+=2) {
      byte byte1 = bytes[i];
      byte byte2 = bytes[i+1];
			result += String.format("%x%x ", byte1, byte2);
		}
		return result;
	}

  public static String toHex(byte...input){
    String result = "";
    for(byte b:input){
      result += String.format("%02x ", b);
    }
    return result;
  }

  public static byte[] espPacket1 = { 
    0x0a, 0x0a, 0x0b, 0x0b, 0x0c, 0x0c, 0x0d, 0x0d, 0x0e, 0x0e, 0x00, 0x03, 0x0a, 0x0a, 0x0b, 0x0b, 0x0c, 0x0c, 0x0d, 0x0d, 0x0e, 0x0e, 0x00, 0x02, 0x00, 0x08, 0x00, 0x00, 0x04, 0x05, 0x00, 0x00, 0x00, 0x00, 0x07, 0x08, 0x00, 0x00, 0x04, 0x02, 0x04, 0x00, 0x00, 0x00, 0x04, 0x02, 0x03, 0x02, 0x02, 0x00, 0x00, 0x0e, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x02, 0x00, 0x03, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x02, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x03, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x0e, 0x0b, 0x0c, 0x02, 0x01, 0x06, 0x07, 0x0c, 0x0d, 0x04, 0x0c, 0x0d, 0x01, 0x0d, 0x04, 0x0a, 0x0c, 0x04, 0x07, 0x03, 0x0f, 0x09, 0x0e, 0x0d, 0x0d, 0x07, 0x01, 0x06, 0x07, 0x09, 0x03, 0x0c, 0x0c, 0x01, 0x09, 0x0f, 0x0e, 0x08, 0x00, 0x00, 0x0d, 0x05, 0x07, 0x05, 0x00, 0x0f, 0x02, 0x06, 0x04, 0x09, 0x0e, 0x0f, 0x0e, 0x0a, 0x0c, 0x0a, 0x0f, 0x0e, 0x06, 0x01, 0x0a, 0x09, 0x0e, 0x05, 0x0e, 0x0b, 0x06, 0x04, 0x07, 0x05, 0x0c, 0x06, 0x0e, 0x00, 0x07, 0x07, 0x07, 0x04, 0x04, 0x0f, 0x02, 0x04, 0x06, 0x0f, 0x02, 0x07, 0x04, 0x03, 0x0d, 0x0c, 0x04, 0x0b, 0x0c, 0x09, 0x01, 0x03, 0x08, 0x0c, 0x06, 0x07, 0x01, 0x07, 0x00, 0x0e, 0x04, 0x0f, 0x0b, 0x00, 0x07, 0x08, 0x0e, 0x0a, 0x06, 0x08, 0x01, 0x06, 0x05, 0x0a, 0x0d, 0x0d, 0x0d, 0x00, 0x0a, 0x04, 0x0e, 0x03, 0x0c, 0x01, 0x01, 0x05, 0x09, 0x07, 0x05, 0x04, 0x08, 0x02, 0x00, 0x0f, 0x06, 0x00, 0x0f, 0x05, 0x0d, 0x0f, 0x0f, 0x02, 0x08, 0x03, 0x03, 0x02, 0x07, 0x09, 0x0e, 0x03, 0x0d, 0x04, 0x0d, 0x01, 0x05, 0x03, 0x0f, 0x0d, 0x08, 0x0f, 0x05, 0x0c, 0x09, 0x01, 0x04, 0x0d, 0x07, 0x03, 0x03, 0x06, 0x0d, 0x0b, 0x04, 0x07, 0x02, 0x03, 0x04, 0x0f, 0x06

  };
}
