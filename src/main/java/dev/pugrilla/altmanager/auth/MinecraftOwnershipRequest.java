package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.network.HttpRequest;
import dev.pugrilla.altmanager.network.HttpRequestException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
public final class MinecraftOwnershipRequest implements HttpRequest {
   private static final List<String> OWNERSHIP_ENTITLEMENTS = Arrays.asList("game_minecraft", "product_minecraft", "product_game_pass_pc");
   private final String accessToken;

   public MinecraftOwnershipRequest(String s) {
      this.accessToken = s;
   }

   public MinecraftOwnershipResponse send() throws HttpRequestException {
      try {
         HttpURLConnection httpurlconnection = (HttpURLConnection)URI.create("https://api.minecraftservices.com/entitlements/mcstore").toURL().openConnection();
         CookieUtils.configureConnection(httpurlconnection);
         httpurlconnection.setRequestProperty("Accept", "application/json");
         httpurlconnection.setRequestProperty("Authorization", "Bearer " + this.accessToken);
         String s = IOUtils.toString(httpurlconnection.getInputStream(), StandardCharsets.UTF_8);
         JsonObject jsonobject = (JsonObject)new Gson().fromJson(s, JsonObject.class);

         for (JsonElement jsonelement : jsonobject.getAsJsonArray("items")) {
            JsonObject jsonobject1 = jsonelement.getAsJsonObject();
            String s1 = jsonobject1.get("name").getAsString();
            if (OWNERSHIP_ENTITLEMENTS.contains(s1)) {
               return new MinecraftOwnershipResponse(true);
            }
         }

         return new MinecraftOwnershipResponse(false);
      } catch (IOException ioexception) {
         throw new HttpRequestException(ioexception.getMessage());
      }
   }
}
