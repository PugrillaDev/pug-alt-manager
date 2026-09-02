package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.network.HttpRequest;
import dev.pugrilla.altmanager.network.HttpRequestException;

import java.io.IOException;
import java.net.URI;
import javax.net.ssl.HttpsURLConnection;
public final class CookieRedirectRequest implements HttpRequest {
   private final URI endpoint;
   private final String cookieHeader;

   public CookieRedirectRequest(String s, String s1) {
      this.endpoint = URI.create(s);
      this.cookieHeader = s1;
   }

   public String send() throws HttpRequestException {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)this.endpoint.toURL().openConnection();
         CookieUtils.configureConnection(httpsurlconnection);
         httpsurlconnection.setRequestProperty("Cookie", this.cookieHeader);
         httpsurlconnection.setInstanceFollowRedirects(false);
         return httpsurlconnection.getHeaderField("Location");
      } catch (IOException ioexception) {
         throw new HttpRequestException(ioexception.getMessage());
      }
   }
}
