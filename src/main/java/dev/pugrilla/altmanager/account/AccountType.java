package dev.pugrilla.altmanager.account;

import dev.pugrilla.altmanager.auth.Cookie;
public enum AccountType {
   COOKIE("Cookie", CookieAccount::new),
   CREDENTIALS("Credentials", CredentialsAccount::new),
   SESSION("Session", SessionAccount::new),
   BROWSER("Browser", BrowserAccount::new);
   private final String displayName;
   private final AccountFactory factory;

   AccountType(String s1, AccountFactory AccountFactory) {
      this.displayName = s1;
      this.factory = AccountFactory;
   }

   public AccountFactory getSupplier() {
      return this.factory;
   }

   public String getName() {
      return this.displayName;
   }

   @Override
   public String toString() {
      return this.getName();
   }
}
