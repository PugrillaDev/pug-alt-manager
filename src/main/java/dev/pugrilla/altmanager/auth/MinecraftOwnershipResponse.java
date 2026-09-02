package dev.pugrilla.altmanager.auth;

public class MinecraftOwnershipResponse {
   private final boolean owned;

   public MinecraftOwnershipResponse(boolean flag) {
      this.owned = flag;
   }

   public boolean isOwned() {
      return this.owned;
   }
}
