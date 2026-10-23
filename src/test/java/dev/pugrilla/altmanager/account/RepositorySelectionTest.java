package dev.pugrilla.altmanager.account;

import dev.pugrilla.altmanager.storage.RepositoryEncryption;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class RepositorySelectionTest {
   @Test public void deletingSelectedAccountSelectsExistingAccountThenClearsSelection() {
      AccountRepository repository = new AccountRepository("Test", null, 1L, new RepositoryEncryption(false));
      AbstractAccount first = new SessionAccount("A", UUID.randomUUID(), "a");
      AbstractAccount second = new SessionAccount("B", UUID.randomUUID(), "b");
      repository.addAccount(first);
      repository.addAccount(second);
      repository.deleteAccount(first);
      assertSame(second, repository.getSelectedAccount());
      assertNull(first.getRepository());
      repository.deleteAccount(second);
      assertNull(repository.getSelectedAccount());
   }
}
