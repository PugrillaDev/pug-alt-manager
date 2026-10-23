package dev.pugrilla.altmanager.storage;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.SessionAccount;
import java.io.File;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LastUsedAccountTest {
   @Rule public TemporaryFolder temporary = new TemporaryFolder();

   private AbstractAccount account(String name, long used) {
      AbstractAccount account = new SessionAccount(name, UUID.randomUUID(), "fixture-token");
      account.setLastUsed(used);
      return account;
   }

   private AccountRepository repository(FileStorageManager storage, String name) {
      AccountRepository repository = new AccountRepository(name, null, 1L, new RepositoryEncryption(false));
      storage.createRepository(repository);
      return repository;
   }

   @Test public void startupChoosesLastActualUseAcrossRepositoriesAfterRestart() throws Exception {
      File directory = temporary.newFolder();
      FileStorageManager storage = new FileStorageManager(null, directory, false);
      AccountRepository old = repository(storage, "Old");
      AbstractAccount earlier = account("Earlier", 100L);
      AbstractAccount inspected = account("OnlyBrowsed", 0L);
      old.addAccount(earlier);
      old.addAccount(inspected);
      old.setSelectedAccount(inspected);
      AccountRepository recent = repository(storage, "Recent");
      AbstractAccount latest = account("LastUsed", 200L);
      recent.addAccount(latest);
      storage.setSelectedRepository(old);
      storage.save();

      FileStorageManager restored = new FileStorageManager(null, directory, false);
      assertEquals("OnlyBrowsed", restored.getSelectedAccount().getUsername());
      assertEquals("LastUsed", restored.getLastUsedAccount().getUsername());
      assertEquals(200L, restored.getLastUsedAccount().getLastUsed());
      assertEquals("Recent", restored.getLastUsedAccount().getRepository().getName());
   }

   @Test public void neverUsedAccountsDoNotBecomeStartupFallbacks() throws Exception {
      FileStorageManager storage = new FileStorageManager(null, temporary.newFolder(), false);
      AccountRepository repository = repository(storage, "Never used");
      repository.addAccount(account("NeverUsed", 0L));
      storage.setSelectedRepository(repository);
      assertNotNull(storage.getSelectedAccount());
      assertNull(storage.getLastUsedAccount());
   }

   @Test public void lockedLastUsedAccountDoesNotChooseAnOlderUnrelatedAccount() throws Exception {
      File directory = temporary.newFolder();
      FileStorageManager storage = new FileStorageManager(null, directory, false);
      AccountRepository unlocked = repository(storage, "Unlocked");
      unlocked.addAccount(account("Older", 100L));
      AccountRepository locked = new AccountRepository("Locked", null, 1L, new RepositoryEncryption("fixture-password"));
      locked.addAccount(account("Latest", 200L));
      storage.createRepository(locked);
      storage.setSelectedRepository(unlocked);
      storage.save();
      FileStorageManager restored = new FileStorageManager(null, directory, false);
      assertEquals("Latest", restored.getLastUsedAccount().getUsername());
      assertFalse(restored.getLastUsedAccount().getRepository().getEncryption().isDecrypted());
   }

   @Test public void changingSelectionDoesNotChangeLastUsedHistory() throws Exception {
      FileStorageManager storage = new FileStorageManager(null, temporary.newFolder(), false);
      AccountRepository repository = repository(storage, "Accounts");
      AbstractAccount used = account("Used", 200L);
      AbstractAccount browsed = account("Browsed", 0L);
      repository.addAccount(used);
      repository.addAccount(browsed);
      storage.setSelectedRepository(repository);
      repository.setSelectedAccount(browsed);
      assertSame(used, storage.getLastUsedAccount());
      assertEquals(0L, browsed.getLastUsed());
   }
}
