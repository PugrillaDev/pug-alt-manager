package dev.pugrilla.altmanager.storage;

import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.SessionAccount;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class AltManagerStartupTest {
   @Test public void restoresSavedAccountsWhileManagerIsStillBeingConstructed() throws Exception {
      String isolatedAppData = System.getProperty("pugaltmanager.testAppData");
      assertNotNull("Run through Gradle with isolated account storage", isolatedAppData);
      assertEquals(new File(isolatedAppData).getCanonicalPath(), new File(System.getenv("APPDATA")).getCanonicalPath());
      File directory = new File(isolatedAppData, ".pugaltmanager");
      FileStorageManager fixture = new FileStorageManager(null, directory, false);
      fixture.deleteAllRepositories();
      AccountRepository repository = new AccountRepository("Startup fixture", null, 1L, new RepositoryEncryption(false));
      AbstractAccount first = new SessionAccount("First", UUID.fromString("12345678-1234-1234-1234-123456789abc"), "fixture-token-first");
      AbstractAccount selected = new SessionAccount("Selected", UUID.fromString("12345678-1234-1234-1234-123456789abd"), "fixture-token-selected");
      repository.addAccount(first);
      repository.addAccount(selected);
      repository.setSelectedAccount(selected);
      fixture.createRepository(repository);
      fixture.setSelectedRepository(repository);
      fixture.save();
      byte[] databaseBeforeStartup = Files.readAllBytes(new File(directory, "accounts.dat").toPath());

      AltManager manager = new AltManager();
      FileStorageManager restored = manager.getStorageManager();
      assertEquals(1, restored.getRepositoryCount());
      assertEquals(2, restored.getSelectedRepository().getAccountCount());
      assertEquals("Selected", restored.getSelectedAccount().getUsername());
      assertEquals("fixture-token-selected", restored.getSelectedAccount().getAccessToken());
      assertArrayEquals(databaseBeforeStartup, Files.readAllBytes(new File(directory, "accounts.dat").toPath()));
      assertEquals(0, directory.listFiles((parent, name) -> name.startsWith("accounts.dat.corrupt-")).length);
      manager.getThreadPool().shutdownNow();
      manager.getSkinThreadPool().shutdownNow();
   }
}
