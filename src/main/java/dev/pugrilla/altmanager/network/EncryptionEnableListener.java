package dev.pugrilla.altmanager.network;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import javax.crypto.SecretKey;
final class EncryptionEnableListener implements GenericFutureListener<Future<? super Void>> {
   private final BanCheckNetworkManager networkManager;
   private final SecretKey secretKey;

   EncryptionEnableListener(BanCheckNetworkManager BanCheckNetworkManager, SecretKey secretkey) {
      this.networkManager = BanCheckNetworkManager;
      this.secretKey = secretkey;
   }

   public void operationComplete(Future<? super Void> future) throws Exception {
      this.networkManager.enableEncryption(this.secretKey);
   }
}
