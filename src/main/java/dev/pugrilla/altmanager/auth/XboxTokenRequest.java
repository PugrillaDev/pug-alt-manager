package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.network.HttpRequest;
import dev.pugrilla.altmanager.network.HttpRequestException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
public final class XboxTokenRequest implements HttpRequest {
   private final URI loginUri;
   private final String cookieHeader;

   public XboxTokenRequest(String s, String s1) {
      this.loginUri = URI.create(s);
      this.cookieHeader = s1;
   }

   public XboxTokenResponse send() throws HttpRequestException {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)this.loginUri.toURL().openConnection();
         CookieUtils.configureConnection(httpsurlconnection);
         httpsurlconnection.setRequestProperty("Cookie", this.cookieHeader);
         httpsurlconnection.setInstanceFollowRedirects(false);
         String s = httpsurlconnection.getHeaderField("Location");
         if (!s.contains("accessToken=")) {
            throw new HttpRequestException("No access token!");
         }

         String s1 = s.split("accessToken=")[1];
         String s2 = new String(Base64.getDecoder().decode(s1), StandardCharsets.UTF_8).split("\"rp://api.minecraftservices.com/\",")[1];
         String s3 = s2.split("\"Token\":\"")[1].split("\"")[0];
         String s4 = s2.split(Pattern.quote("{\"DisplayClaims\":{\"xui\":[{\"uhs\":\""))[1].split("\"")[0];
         String s5 = "XBL3.0 x=" + s4 + ";" + s3;
         return new XboxTokenResponse(s5);
      } catch (Exception exception) {
         throw new HttpRequestException(exception.getMessage());
      }
   }
}
