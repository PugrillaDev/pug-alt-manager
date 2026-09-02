package dev.pugrilla.altmanager.account;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.UUID;
public final class SessionAccount extends AbstractAccount {
   public SessionAccount(String s, UUID uuid, String s1) {
      super(AccountType.SESSION, s, uuid, s1);
   }

   @Override
   public LoginResult loginImpl() {
      return new LoginResult(this.createSessionObject(), LoginResponseType.SUCCESS);
   }

   @Override
   protected void deserialize(DataInputStream datainputstream) {
   }

   @Override
   protected void serialize(DataOutputStream dataoutputstream) {
   }
}
