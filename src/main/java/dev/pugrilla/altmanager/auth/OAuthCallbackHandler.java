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
import java.nio.charset.StandardCharsets;
import org.apache.http.entity.ContentType;
public class OAuthCallbackHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
   protected void channelRead0(ChannelHandlerContext channelhandlercontext, FullHttpRequest fullhttprequest) {
      String s = fullhttprequest.getUri();

      try {
         if (s.contains("?code=")) {
            String s1 = fullhttprequest.getUri().split("\\?code=")[1];
            String s2 = MinecraftServicesApi.post(
               "https://login.live.com/oauth20_token.srf",
               "client_id="
                  + BrowserAccount.getOAuthClientId()
                  + "&code="
                  + s1
                  + "&client_secret="
                  + BrowserAccount.getOAuthClientSecret()
                  + "&grant_type=authorization_code&redirect_uri=http://localhost:"
                  + BrowserAccount.getOAuthCallbackPort(),
               false
            );
            if (s2 != null) {
               JsonObject jsonobject = new JsonParser().parse(s2).getAsJsonObject();
               if (jsonobject.has("refresh_token")) {
                  BrowserAccount.getRefreshTokenConsumer().accept(jsonobject.get("refresh_token").getAsString());
                  this.writeText(
                     channelhandlercontext,
                     "<html><h1>Successful account login!</h1><p>You can close this webpage and return to your client.</p><script>close()</script></html>"
                  );
                  BrowserAccount.stopOAuthServer();
                  return;
               }
            }
         }
      } catch (Throwable throwable) {
         throw new RuntimeException("Browser exchange", throwable);
      }
   }

   private void writeText(ChannelHandlerContext channelhandlercontext, String s) {
      DefaultFullHttpResponse defaultfullhttpresponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      defaultfullhttpresponse.headers().set("Content-Type", ContentType.TEXT_HTML + "; charset=UTF-8");
      defaultfullhttpresponse.content().writeBytes(s.getBytes(StandardCharsets.UTF_8));
      channelhandlercontext.writeAndFlush(defaultfullhttpresponse).addListener(ChannelFutureListener.CLOSE);
   }
}
