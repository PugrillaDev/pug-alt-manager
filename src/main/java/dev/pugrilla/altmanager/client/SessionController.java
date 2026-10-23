package dev.pugrilla.altmanager.client;

import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.LoginResult;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;

/** Requests and completions run on the client thread; only authentication runs on workers. */
public final class SessionController {
   private final Executor worker;
   private final Executor client;
   private final BiFunction<AbstractAccount, Boolean, LoginResult> prepare;
   private final BiConsumer<AbstractAccount, Boolean> activate;
   private final Predicate<AbstractAccount> available;
   private final Consumer<String> status;
   private final Consumer<Throwable> errorHandler;
   private long generation;
   private volatile AbstractAccount pending;

   public SessionController(AltManager manager) {
      this(manager.getThreadPool(), task -> Minecraft.getMinecraft().addScheduledTask(task),
         (account, force) -> account.prepareSession(manager.getStorageManager().isAutoRefreshSession(), force),
         AbstractAccount::setMinecraftSession,
         account -> account.getRepository() != null && account.getRepository().getEncryption().isDecrypted()
            && manager.getStorageManager().getRepositories().contains(account.getRepository())
            && manager.getStorageManager().getSelectedAccount() == account,
         message -> manager.getMainScreen().setStatus(message),
         failure -> manager.getLogger().warn("Account session operation failed ({})", failure.getClass().getSimpleName()));
   }

   SessionController(Executor worker, Executor client, BiFunction<AbstractAccount, Boolean, LoginResult> prepare,
      BiConsumer<AbstractAccount, Boolean> activate, Predicate<AbstractAccount> available,
      Consumer<String> status, Consumer<Throwable> errorHandler) {
      this.worker = worker;
      this.client = client;
      this.prepare = prepare;
      this.activate = activate;
      this.available = available;
      this.status = status;
      this.errorHandler = errorHandler;
   }

   public boolean isSwitching(AbstractAccount account) {
      return this.pending == account;
   }

   public boolean isSwitching() {
      return this.pending != null;
   }

   public void useAccount(AbstractAccount account, boolean markUsed, boolean forceRefresh) {
      this.submit(account, markUsed, () -> this.prepare.apply(account, forceRefresh), () -> true, "Using ");
   }

   public void refreshAccount(AbstractAccount account) {
      Minecraft minecraft = Minecraft.getMinecraft();
      net.minecraft.util.Session previous = minecraft.getSession();
      this.submit(account, false, account::login,
         () -> minecraft.getSession() == previous && previous.getProfile().getId().equals(account.getUuid()), "Refreshed session for ");
   }

   private void submit(AbstractAccount account, boolean markUsed, Supplier<LoginResult> operation,
      Supplier<Boolean> shouldActivate, String successMessage) {
      if (this.pending == account || !this.available.test(account)) {
         return;
      }
      long request = ++this.generation;
      AccountRepository owner = account.getRepository();
      this.pending = account;
      this.status.accept("Checking / refreshing session for " + account.getUsername() + "...");
      try {
         this.worker.execute(() -> {
            try {
               LoginResult result = operation.get();
               this.client.execute(() -> {
                  if (request != this.generation) {
                     return;
                  }
                  this.pending = null;
                  if (account.getRepository() != owner || !this.available.test(account)) {
                     this.status.accept("Account selection changed; session switch cancelled.");
                     return;
                  }
                  if (result.isSuccessfulLogin()) {
                     if (shouldActivate.get()) {
                        this.activate.accept(account, markUsed);
                     }
                     this.status.accept(EnumChatFormatting.GREEN + successMessage + account.getUsername());
                  } else {
                     this.status.accept(result.getResponseType().getDescription());
                  }
               });
            } catch (Exception failure) {
               this.client.execute(() -> this.fail(request, failure));
            }
         });
      } catch (RuntimeException failure) {
         this.fail(request, failure);
      }
   }

   private void fail(long request, Throwable failure) {
      this.errorHandler.accept(failure);
      if (request == this.generation) {
         this.pending = null;
         this.status.accept("Session check failed. Please try again.");
      }
   }
}
