package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.account.BrowserAccount;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class OAuthCallbackTest {
   @Test public void browserCallbackRequiresOneTimeStateAndCleansUpPort() throws Exception {
      AtomicReference<String> received = new AtomicReference<>();
      try {
         String url = BrowserAccount.startOAuthServerAndGetLoginUrl(received::set);
         assertNotNull(url);
         assertTrue(BrowserAccount.getOAuthCallbackPort() > 0);
         String state = null;
         for (String part : new URI(url).getRawQuery().split("&")) {
            if (part.startsWith("state=")) { state = part.substring(6); }
         }
         assertNotNull(state);
         assertNull(BrowserAccount.takeRefreshTokenConsumer("wrong-state"));
         java.util.function.Consumer<String> consumer = BrowserAccount.takeRefreshTokenConsumer(state);
         assertNotNull(consumer);
         consumer.accept("test-refresh-token");
         assertEquals("test-refresh-token", received.get());
         assertNull(BrowserAccount.takeRefreshTokenConsumer(state));
      } finally {
         BrowserAccount.stopOAuthServer();
      }
      assertEquals(0, BrowserAccount.getOAuthCallbackPort());
   }

   @Test public void malformedAndForgedCallbacksReturnAnErrorResponse() {
      for (String uri : new String[]{"/favicon.ico", "/?code=test&state=forged"}) {
         EmbeddedChannel channel = new EmbeddedChannel(new OAuthCallbackHandler());
         channel.writeInbound(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri));
         FullHttpResponse response = (FullHttpResponse)channel.readOutbound();
         assertNotNull(response);
         assertEquals(HttpResponseStatus.BAD_REQUEST, response.getStatus());
         response.release();
         channel.finish();
      }
   }
}
