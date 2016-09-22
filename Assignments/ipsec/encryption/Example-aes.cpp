/*
 * Compiling: g++ Example-aes.cpp -lssl -lcrypto -o Example-aes
 * Running: ./Example-aes
 * This example requires libssl and libcrypto
 *
 *  In this example we encrypt "abcdefghijklmnopabcdefghijklmnop" with key:
 *   [0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f]
 *   and iv:
 *   [0x0f, 0x0e, 0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00]
 *   using aes in cbc mode without padding.
 *  Than we decrypt the obtained ciphertext.
 */

#include <stdio.h>
#include <iostream>
#include <openssl/aes.h>

using namespace std;

void printHex(unsigned char* input, size_t length){
  for(size_t i=0;i<length;++i) printf("%02x ", input[i]);
}

int main(){
  // Define all unsigned char arrays that are going to be used.
  // Note that calling AES_cbc_encrypt overwrites the IV. Therefore we declare an IV for encryption as well as decryption
  unsigned char keyBytes[] = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
  unsigned char iv_enc[] = {0x0f, 0x0e, 0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00};
  unsigned char iv_dec[] = {0x0f, 0x0e, 0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00};
  unsigned char plaintext[] = "abcdefghijklmnopabcdefghijklmnop";
  unsigned char ciphertext[sizeof(plaintext) - 1]; // Don't include null terminator
  unsigned char decryptedPlaintext[sizeof(plaintext)];

  AES_KEY key;
  // Prepare key for encryption
  AES_set_encrypt_key(keyBytes, sizeof(keyBytes)*8, &key);
  // Encrypt with aes in cbc mode (no padding used)
  AES_cbc_encrypt(plaintext, ciphertext, sizeof(ciphertext), &key, iv_enc, AES_ENCRYPT);

  cout << "Ciphertext: ";
  printHex(ciphertext, sizeof(ciphertext));
  cout << endl;

  // Prepare key for decryption
  AES_set_decrypt_key(keyBytes, sizeof(keyBytes)*8, &key);
  // Decrypt with aes in cbc mode (no padding used)
  AES_cbc_encrypt(ciphertext, decryptedPlaintext, sizeof(ciphertext), &key, iv_dec, AES_DECRYPT);

  decryptedPlaintext[sizeof(ciphertext)] = 0x0;
  cout << "Decrypted text: " << decryptedPlaintext << endl;
}

