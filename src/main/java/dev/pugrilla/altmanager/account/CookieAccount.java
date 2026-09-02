package dev.pugrilla.altmanager.account;

import dev.pugrilla.altmanager.auth.Cookie;
import dev.pugrilla.altmanager.auth.CookieRedirectRequest;
import dev.pugrilla.altmanager.auth.CookieUtils;
import dev.pugrilla.altmanager.auth.MinecraftLoginRequest;
import dev.pugrilla.altmanager.auth.MinecraftLoginResponse;
import dev.pugrilla.altmanager.auth.MinecraftOwnershipRequest;
import dev.pugrilla.altmanager.auth.MinecraftOwnershipResponse;
import dev.pugrilla.altmanager.auth.MinecraftProfileRequest;
import dev.pugrilla.altmanager.auth.MinecraftProfileResponse;
import dev.pugrilla.altmanager.auth.NoMinecraftProfileException;
import dev.pugrilla.altmanager.auth.XboxLoginUrlRequest;
import dev.pugrilla.altmanager.auth.XboxTokenRequest;
import dev.pugrilla.altmanager.auth.XboxTokenResponse;
import dev.pugrilla.altmanager.network.HttpRequestException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Session;
public final class CookieAccount extends AbstractAccount {
   private String cookieHeader;

   public CookieAccount(String s, UUID uuid, String s1) {
      super(AccountType.COOKIE, s, uuid, s1);
   }

   public CookieAccount(String s) {
      this("UninitAcc", AbstractAccount.NIL_UUID, "");
      String[] astring = CookieUtils.splitLines(s);
      StringBuilder stringbuilder = new StringBuilder();

      for (int i = 0; i < astring.length; i++) {
         String s1 = astring[i];
         String[] astring1 = s1.split("\t");
         if (astring1.length >= 5) {
            String s2 = astring1[0];
            if (s2.endsWith("live.com")) {
               stringbuilder.append(s1);
               if (i < astring.length - 1) {
                  stringbuilder.append("\n");
               }
            }
         }
      }

      this.cookieHeader = stringbuilder.toString();
   }

   public CookieAccount(byte[] abyte) {
      this(new String(abyte, StandardCharsets.UTF_8));
   }

   public boolean isEmpty() {
      return this.cookieHeader.isEmpty();
   }

   @Override
   protected LoginResult loginImpl() {
      if (this.isEmpty()) {
         return new LoginResult(LoginResponseType.CORRUPT_DATA);
      }

      String s;
      try {
         s = new XboxLoginUrlRequest().send();
      } catch (HttpRequestException exception) {
         return new LoginResult(LoginResponseType.RATE_LIMITED);
      }

      List list = CookieUtils.parseCookieHeader(this.cookieHeader);
      StringBuilder stringbuilder = new StringBuilder();

      for (int i = 0; i < list.size(); i++) {
         Cookie Cookie = (Cookie)list.get(i);
         if (i == list.size() - 1) {
            stringbuilder.append(Cookie.toString().trim());
         } else {
            stringbuilder.append(Cookie.toString().trim()).append("; ");
         }
      }

      String s3 = stringbuilder.toString().trim();

      try {
         String s4 = new CookieRedirectRequest(s, s3).send();
         if (s4 == null) {
            return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
         }

         XboxTokenResponse XboxTokenResponse = new XboxTokenRequest(s4, s3).send();
         String s1 = XboxTokenResponse.getXbl();
         MinecraftLoginResponse MinecraftLoginResponse = new MinecraftLoginRequest(s1).send();
         String s2 = MinecraftLoginResponse.accessToken;
         if (s2 == null) {
            return new LoginResult(LoginResponseType.VALID_RATE_LIMITED);
         }

         try {
            MinecraftProfileResponse MinecraftProfileResponse = new MinecraftProfileRequest(s2).send();
            return new LoginResult(new Session(MinecraftProfileResponse.name, MinecraftProfileResponse.id, s2, "mojang"), LoginResponseType.SUCCESS);
         } catch (NoMinecraftProfileException NoMinecraftProfileException) {
            MinecraftOwnershipResponse MinecraftOwnershipResponse = new MinecraftOwnershipRequest(s2).send();
            return MinecraftOwnershipResponse.isOwned()
               ? new LoginResult(new Session(null, null, s2, "mojang"), LoginResponseType.NO_MINECRAFT_PROFILE)
               : new LoginResult(new Session(null, null, s2, "mojang"), LoginResponseType.DOES_NOT_OWN_MINECRAFT);
         }
      } catch (HttpRequestException HttpRequestException) {
         HttpRequestException.printStackTrace();
         return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
      }
   }

   @Override
   protected void deserialize(DataInputStream datainputstream) throws IOException {
      this.cookieHeader = datainputstream.readUTF();
   }

   @Override
   protected void serialize(DataOutputStream dataoutputstream) throws IOException {
      dataoutputstream.writeUTF(this.cookieHeader);
   }
}
