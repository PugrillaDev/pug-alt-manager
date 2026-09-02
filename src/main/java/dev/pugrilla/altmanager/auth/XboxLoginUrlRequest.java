package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.network.HttpRequest;
import dev.pugrilla.altmanager.network.HttpRequestException;

import java.net.URI;
import javax.net.ssl.HttpsURLConnection;
public final class XboxLoginUrlRequest implements HttpRequest {
   public String send() throws HttpRequestException {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create(
               "https://sisu.xboxlive.com/connect/XboxLive/?state=login&cobrandId=8058f65d-ce06-4c30-9559-473c9275a65d&tid=896928775&ru=https://www.minecraft.net/en-us/login?return_url=https%3A%2F%2Fwww.minecraft.net%2Fen-us%2Fmsaprofile%2Fmygames%2Feditprofile&aid=1142970254"
            )
            .toURL()
            .openConnection();
         CookieUtils.configureConnection(httpsurlconnection);
         httpsurlconnection.setInstanceFollowRedirects(false);
         String s = httpsurlconnection.getHeaderField("Location");
         return s.replaceAll(" ", "%20");
      } catch (Exception exception) {
         throw new HttpRequestException(exception.getMessage());
      }
   }
}
