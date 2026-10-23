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
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.util.Session;
public final class BrowserAccount extends AbstractAccount {
   private static final String CLIENT_ID = "c4f0db78-5015-4a94-8b34-1a4da87b9ce4";
   private static final String CLIENT_SECRET = "G.F8Q~x.uPo1CEz5I2CRpkx-KkVjNn1rNVM1ScYg";
   private String refreshToken;
   private static volatile Consumer<String> refreshTokenConsumer;
   private static volatile String oauthState;
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
         JsonObject microsoft = tokenPost("https://login.live.com/oauth20_token.srf",
            "client_id=" + encodeQueryValue(CLIENT_ID) + "&client_secret=" + encodeQueryValue(CLIENT_SECRET)
               + "&refresh_token=" + encodeQueryValue(this.refreshToken) + "&grant_type=refresh_token", false);
         if (microsoft.has("refresh_token")) {
            this.refreshToken = microsoft.get("refresh_token").getAsString();
         }
         String access = microsoft.get("access_token").getAsString();
         JsonObject xbox = tokenPost("https://user.auth.xboxlive.com/user/authenticate",
            "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d="
               + access + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}", true);
         JsonObject xsts = tokenPost("https://xsts.auth.xboxlive.com/xsts/authorize",
            "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xbox.get("Token").getAsString()
               + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}", true);
         String userHash = xsts.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0)
            .getAsJsonObject().get("uhs").getAsString();
         JsonObject minecraft = tokenPost("https://api.minecraftservices.com/authentication/login_with_xbox",
            "{\"identityToken\":\"XBL3.0 x=" + userHash + ";" + xsts.get("Token").getAsString() + "\"}", true);
         String token = minecraft.get("access_token").getAsString();
         MinecraftServicesApi.Response profile = MinecraftServicesApi.request("GET",
            "https://api.minecraftservices.com/minecraft/profile", token, null, null);
         if (profile.status == 404) {
            return new LoginResult(new Session(null, null, token, "mojang"), MinecraftServicesApi.ownsMinecraft(token)
               ? LoginResponseType.NO_MINECRAFT_PROFILE : LoginResponseType.DOES_NOT_OWN_MINECRAFT);
         }
         requireSuccess(profile.status);
         MinecraftProfileDto data = new Gson().fromJson(profile.body, MinecraftProfileDto.class);
         return new LoginResult(new Session(data.name, data.id, token, "mojang"), LoginResponseType.SUCCESS);
      } catch (AuthFailure failure) {
         return new LoginResult(failure.response);
      } catch (IOException failure) {
         return new LoginResult(LoginResponseType.NETWORK_ERROR);
      } catch (RuntimeException failure) {
         return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
      }
   }

   private static JsonObject tokenPost(String url, String body, boolean json) throws IOException {
      MinecraftServicesApi.Response response = MinecraftServicesApi.request("POST", url, null,
         json ? "application/json" : "application/x-www-form-urlencoded; charset=UTF-8", body.getBytes(StandardCharsets.UTF_8));
      requireSuccess(response.status);
      return new JsonParser().parse(response.body).getAsJsonObject();
   }

   private static void requireSuccess(int status) {
      if (status / 100 != 2) {
         throw new AuthFailure(status == 429 ? LoginResponseType.RATE_LIMITED
            : status >= 500 ? LoginResponseType.NETWORK_ERROR : LoginResponseType.INVALID_ACCOUNT);
      }
   }

   private static final class AuthFailure extends RuntimeException {
      private final LoginResponseType response;

      private AuthFailure(LoginResponseType response) {
         this.response = response;
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
      if (s == null) {
         return false;
      }
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
               processbuilder.command("rundll32.exe", "url.dll,FileProtocolHandler", s);
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
   public static synchronized String startOAuthServerAndGetLoginUrl(Consumer<String> consumer) {
      boolean flag = bossEventLoopGroup != null && workerEventLoopGroup != null;
      if (!flag) {
         try {
            refreshTokenConsumer = consumer;
            oauthState = UUID.randomUUID().toString();
            bossEventLoopGroup = new NioEventLoopGroup(1);
            workerEventLoopGroup = new NioEventLoopGroup(1);
            ServerBootstrap serverbootstrap = new ServerBootstrap();
            ((ServerBootstrap)((ServerBootstrap)serverbootstrap.group(bossEventLoopGroup, workerEventLoopGroup).channel(NioServerSocketChannel.class))
                  .childHandler(new OAuthServerChannelInitializer())
                  .option(ChannelOption.SO_BACKLOG, 128))
               .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelfuture = serverbootstrap.bind("127.0.0.1", 0).sync();
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
            + "&state=" + oauthState
            + "&scope=XboxLive.signin%20offline_access"
         : null;
   }
   public static synchronized void stopOAuthServer() {
      if (bossEventLoopGroup != null) {
         bossEventLoopGroup.shutdownGracefully();
      }
      if (workerEventLoopGroup != null) {
         workerEventLoopGroup.shutdownGracefully();
      }
      bossEventLoopGroup = null;
      workerEventLoopGroup = null;
      refreshTokenConsumer = null;
      oauthState = null;
      oauthCallbackPort = 0;
   }

   public static synchronized Consumer<String> takeRefreshTokenConsumer(String state) {
      if (state == null || !state.equals(oauthState)) {
         return null;
      }
      Consumer<String> consumer = refreshTokenConsumer;
      refreshTokenConsumer = null;
      return consumer;
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
