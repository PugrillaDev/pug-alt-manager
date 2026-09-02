package dev.pugrilla.altmanager.account;

import dev.pugrilla.altmanager.auth.MinecraftProfileDto;
import dev.pugrilla.altmanager.auth.MinecraftServicesApi;
import dev.pugrilla.altmanager.util.AltManagerUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import net.minecraft.util.Session;
import org.apache.commons.io.IOUtils;
public final class CredentialsAccount extends AbstractAccount {
   private String email;
   private String password;

   public CredentialsAccount(String s, UUID uuid, String s1) {
      super(AccountType.CREDENTIALS, s, uuid, s1);
   }

   public CredentialsAccount(String s, String s1) {
      this("UninitAcc", AbstractAccount.NIL_UUID, "");
      this.email = s;
      this.password = s1;
   }

   public String getEmail() {
      return this.email;
   }

   public String getPassword() {
      return this.password;
   }

   public void setEmail(String s) {
      this.email = s;
   }

   public void setPassword(String s) {
      this.password = s;
   }

   @Override
   public LoginResult loginImpl() {
      return loginWithCredentials(this.getEmail(), this.getPassword(), false);
   }

   @Override
   protected void deserialize(DataInputStream datainputstream) throws IOException {
      this.setEmail(datainputstream.readUTF());
      this.setPassword(datainputstream.readUTF());
   }

   @Override
   protected void serialize(DataOutputStream dataoutputstream) throws IOException {
      dataoutputstream.writeUTF(this.getEmail());
      dataoutputstream.writeUTF(this.getPassword());
   }
   private static LoginResult loginWithCredentials(String s, String s1, boolean flag) {
      CookieHandler cookiehandler = CookieHandler.getDefault();
      CookieManager cookiemanager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
      CookieHandler.setDefault(cookiemanager);

      try {
         String s2 = "Mozilla/5.0 (XboxReplay; XboxLiveAuth/3.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create(
               "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&display=touch&response_type=token&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf&locale=en&client_id=000000004C12AE6F"
            )
            .toURL()
            .openConnection();
         httpsurlconnection.setRequestProperty("User-Agent", s2);
         httpsurlconnection.setConnectTimeout(5000);
         httpsurlconnection.setReadTimeout(5000);
         httpsurlconnection.connect();
         String s3 = IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8);
         String s4 = AltManagerUtils.extractFirstRegexGroup("sFTTag:'.*value=\"([^\"]*)\"", s3);
         String s5 = AltManagerUtils.extractFirstRegexGroup("urlPost: ?'(.+?(?='))", s3);
         if (s4 != null && s5 != null) {
            String s6 = URLEncoder.encode(s, "UTF-8");
            String s7 = URLEncoder.encode(s1, "UTF-8");
            String s8 = "loginfmt=" + s6 + "&passwd=" + s7 + "&PPFT=" + URLEncoder.encode(s4, "UTF-8") + "&login=" + s6;
            httpsurlconnection = (HttpsURLConnection)URI.create(s5).toURL().openConnection();
            httpsurlconnection.setRequestMethod("POST");
            httpsurlconnection.setRequestProperty("User-Agent", s2);
            httpsurlconnection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsurlconnection.addRequestProperty("Accept", "*/*");
            httpsurlconnection.setDoOutput(true);
            httpsurlconnection.getOutputStream().write(s8.getBytes(StandardCharsets.UTF_8));
            httpsurlconnection.setInstanceFollowRedirects(false);
            httpsurlconnection.setConnectTimeout(5000);
            httpsurlconnection.setReadTimeout(5000);
            httpsurlconnection.connect();
            String s9 = httpsurlconnection.getHeaderField("Location");
            if (s9 == null) {
               return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
            }

            s3 = IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8);
            String s10 = s3.toLowerCase();
            if (s10.contains("abuse")) {
               return new LoginResult(LoginResponseType.ABUSE_LOCKED);
            }

            if (s10.contains("recover")) {
               return new LoginResult(LoginResponseType.RECOVERY_LOCKED);
            }

            if (!s3.contains("proofs/Add")) {
               CookieHandler.setDefault(cookiehandler);
               String s19 = AltManagerUtils.extractFirstRegexGroup("access_token=([^&]*)", s9);
               if (s19 == null) {
                  return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
               }

               String s20 = URLDecoder.decode(s19, "UTF-8");
               s8 = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\""
                  + s20
                  + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
               httpsurlconnection = (HttpsURLConnection)URI.create("https://user.auth.xboxlive.com/user/authenticate").toURL().openConnection();
               httpsurlconnection.setRequestMethod("POST");
               httpsurlconnection.setRequestProperty("User-Agent", s2);
               httpsurlconnection.addRequestProperty("Content-Type", "application/json");
               httpsurlconnection.addRequestProperty("Accept", "*/*");
               httpsurlconnection.setDoOutput(true);
               httpsurlconnection.getOutputStream().write(s8.getBytes(StandardCharsets.UTF_8));
               httpsurlconnection.setConnectTimeout(5000);
               httpsurlconnection.setReadTimeout(5000);
               httpsurlconnection.connect();
               Gson gson = new Gson();
               String s14 = ((JsonObject)gson.fromJson(IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8), JsonObject.class))
                  .get("Token")
                  .getAsString();
               s8 = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""
                  + s14
                  + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
               httpsurlconnection = (HttpsURLConnection)URI.create("https://xsts.auth.xboxlive.com/xsts/authorize").toURL().openConnection();
               httpsurlconnection.setRequestMethod("POST");
               httpsurlconnection.setRequestProperty("User-Agent", s2);
               httpsurlconnection.addRequestProperty("Content-Type", "application/json");
               httpsurlconnection.addRequestProperty("Accept", "*/*");
               httpsurlconnection.setDoOutput(true);
               httpsurlconnection.getOutputStream().write(s8.getBytes(StandardCharsets.UTF_8));
               httpsurlconnection.setConnectTimeout(5000);
               httpsurlconnection.setReadTimeout(5000);
               httpsurlconnection.connect();
               s3 = IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8);
               s14 = s3.split("\"Token\":\"")[1].split("\"")[0];
               String s15 = s3.split("\"uhs\":\"")[1].split("\"")[0];
               String s16 = "XBL3.0 x=" + s15 + ";" + s14;
               String s17 = MinecraftServicesApi.post("https://api.minecraftservices.com/authentication/login_with_xbox", "{\"identityToken\":\"" + s16 + "\"}", true);
               if (s17 == null) {
                  return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
               }

               JsonObject jsonobject = new JsonParser().parse(s17).getAsJsonObject();
               if (!jsonobject.has("access_token")) {
                  return new LoginResult(LoginResponseType.VALID_RATE_LIMITED);
               }

               String s18 = jsonobject.get("access_token").getAsString();

               try {
                  httpsurlconnection = (HttpsURLConnection)URI.create("https://api.minecraftservices.com/minecraft/profile").toURL().openConnection();
                  httpsurlconnection.setRequestProperty("Authorization", "Bearer " + s18);
                  httpsurlconnection.setConnectTimeout(5000);
                  httpsurlconnection.setReadTimeout(5000);
                  httpsurlconnection.connect();
                  MinecraftProfileDto MinecraftProfileDto = (MinecraftProfileDto)gson.fromJson(IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8), MinecraftProfileDto.class);
                  httpsurlconnection.disconnect();
                  return new LoginResult(new Session(MinecraftProfileDto.name, MinecraftProfileDto.id, s18, "mojang"), LoginResponseType.SUCCESS);
               } catch (FileNotFoundException filenotfoundexception) {
                  return MinecraftServicesApi.ownsMinecraft(s18)
                     ? new LoginResult(new Session(null, null, s18, "mojang"), LoginResponseType.NO_MINECRAFT_PROFILE)
                     : new LoginResult(new Session(null, null, s18, "mojang"), LoginResponseType.DOES_NOT_OWN_MINECRAFT);
               } finally {
                  httpsurlconnection.disconnect();
               }
            } else {
               if (flag) {
                  return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
               }

               String s11 = s3.split("type=\"hidden\" name=\"uaid\" id=\"uaid\" value=\"")[1].split("\"")[0];
               String s12 = s3.split("name=\"pprid\" id=\"pprid\" value=\"")[1].split("\"")[0];
               String s13 = s3.split("name=\"ipt\" id=\"ipt\" value=\"")[1].split("\"")[0];
               s8 = "ipt=" + s13 + "&pprid=" + s12 + "&uaid=" + s11;
               httpsurlconnection = (HttpsURLConnection)URI.create(s3.split("><form name=\"fmHF\" id=\"fmHF\" action=\"")[1].split("\"")[0])
                  .toURL()
                  .openConnection();
               httpsurlconnection.setRequestMethod("POST");
               httpsurlconnection.setRequestProperty("User-Agent", s2);
               httpsurlconnection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
               httpsurlconnection.addRequestProperty("Accept", "*/*");
               httpsurlconnection.setDoOutput(true);
               httpsurlconnection.getOutputStream().write(s8.getBytes(StandardCharsets.UTF_8));
               httpsurlconnection.setConnectTimeout(5000);
               httpsurlconnection.setReadTimeout(5000);
               httpsurlconnection.connect();
               s3 = IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8);
               s8 = "iProofOptions=Email&DisplayPhoneCountryISO=US&DisplayPhoneNumber=&EmailAddress=&canary="
                  + s3.split("=\"canary\" name=\"canary\" value=\"")[1].split("\"")[0]
                  + "&action=Skip&PhoneNumber=&PhoneCountryISO=";
               httpsurlconnection = (HttpsURLConnection)URI.create(s3.split("id=\"frmAddProof\" method=\"post\" action=\"")[1].split("\"")[0])
                  .toURL()
                  .openConnection();
               httpsurlconnection.setRequestMethod("POST");
               httpsurlconnection.setRequestProperty("User-Agent", s2);
               httpsurlconnection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
               httpsurlconnection.addRequestProperty("Accept", "*/*");
               httpsurlconnection.setDoOutput(true);
               httpsurlconnection.getOutputStream().write(s8.getBytes(StandardCharsets.UTF_8));
               httpsurlconnection.setConnectTimeout(5000);
               httpsurlconnection.setReadTimeout(5000);
               httpsurlconnection.connect();
               return httpsurlconnection.getResponseCode() != 200 ? new LoginResult(LoginResponseType.INVALID_ACCOUNT) : loginWithCredentials(s, s1, true);
            }
         } else {
            CookieHandler.setDefault(cookiehandler);
            return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
         }
      } catch (IOException ioexception) {
         CookieHandler.setDefault(cookiehandler);
         return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
      }
   }
}
