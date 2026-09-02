package dev.pugrilla.altmanager.auth;

public class XboxTokenResponse {
   private final String xboxIdentityToken;

   public XboxTokenResponse(String s) {
      this.xboxIdentityToken = s;
   }

   public String getXbl() {
      return this.xboxIdentityToken;
   }
}
