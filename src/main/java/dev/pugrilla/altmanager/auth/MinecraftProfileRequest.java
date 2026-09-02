package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.network.HttpRequest;
import dev.pugrilla.altmanager.network.HttpRequestException;

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
public final class MinecraftProfileRequest implements HttpRequest {
   private final String accessToken;

   public MinecraftProfileRequest(String s) {
      this.accessToken = s;
   }

   public MinecraftProfileResponse send() throws HttpRequestException {
      try {
         HttpURLConnection httpurlconnection = (HttpURLConnection)URI.create("https://api.minecraftservices.com/minecraft/profile").toURL().openConnection();
         CookieUtils.configureConnection(httpurlconnection);
         httpurlconnection.setRequestProperty("Authorization", "Bearer " + this.accessToken);
         String s = IOUtils.toString(httpurlconnection.getInputStream(), StandardCharsets.UTF_8);
         Gson gson = new Gson();
         return (MinecraftProfileResponse)gson.fromJson(s, MinecraftProfileResponse.class);
      } catch (FileNotFoundException filenotfoundexception) {
         throw new NoMinecraftProfileException("No Minecraft profile!");
      } catch (IOException ioexception) {
         throw new HttpRequestException(ioexception.getMessage());
      }
   }
}
