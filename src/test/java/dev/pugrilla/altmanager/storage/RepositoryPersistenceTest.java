package dev.pugrilla.altmanager.storage;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.SessionAccount;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class RepositoryPersistenceTest {
   private static final String PASSWORD = "test-password";
   private static final String ACCESS_TOKEN = "sensitive-minecraft-access-token";
   private static final UUID ACCOUNT_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

   @Test
   public void encryptedCurrentFormatProtectsAndRestoresAccessToken() throws Exception {
      byte[] serialized = writeRepository(createRepository(true), 1);

      assertFalse(containsPlaintextToken(serialized));

      AccountRepository restored = readRepository(serialized, 1);
      AbstractAccount account = restored.getAccountList().get(0);
      assertEquals("", account.getAccessToken());
      assertTrue(restored.getEncryption().tryDecryptWithPassword(PASSWORD));
      account.decryptIfWaitingPassword();
      assertEquals(ACCESS_TOKEN, account.getAccessToken());
   }

   @Test
   public void unencryptedCurrentFormatKeepsAccessTokenReadable() throws Exception {
      byte[] serialized = writeRepository(createRepository(false), 1);

      assertTrue(containsPlaintextToken(serialized));
      assertEquals(ACCESS_TOKEN, readRepository(serialized, 1).getAccountList().get(0).getAccessToken());
   }

   @Test
   public void legacyEncryptedRepositoryLoadsAndMigratesAfterUnlock() throws Exception {
      byte[] legacyBytes = writeRepository(createRepository(true), 0);
      AccountRepository legacyRepository = readRepository(legacyBytes, 0);
      AbstractAccount account = legacyRepository.getAccountList().get(0);

      assertEquals(ACCESS_TOKEN, account.getAccessToken());
      assertThrows(IOException.class, () -> writeRepository(legacyRepository, 1));

      assertTrue(legacyRepository.getEncryption().tryDecryptWithPassword(PASSWORD));
      account.decryptIfWaitingPassword();
      byte[] migratedBytes = writeRepository(legacyRepository, 1);

      assertFalse(containsPlaintextToken(migratedBytes));
      AccountRepository migratedRepository = readRepository(migratedBytes, 1);
      AbstractAccount migratedAccount = migratedRepository.getAccountList().get(0);
      assertTrue(migratedRepository.getEncryption().tryDecryptWithPassword(PASSWORD));
      migratedAccount.decryptIfWaitingPassword();
      assertEquals(ACCESS_TOKEN, migratedAccount.getAccessToken());
   }

   @Test
   public void lockingRepositoryClearsPasswordDerivedKey() {
      RepositoryEncryption encryption = new RepositoryEncryption(PASSWORD);

      encryption.lock();

      assertFalse(encryption.isDecrypted());
      assertThrows(RuntimeException.class, () -> encryption.encryptRaw(ACCESS_TOKEN.getBytes(StandardCharsets.UTF_8)));
   }

   private static AccountRepository createRepository(boolean encrypted) {
      RepositoryEncryption encryption = encrypted ? new RepositoryEncryption(PASSWORD) : new RepositoryEncryption(false);
      AccountRepository repository = new AccountRepository("Test Repository", null, 1L, encryption);
      repository.addAccount(new SessionAccount("TestUser", ACCOUNT_UUID, ACCESS_TOKEN));
      return repository;
   }

   private static byte[] writeRepository(AccountRepository repository, int version) throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      FileStorageManager.writeRepository(repository, new DataOutputStream(bytes), version);
      return bytes.toByteArray();
   }

   private static AccountRepository readRepository(byte[] bytes, int version) throws IOException {
      return FileStorageManager.readRepository(null, new DataInputStream(new ByteArrayInputStream(bytes)), version);
   }

   private static boolean containsPlaintextToken(byte[] bytes) {
      return new String(bytes, StandardCharsets.ISO_8859_1).contains(ACCESS_TOKEN);
   }
}
