package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.network.HttpRequest;
import dev.pugrilla.altmanager.network.HttpRequestException;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
public final class MinecraftLoginRequest implements HttpRequest {
   private final String requestBody;

   public MinecraftLoginRequest(String s) {
      this.requestBody = "{\"identityToken\":\"" + s + "\"}";
   }

   public MinecraftLoginResponse send() throws HttpRequestException {
      try {
         HttpURLConnection httpurlconnection = (HttpURLConnection)URI.create("https://api.minecraftservices.com/authentication/login_with_xbox")
            .toURL()
            .openConnection();
         CookieUtils.configureConnection(httpurlconnection);
         httpurlconnection.setRequestProperty("Content-Type", "application/json");
         httpurlconnection.setRequestProperty("Accept", "application/json");
         httpurlconnection.setRequestMethod("POST");
         httpurlconnection.setDoOutput(true);
         httpurlconnection.getOutputStream().write(this.requestBody.getBytes(StandardCharsets.UTF_8));
         String s = IOUtils.toString(httpurlconnection.getInputStream(), StandardCharsets.UTF_8);
         Gson gson = new Gson();
         return (MinecraftLoginResponse)gson.fromJson(s, MinecraftLoginResponse.class);
      } catch (IOException ioexception) {
         throw new HttpRequestException(ioexception.getMessage());
      }
   }
}
