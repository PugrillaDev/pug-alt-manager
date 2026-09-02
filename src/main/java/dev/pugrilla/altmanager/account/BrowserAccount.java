package dev.pugrilla.altmanager.account;

import dev.pugrilla.altmanager.auth.MinecraftProfileDto;
import dev.pugrilla.altmanager.auth.MinecraftServicesApi;
import dev.pugrilla.altmanager.auth.OAuthServerChannelInitializer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import javax.net.ssl.HttpsURLConnection;
import net.minecraft.util.Session;
import org.apache.commons.io.IOUtils;
public final class BrowserAccount extends AbstractAccount {
   private static final String CLIENT_ID = "c4f0db78-5015-4a94-8b34-1a4da87b9ce4";
   private static final String CLIENT_SECRET = "G.F8Q~x.uPo1CEz5I2CRpkx-KkVjNn1rNVM1ScYg";
   private String refreshToken;
   private static Consumer<String> refreshTokenConsumer;
   private static EventLoopGroup bossEventLoopGroup;
   private static EventLoopGroup workerEventLoopGroup;
   private static int oauthCallbackPort;

   public BrowserAccount(String s, UUID uuid, String s1) {
      super(AccountType.BROWSER, s, uuid, s1);
   }

   public BrowserAccount(String s) {
      this("UninitAcc", AbstractAccount.NIL_UUID, "");
      this.refreshToken = s;
   }

   public String getRefreshToken() {
      return this.refreshToken;
   }

   @Override
   protected LoginResult loginImpl() {
      try {
         String s = MinecraftServicesApi.post(
            "https://login.live.com/oauth20_token.srf",
            "client_id="
               + encodeQueryValue(CLIENT_ID)
               + "&client_secret="
               + encodeQueryValue(CLIENT_SECRET)
               + "&refresh_token="
               + encodeQueryValue(this.refreshToken)
               + "&grant_type=refresh_token&redirect_uri=http://localhost:"
               + oauthCallbackPort,
            false
         );
         if (s == null) {
            return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
         }

         JsonObject jsonobject = new JsonParser().parse(s).getAsJsonObject();
         String s1 = jsonobject.get("access_token").getAsString();
         this.refreshToken = jsonobject.get("refresh_token").getAsString();
         s = MinecraftServicesApi.post(
            "https://user.auth.xboxlive.com/user/authenticate",
            "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d="
               + s1
               + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}",
            true
         );
         if (s == null) {
            return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
         }

         JsonObject jsonobject1 = new JsonParser().parse(s).getAsJsonObject();
         String s2 = jsonobject1.get("Token").getAsString();
         String s3 = s.split("\"uhs\":\"")[1].split("\"")[0];
         s = MinecraftServicesApi.post(
            "https://xsts.auth.xboxlive.com/xsts/authorize",
            "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""
               + s2
               + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}",
            true
         );
         if (s == null) {
            return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
         }

         JsonObject jsonobject2 = new JsonParser().parse(s).getAsJsonObject();
         s2 = jsonobject2.get("Token").getAsString();
         s = MinecraftServicesApi.post(
            "https://api.minecraftservices.com/authentication/login_with_xbox", "{\"identityToken\":\"XBL3.0 x=" + s3 + ";" + s2 + "\"}", true
         );
         if (s == null) {
            return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
         }

         JsonObject jsonobject3 = new JsonParser().parse(s).getAsJsonObject();
         if (!jsonobject3.has("access_token")) {
            return new LoginResult(LoginResponseType.VALID_RATE_LIMITED);
         }

         String s4 = jsonobject3.get("access_token").getAsString();
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create("https://api.minecraftservices.com/minecraft/profile").toURL().openConnection();

         try {
            httpsurlconnection.setRequestProperty("Authorization", "Bearer " + s4);
            httpsurlconnection.setConnectTimeout(5000);
            httpsurlconnection.setReadTimeout(5000);
            httpsurlconnection.connect();
            MinecraftProfileDto MinecraftProfileDto = (MinecraftProfileDto)new Gson().fromJson(IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8), MinecraftProfileDto.class);
            httpsurlconnection.disconnect();
            return new LoginResult(new Session(MinecraftProfileDto.name, MinecraftProfileDto.id, s4, "mojang"), LoginResponseType.SUCCESS);
         } catch (FileNotFoundException filenotfoundexception) {
            return MinecraftServicesApi.ownsMinecraft(s4)
               ? new LoginResult(new Session(null, null, s4, "mojang"), LoginResponseType.NO_MINECRAFT_PROFILE)
               : new LoginResult(new Session(null, null, s4, "mojang"), LoginResponseType.DOES_NOT_OWN_MINECRAFT);
         } finally {
            httpsurlconnection.disconnect();
         }
      } catch (Exception exception) {
         return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
      }
   }

   @Override
   protected void deserialize(DataInputStream datainputstream) throws IOException {
      this.refreshToken = datainputstream.readUTF();
   }

   @Override
   protected void serialize(DataOutputStream dataoutputstream) throws IOException {
      dataoutputstream.writeUTF(this.refreshToken);
   }
   public static boolean openUrl(String s) {
      try {
         if (!Desktop.getDesktop().isSupported(Action.BROWSE)) {
            throw new Throwable();
         }

         Desktop.getDesktop().browse(new URI(s));
         return true;
      } catch (Throwable throwable) {
         String s1 = System.getProperty("os.name").toLowerCase();
         ProcessBuilder processbuilder = new ProcessBuilder();

         try {
            if (s1.contains("win")) {
               processbuilder.command("cmd", "/c", "start", s);
            } else if (s1.contains("mac")) {
               processbuilder.command("open", s);
            } else {
               if (!s1.contains("nix") && !s1.contains("nux")) {
                  return false;
               }

               processbuilder.command("xdg-open", s);
            }

            processbuilder.start();
            return true;
         } catch (IOException ioexception) {
            return false;
         }
      }
   }
   public static String startOAuthServerAndGetLoginUrl(Consumer<String> consumer) {
      boolean flag = bossEventLoopGroup != null && workerEventLoopGroup != null;
      if (!flag) {
         try {
            refreshTokenConsumer = consumer;
            bossEventLoopGroup = new NioEventLoopGroup(1);
            workerEventLoopGroup = new NioEventLoopGroup();
            ServerBootstrap serverbootstrap = new ServerBootstrap();
            ((ServerBootstrap)((ServerBootstrap)serverbootstrap.group(bossEventLoopGroup, workerEventLoopGroup).channel(NioServerSocketChannel.class))
                  .childHandler(new OAuthServerChannelInitializer())
                  .option(ChannelOption.SO_BACKLOG, 128))
               .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelfuture = serverbootstrap.bind(0).sync();
            oauthCallbackPort = ((NioServerSocketChannel)channelfuture.channel()).localAddress().getPort();
         } catch (Exception exception) {
            stopOAuthServer();
         }
      }

      return oauthCallbackPort > 0
         ? "https://login.live.com/oauth20_authorize.srf?client_id="
            + encodeQueryValue(CLIENT_ID)
            + "&prompt=select_account&response_type=code&redirect_uri=http://localhost:"
            + oauthCallbackPort
            + "&scope=XboxLive.signin%20offline_access"
         : null;
   }
   public static void stopOAuthServer() {
      if (bossEventLoopGroup != null) {
         bossEventLoopGroup.shutdownGracefully();
         workerEventLoopGroup.shutdownGracefully();
         bossEventLoopGroup = null;
         workerEventLoopGroup = null;
         refreshTokenConsumer = null;
      }
   }

   public static int getOAuthCallbackPort() {
      return oauthCallbackPort;
   }

   public static Consumer<String> getRefreshTokenConsumer() {
      return refreshTokenConsumer;
   }

   public static String getOAuthClientId() {
      return CLIENT_ID;
   }

   public static String getOAuthClientSecret() {
      return CLIENT_SECRET;
   }

   private static String encodeQueryValue(String value) {
      try {
         return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
      } catch (Exception exception) {
         throw new IllegalStateException("Could not encode OAuth request parameter", exception);
      }
   }
}
