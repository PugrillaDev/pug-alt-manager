package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.account.BrowserAccount;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OAuthCallbackHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
   protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) {
      Map<String, List<String>> parameters = new QueryStringDecoder(request.getUri()).parameters();
      String code = first(parameters, "code");
      if (code == null) {
         this.writeText(context, HttpResponseStatus.BAD_REQUEST, "Login was not completed. Return to the client and try again.");
         return;
      }
      Consumer<String> consumer = BrowserAccount.takeRefreshTokenConsumer(first(parameters, "state"));
      if (consumer == null) {
         this.writeText(context, HttpResponseStatus.BAD_REQUEST, "This login link is invalid or has already been used.");
         return;
      }
      try {
         String response = MinecraftServicesApi.post("https://login.live.com/oauth20_token.srf",
            "client_id=" + encode(BrowserAccount.getOAuthClientId()) + "&code=" + encode(code)
               + "&client_secret=" + encode(BrowserAccount.getOAuthClientSecret())
               + "&grant_type=authorization_code&redirect_uri=" + encode("http://localhost:" + BrowserAccount.getOAuthCallbackPort()), false);
         if (response != null) {
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            if (json.has("refresh_token")) {
               consumer.accept(json.get("refresh_token").getAsString());
               this.writeText(context, HttpResponseStatus.OK, "Login completed. You can close this page and return to your client.");
               return;
            }
         }
         this.writeText(context, HttpResponseStatus.BAD_GATEWAY, "The sign-in service rejected this login. Return to the client and try again.");
      } catch (Exception failure) {
         this.writeText(context, HttpResponseStatus.BAD_GATEWAY, "The sign-in service could not be reached. Return to the client and try again.");
      } finally {
         BrowserAccount.stopOAuthServer();
      }
   }

   private static String first(Map<String, List<String>> parameters, String key) {
      List<String> values = parameters.get(key);
      return values == null || values.size() != 1 ? null : values.get(0);
   }

   private static String encode(String value) throws Exception {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
   }

   private void writeText(ChannelHandlerContext context, HttpResponseStatus status, String text) {
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
      response.headers().set("Content-Type", "text/plain; charset=UTF-8");
      response.headers().set("Cache-Control", "no-store");
      response.content().writeBytes(text.getBytes(StandardCharsets.UTF_8));
      response.headers().set("Content-Length", response.content().readableBytes());
      context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
   }
}
