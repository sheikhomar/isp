import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

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

public class Example {
	public static void main(String[] args) {
		// Using bouncycastle as crypto library, bouncycaslte contains more ciphers than plain java.
		// This is not nessesary for this example but for later use it can by useful.
		// Bouncycastle can be obtained at: https://www.bouncycastle.org/latest_releases.html
		Security.addProvider(new BouncyCastleProvider());

		
		// Define all byte arrays that are going to be used.
		byte[] plaintext = "abcdefghijklmnopabcdefghijklmnop".getBytes();
		byte[] keyBytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
		byte[] ivBytes = {0x0f, 0x0e, 0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00};
		byte[] ciphertext;
		byte[] decryptedPlaintext;
		
		// Prepare key object
		Key key = new SecretKeySpec(keyBytes, "AES");
		// Prepare IV object
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		
		try {
			// Create cipher aes in cbc mode without padding
			Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
			// Initiate the cipher in encryption mode with the correct iv and key
			aes.init(Cipher.ENCRYPT_MODE, key, iv);
			ciphertext = aes.doFinal(plaintext);
			System.out.println("Ciphertext: " + toHex(ciphertext));
			
			// Initiate the cipher in decryption mode with the correct iv and key
			aes.init(Cipher.DECRYPT_MODE, key, iv);
			decryptedPlaintext = aes.doFinal(ciphertext);
			System.out.println("Decrypted text: " + new String(decryptedPlaintext));
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			System.err.println("Failed to encrypt:");
			e.printStackTrace();
		}
		
	}
	
	public static String toHex(byte...input){
		String result = "";
		for(byte b:input){
			result += String.format("%02x ", b);
		}
		return result;
	}
}
