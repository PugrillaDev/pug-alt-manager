package dev.pugrilla.altmanager.account;


import net.minecraft.util.Session;
public final class LoginResult {
   private final Session session;
   private final LoginResponseType responseType;

   public LoginResult(Session session, LoginResponseType LoginResponseType) {
      this.session = session;
      this.responseType = LoginResponseType;
   }

   public LoginResult(LoginResponseType LoginResponseType) {
      this(null, LoginResponseType);
   }

   public Session getSession() {
      return this.session;
   }

   public LoginResponseType getResponseType() {
      return this.responseType;
   }

   public boolean isSuccessfulLogin() {
      return this.responseType == LoginResponseType.SUCCESS;
   }
}
