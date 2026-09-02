package dev.pugrilla.altmanager.storage;


import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
public class RepositoryEncryption {
   public static final String ENCRYPTION_SENTINEL = "Encrypted";
   private final boolean enabled;
   private String encryptedTestString;
   private SecretKey key;

   public RepositoryEncryption(boolean flag) {
      this.enabled = flag;
   }

   public RepositoryEncryption(String s) {
      this(true);
      this.setKey(s);
      this.encryptedTestString = this.encrypt("Encrypted");
   }

   public void setTestString(String s) {
      this.encryptedTestString = s;
   }

   private void setKey(String s) {
      this.key = deriveKey(s);
   }
   private static SecretKeySpec deriveKey(String s) {
      try {
         Mac mac = Mac.getInstance("HmacSHA256");
         SecretKeySpec secretkeyspec = new SecretKeySpec(s.getBytes(StandardCharsets.UTF_16LE), "HmacSHA256");
         mac.init(secretkeyspec);
         byte[] abyte = mac.doFinal(s.getBytes());
         byte[] abyte1 = new byte[16];
         System.arraycopy(abyte, 0, abyte1, 0, 16);
         return new SecretKeySpec(abyte1, "AES");
      } catch (Throwable throwable) {
         throw new RuntimeException("Creating repository encryption key");
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isDecrypted() {
      return !this.enabled || this.key != null;
   }

   public String getTestString() {
      return this.encryptedTestString;
   }

   private String encrypt(String s) {
      if (!this.isEnabled()) {
         return s;
      }

      try {
         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(1, this.key);
         return Base64.getEncoder().encodeToString(cipher.doFinal(s.getBytes()));
      } catch (Throwable throwable) {
         throw new RuntimeException("Encrypting repository data", throwable);
      }
   }

   private String decrypt(String s) {
      if (!this.isEnabled()) {
         return s;
      }

      try {
         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(2, this.key);
         return new String(cipher.doFinal(Base64.getDecoder().decode(s.getBytes())));
      } catch (Throwable throwable) {
         throw new RuntimeException("Decrypting repository data", throwable);
      }
   }

   public boolean tryDecryptWithPassword(String s) {
      if (this.isDecrypted()) {
         return true;
      }

      try {
         SecretKey secretkey = this.key = deriveKey(s);
         String s1 = this.decrypt(this.encryptedTestString);
         this.key = null;
         if (s1.equals("Encrypted")) {
            this.key = secretkey;
            return true;
         } else {
            return false;
         }
      } catch (Throwable throwable) {
         this.key = null;
         return false;
      }
   }

   public void lock() {
      if (!this.isDecrypted() && this.isEnabled()) {
         this.key = null;
      }
   }

   public byte[] encryptRaw(byte[] abyte) {
      if (!this.isEnabled()) {
         return abyte;
      }

      if (!this.isDecrypted()) {
         throw new RuntimeException("Tried to encrypt with no password");
      }

      try {
         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(1, this.key);
         return cipher.doFinal(abyte);
      } catch (Throwable throwable) {
         throw new RuntimeException("Encrypting raw repository data", throwable);
      }
   }

   public byte[] decryptRaw(byte[] abyte) {
      if (!this.isEnabled()) {
         return abyte;
      }

      if (!this.isDecrypted()) {
         throw new RuntimeException("Tried to decrypt with no password");
      }

      try {
         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(2, this.key);
         return cipher.doFinal(abyte);
      } catch (Throwable throwable) {
         throw new RuntimeException("Decrypting raw repository data", throwable);
      }
   }
}
