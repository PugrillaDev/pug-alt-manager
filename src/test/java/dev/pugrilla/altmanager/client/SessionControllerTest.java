package dev.pugrilla.altmanager.client;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.LoginResult;
import dev.pugrilla.altmanager.account.LoginResponseType;
import dev.pugrilla.altmanager.account.SessionAccount;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionControllerTest {
   private final List<Runnable> workers = new ArrayList<>();
   private final List<Runnable> clients = new ArrayList<>();
   private final List<AbstractAccount> activated = new ArrayList<>();
   private final Set<AbstractAccount> available = new HashSet<>();
   private final List<String> messages = new ArrayList<>();
   private final SessionController controller = new SessionController(workers::add, clients::add,
      (account, force) -> new LoginResult(account.createSessionObject(), LoginResponseType.SUCCESS),
      (account, used) -> activated.add(account), available::contains, messages::add, failure -> fail(failure.toString()));

   private AbstractAccount account(String name) {
      AbstractAccount account = new SessionAccount(name, UUID.randomUUID(), "test-token");
      available.add(account);
      return account;
   }

   @Test public void repeatedClicksApplyOnceAndWaitForClientThreadCompletion() {
      AbstractAccount account = account("A");
      controller.useAccount(account, true, false);
      controller.useAccount(account, true, false);
      assertEquals(1, workers.size());
      assertTrue(activated.isEmpty());
      workers.get(0).run();
      assertTrue(activated.isEmpty());
      clients.get(0).run();
      assertEquals(1, activated.size());
      assertFalse(controller.isSwitching());
   }

   @Test public void slowOldRefreshCannotReplaceMoreRecentAccountSwitch() {
      AbstractAccount first = account("A");
      AbstractAccount second = account("B");
      controller.useAccount(first, true, false);
      controller.useAccount(second, true, false);
      workers.get(1).run();
      clients.get(0).run();
      workers.get(0).run();
      clients.get(1).run();
      assertEquals(1, activated.size());
      assertSame(second, activated.get(0));
   }

   @Test public void deletedAccountCannotActivateWhenRefreshFinishes() {
      AbstractAccount account = account("A");
      controller.useAccount(account, true, false);
      available.remove(account);
      workers.get(0).run();
      clients.get(0).run();
      assertTrue(activated.isEmpty());
      assertFalse(controller.isSwitching());
   }

   @Test public void failedAuthenticationLeavesActiveSessionUntouched() {
      AbstractAccount account = account("A");
      SessionController failed = new SessionController(workers::add, clients::add,
         (target, force) -> new LoginResult(LoginResponseType.INVALID_ACCOUNT),
         (target, used) -> activated.add(target), available::contains, messages::add, failure -> fail());
      failed.useAccount(account, true, false);
      workers.get(0).run();
      clients.get(0).run();
      assertTrue(activated.isEmpty());
      assertFalse(failed.isSwitching());
      assertEquals(LoginResponseType.INVALID_ACCOUNT.getDescription(), messages.get(messages.size() - 1));
   }

   @Test public void rejectedWorkerSchedulingClearsBusyState() {
      AbstractAccount account = account("A");
      List<Throwable> failures = new ArrayList<>();
      SessionController rejected = new SessionController(task -> { throw new java.util.concurrent.RejectedExecutionException(); },
         clients::add, (target, force) -> new LoginResult(LoginResponseType.SUCCESS),
         (target, used) -> activated.add(target), available::contains, messages::add, failures::add);
      rejected.useAccount(account, true, false);
      assertFalse(rejected.isSwitching());
      assertTrue(activated.isEmpty());
      assertEquals(1, failures.size());
   }

   @Test public void accountMovedDuringRefreshCannotActivateFromStaleRequest() {
      AbstractAccount account = account("A");
      controller.useAccount(account, true, false);
      dev.pugrilla.altmanager.account.AccountRepository destination = new dev.pugrilla.altmanager.account.AccountRepository(
         "Destination", null, 1L, new dev.pugrilla.altmanager.storage.RepositoryEncryption(false));
      destination.addAccount(account);
      workers.get(0).run();
      clients.get(0).run();
      assertTrue(activated.isEmpty());
   }
}
