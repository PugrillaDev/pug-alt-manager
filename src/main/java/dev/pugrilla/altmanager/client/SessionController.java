package dev.pugrilla.altmanager.client;

import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.LoginResult;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

/** Requests and completions run on the client thread; only authentication runs on workers. */
public final class SessionController {
   public static final class Status {
      private final String message;
      private final boolean error;
      private final boolean pending;
      private final long expiresAt;

      private Status(String message, boolean error, boolean pending) {
         this.message = message;
         this.error = error;
         this.pending = pending;
         this.expiresAt = pending ? 0L : System.currentTimeMillis() + 10000L;
      }

      public String getMessage() { return this.message; }
      public boolean isError() { return this.error; }
      public boolean isPending() { return this.pending; }
   }

   private final Executor worker;
   private final Executor client;
   private final BiFunction<AbstractAccount, Boolean, LoginResult> prepare;
   private final BiConsumer<AbstractAccount, Boolean> activate;
   private final Predicate<AbstractAccount> available;
   private final Predicate<AbstractAccount> active;
   private final Consumer<String> statusObserver;
   private final Consumer<Throwable> errorHandler;
   private final Map<AbstractAccount, Status> statuses = new IdentityHashMap<>();
   private long generation;
   private volatile AbstractAccount pending;

   public SessionController(AltManager manager) {
      this(manager.getThreadPool(), task -> Minecraft.getMinecraft().addScheduledTask(task),
         (account, force) -> account.prepareSession(manager.getStorageManager().isAutoRefreshSession(), force),
         AbstractAccount::setMinecraftSession,
         account -> account.getRepository() != null && account.getRepository().getEncryption().isDecrypted()
            && manager.getStorageManager().getRepositories().contains(account.getRepository()),
         account -> matchesSession(account, Minecraft.getMinecraft().getSession()),
         message -> { },
         failure -> manager.getLogger().warn("Account session operation failed ({})", failure.getClass().getSimpleName()));
   }

   SessionController(Executor worker, Executor client, BiFunction<AbstractAccount, Boolean, LoginResult> prepare,
      BiConsumer<AbstractAccount, Boolean> activate, Predicate<AbstractAccount> available,
      Consumer<String> observer, Consumer<Throwable> errorHandler) {
      this(worker, client, prepare, activate, available, account -> false, observer, errorHandler);
   }

   SessionController(Executor worker, Executor client, BiFunction<AbstractAccount, Boolean, LoginResult> prepare,
      BiConsumer<AbstractAccount, Boolean> activate, Predicate<AbstractAccount> available,
      Predicate<AbstractAccount> active, Consumer<String> observer, Consumer<Throwable> errorHandler) {
      this.worker = worker;
      this.client = client;
      this.prepare = prepare;
      this.activate = activate;
      this.available = available;
      this.active = active;
      this.statusObserver = observer;
      this.errorHandler = errorHandler;
   }

   public static boolean matchesSession(AbstractAccount account, Session session) {
      return session != null && account.getUuid() != null
         && (account.getUuid().getMostSignificantBits() != 0L || account.getUuid().getLeastSignificantBits() != 0L)
         && account.getUuid().equals(session.getProfile().getId());
   }

   public boolean isActive(AbstractAccount account) {
      return this.active.test(account);
   }

   public Status getStatus(AbstractAccount account) {
      Status result = this.statuses.get(account);
      if (result != null && result.expiresAt != 0L && System.currentTimeMillis() >= result.expiresAt) {
         this.statuses.remove(account);
         return null;
      }
      return result;
   }

   public boolean isSwitching(AbstractAccount account) { return this.pending == account; }
   public boolean isSwitching() { return this.pending != null; }

   public void useAccount(AbstractAccount account, boolean markUsed, boolean forceRefresh) {
      this.submit(account, markUsed, () -> this.prepare.apply(account, forceRefresh), () -> true,
         forceRefresh ? "Refreshing session..." : "Checking session...");
   }

   public void refreshAccount(AbstractAccount account) {
      Minecraft minecraft = Minecraft.getMinecraft();
      Session previous = minecraft.getSession();
      this.submit(account, false, account::login,
         () -> minecraft.getSession() == previous && matchesSession(account, previous), "Refreshing session...");
   }

   private void setStatus(AbstractAccount account, String message, boolean error, boolean pending) {
      this.statuses.put(account, new Status(message, error, pending));
      this.statusObserver.accept(message);
   }

   private void submit(AbstractAccount account, boolean markUsed, Supplier<LoginResult> operation,
      Supplier<Boolean> shouldActivate, String progress) {
      if (this.pending == account || !this.available.test(account)) {
         return;
      }
      if (this.pending != null) {
         this.statuses.remove(this.pending);
      }
      long request = ++this.generation;
      AccountRepository owner = account.getRepository();
      this.pending = account;
      this.setStatus(account, progress, false, true);
      try {
         this.worker.execute(() -> {
            try {
               LoginResult result = operation.get();
               this.client.execute(() -> {
                  if (request != this.generation) {
                     return;
                  }
                  this.pending = null;
                  try {
                     if (account.getRepository() != owner || !this.available.test(account)) {
                        this.setStatus(account, "Session switch cancelled.", true, false);
                     } else if (result.isSuccessfulLogin()) {
                        if (shouldActivate.get()) {
                           this.activate.accept(account, markUsed);
                           this.statuses.remove(account);
                        } else {
                           this.setStatus(account, "Session refreshed", false, false);
                        }
                     } else {
                        this.setStatus(account, result.getResponseType().getDescription(), true, false);
                     }
                  } catch (RuntimeException failure) {
                     this.fail(request, account, failure);
                  }
               });
            } catch (Exception failure) {
               this.client.execute(() -> this.fail(request, account, failure));
            }
         });
      } catch (RuntimeException failure) {
         this.fail(request, account, failure);
      }
   }

   private void fail(long request, AbstractAccount account, Throwable failure) {
      this.errorHandler.accept(failure);
      if (request == this.generation) {
         this.pending = null;
         this.setStatus(account, "Session check failed. Please try again.", true, false);
      }
   }
}
