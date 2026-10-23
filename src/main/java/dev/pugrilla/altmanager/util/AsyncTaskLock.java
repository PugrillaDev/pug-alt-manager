package dev.pugrilla.altmanager.util;

import dev.pugrilla.altmanager.AltManager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
public final class AsyncTaskLock {
   private final AtomicInteger activeTaskCount = new AtomicInteger(0);
   private final AltManager altManager;
   private final Consumer<Throwable> errorHandler;

   public AsyncTaskLock(AltManager altmanager, Consumer<Throwable> consumer) {
      this.altManager = altmanager;
      this.errorHandler = consumer;
   }

   public void execute(Runnable runnable) {
      this.activeTaskCount.incrementAndGet();
      try {
         this.altManager.getThreadPool().execute(() -> {
            try {
               runnable.run();
            } catch (Throwable failure) {
               this.handleFailure(failure);
            } finally {
               this.activeTaskCount.decrementAndGet();
            }
         });
      } catch (RuntimeException failure) {
         this.activeTaskCount.decrementAndGet();
         this.handleFailure(failure);
      }
   }

   private void handleFailure(Throwable failure) {
      this.altManager.getLogger().error("Exception inside lock service", failure);
      try {
         this.errorHandler.accept(failure);
      } catch (Throwable handlerFailure) {
         this.altManager.getLogger().error("Exception inside lock service error handler", handlerFailure);
      }
   }

   private void verifyValue(int i) {
      if (i < 0) {
         throw new RuntimeException("Execution lock service went below zero: " + i);
      }
   }

   public boolean isLocked() {
      return this.activeTaskCount.get() != 0;
   }
}
