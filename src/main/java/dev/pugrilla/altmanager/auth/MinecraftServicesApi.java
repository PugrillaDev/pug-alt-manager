package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.skin.SkinVariant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.io.OutputStream;
import net.minecraft.util.Session;
import dev.pugrilla.altmanager.account.LoginResult;
import dev.pugrilla.altmanager.account.LoginResponseType;
import dev.pugrilla.altmanager.util.AltManagerUtils;
import org.apache.commons.io.IOUtils;
public final class MinecraftServicesApi {
   private static final List<String> OWNERSHIP_ENTITLEMENTS = Arrays.asList("game_minecraft", "product_minecraft", "product_game_pass_pc");

   public static final class Response {
      public final int status;
      public final String body;

      private Response(int status, String body) {
         this.status = status;
         this.body = body;
      }
   }

   public static Response request(String method, String url, String token, String contentType, byte[] body) throws IOException {
      HttpURLConnection connection = (HttpURLConnection)URI.create(url).toURL().openConnection();
      try {
         connection.setConnectTimeout(5000);
         connection.setReadTimeout(10000);
         connection.setInstanceFollowRedirects(false);
         connection.setRequestMethod(method);
         connection.setRequestProperty("Accept", "application/json");
         if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
         }
         if (body != null) {
            connection.setRequestProperty("Content-Type", contentType);
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
               output.write(body);
            }
         }
         int status = connection.getResponseCode();
         InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
         try (InputStream input = stream) {
            return new Response(status, input == null ? "" : IOUtils.toString(input, StandardCharsets.UTF_8));
         }
      } finally {
         connection.disconnect();
      }
   }

   public static LoginResult validateSession(Session session) {
      try {
         Response response = request("GET", "https://api.minecraftservices.com/minecraft/profile", session.getToken(), null, null);
         if (response.status == 401 || response.status == 403) {
            return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
         }
         if (response.status == 429) {
            return new LoginResult(LoginResponseType.RATE_LIMITED);
         }
         if (response.status == 404) {
            return new LoginResult(LoginResponseType.NO_MINECRAFT_PROFILE);
         }
         if (response.status != 200) {
            return new LoginResult(LoginResponseType.NETWORK_ERROR);
         }
         JsonObject profile = new JsonParser().parse(response.body).getAsJsonObject();
         UUID uuid = AltManagerUtils.parseUuid(profile.get("id").getAsString());
         if (uuid == null || !uuid.equals(session.getProfile().getId())) {
            return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
         }
         return new LoginResult(session, LoginResponseType.SUCCESS);
      } catch (IOException | RuntimeException failure) {
         return new LoginResult(LoginResponseType.NETWORK_ERROR);
      }
   }
   public static NameChangeStatus getNameChangeStatus(String s) {
      try {
         Response response = request("GET", "https://api.minecraftservices.com/minecraft/profile/namechange", s, null, null);
         switch (response.status) {
            case 200:
               if (new JsonParser().parse(response.body).getAsJsonObject().get("nameChangeAllowed").getAsBoolean()) {
                  return NameChangeStatus.AVAILABLE;
               }

               return NameChangeStatus.COOLDOWN;
            case 401:
               return NameChangeStatus.UNAUTHORIZED;
            case 429:
               return NameChangeStatus.RATE_LIMITED;
            default:
               return NameChangeStatus.ERROR;
         }
      } catch (IOException | RuntimeException ioexception) {
         return NameChangeStatus.ERROR;
      }
   }
   public static int setProfileName(String token, String name) {
      JsonObject body = new JsonObject();
      body.addProperty("profileName", name);
      return sendJson("PUT", "https://api.minecraftservices.com/minecraft/profile/name/" + name, token, body);
   }

   public static int setSkinFromUrl(String token, String url, SkinVariant variant) {
      JsonObject body = new JsonObject();
      body.addProperty("variant", variant.getInternalName());
      body.addProperty("url", url);
      return sendJson("POST", "https://api.minecraftservices.com/minecraft/profile/skins", token, body);
   }

   private static int sendJson(String method, String url, String token, JsonObject body) {
      try {
         return request(method, url, token, "application/json", body.toString().getBytes(StandardCharsets.UTF_8)).status;
      } catch (IOException failure) {
         return -1;
      }
   }

   public static int setSkinFromFile(String token, byte[] image, File file, SkinVariant variant) {
      String boundary = UUID.randomUUID().toString();
      try {
         java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
         body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"variant\"\r\n\r\n"
            + variant.getInternalName() + "\r\n--" + boundary
            + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
         body.write(image);
         body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
         return request("POST", "https://api.minecraftservices.com/minecraft/profile/skins", token,
            "multipart/form-data; boundary=" + boundary, body.toByteArray()).status;
      } catch (IOException failure) {
         return -1;
      }
   }

   public static int createProfile(String token, String name) {
      JsonObject body = new JsonObject();
      body.addProperty("profileName", name);
      return sendJson("POST", "https://api.minecraftservices.com/minecraft/profile", token, body);
   }

   public static boolean ownsMinecraft(String token) {
      try {
         Response response = request("GET", "https://api.minecraftservices.com/entitlements/mcstore", token, null, null);
         if (response.status != 200) {
            return false;
         }
         JsonObject body = new JsonParser().parse(response.body).getAsJsonObject();
         for (JsonElement item : body.getAsJsonArray("items")) {
            if (OWNERSHIP_ENTITLEMENTS.contains(item.getAsJsonObject().get("name").getAsString())) {
               return true;
            }
         }
      } catch (IOException | RuntimeException ignored) {
      }
      return false;
   }

   public static String post(String s, String s1, boolean flag) {
      try {
         Response response = request("POST", s, null, flag ? "application/json" : "application/x-www-form-urlencoded; charset=UTF-8",
            s1.getBytes(StandardCharsets.UTF_8));
         return response.status / 100 == 2 ? response.body : null;
      } catch (Exception exception) {
         return null;
      }
   }
   public static String getUuidForUsername(String username) throws IOException {
      if (!AltManagerUtils.MINECRAFT_USERNAME_PATTERN.matcher(username).matches()) {
         throw new IOException("Invalid Minecraft username");
      }
      Response response = request("GET", "https://api.mojang.com/users/profiles/minecraft/" + username, null, null, null);
      if (response.status != 200) {
         throw new IOException("Profile lookup failed (HTTP " + response.status + ")");
      }
      try {
         return new JsonParser().parse(response.body).getAsJsonObject().get("id").getAsString();
      } catch (RuntimeException failure) {
         throw new IOException("Invalid profile lookup response");
      }
   }
}
