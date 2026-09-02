package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.skin.SkinVariant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.io.IOUtils;
public final class MinecraftServicesApi {
   private static final List<String> OWNERSHIP_ENTITLEMENTS = Arrays.asList("game_minecraft", "product_minecraft", "product_game_pass_pc");
   public static NameChangeStatus getNameChangeStatus(String s) {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create("https://api.minecraftservices.com/minecraft/profile/namechange")
            .toURL()
            .openConnection();
         httpsurlconnection.setRequestProperty("Authorization", "Bearer " + s);
         httpsurlconnection.setRequestMethod("GET");
         httpsurlconnection.setConnectTimeout(5000);
         httpsurlconnection.setReadTimeout(5000);
         httpsurlconnection.connect();
         BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(httpsurlconnection.getInputStream()));
         JsonObject jsonobject = new JsonParser().parse(bufferedreader).getAsJsonObject();
         int i = httpsurlconnection.getResponseCode();
         httpsurlconnection.disconnect();
         bufferedreader.close();
         switch (i) {
            case 200:
               if (jsonobject.get("nameChangeAllowed").getAsBoolean()) {
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
      } catch (IOException ioexception) {
         return NameChangeStatus.ERROR;
      }
   }
   public static int setProfileName(String s, String s1) {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create("https://api.minecraftservices.com/minecraft/profile/name/" + s1)
            .toURL()
            .openConnection();
         httpsurlconnection.setRequestProperty("Authorization", "Bearer " + s);
         httpsurlconnection.setRequestProperty("Content-Type", "application/json");
         httpsurlconnection.setRequestMethod("PUT");
         httpsurlconnection.setDoOutput(true);
         httpsurlconnection.getOutputStream().write(("{\"profileName\":\"" + s1 + "\"}").getBytes(StandardCharsets.UTF_8));
         httpsurlconnection.setConnectTimeout(5000);
         httpsurlconnection.setReadTimeout(5000);
         httpsurlconnection.connect();
         int i = httpsurlconnection.getResponseCode();
         httpsurlconnection.disconnect();
         return i;
      } catch (IOException ioexception) {
         return -1;
      }
   }
   public static int setSkinFromUrl(String s, String s1, SkinVariant SkinVariant) {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create("https://api.minecraftservices.com/minecraft/profile/skins")
            .toURL()
            .openConnection();
         httpsurlconnection.setRequestProperty("Authorization", "Bearer " + s);
         httpsurlconnection.setRequestProperty("Content-Type", "application/json");
         httpsurlconnection.setRequestMethod("POST");
         httpsurlconnection.setDoOutput(true);
         httpsurlconnection.getOutputStream()
            .write(("{\"variant\":\"" + SkinVariant.getInternalName() + "\",\"url\":\"" + s1 + "\"}").getBytes(StandardCharsets.UTF_8));
         httpsurlconnection.setConnectTimeout(5000);
         httpsurlconnection.setReadTimeout(5000);
         httpsurlconnection.connect();
         int i = httpsurlconnection.getResponseCode();
         httpsurlconnection.disconnect();
         return i;
      } catch (IOException ioexception) {
         return -1;
      }
   }
   public static int setSkinFromFile(String s, byte[] abyte, File file1, SkinVariant SkinVariant) {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create("https://api.minecraftservices.com/minecraft/profile/skins")
            .toURL()
            .openConnection();
         httpsurlconnection.setRequestProperty("Authorization", "Bearer " + s);
         String s1 = UUID.randomUUID().toString();
         httpsurlconnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + s1);
         httpsurlconnection.setRequestMethod("POST");
         httpsurlconnection.setDoOutput(true);
         PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(httpsurlconnection.getOutputStream(), StandardCharsets.UTF_8), true);
         String s2 = "\r\n";
         printwriter.append("--").append(s1).append("\r\n");
         printwriter.append("Content-Disposition: form-data; name=\"variant\"").append("\r\n");
         printwriter.append("\r\n");
         printwriter.append(SkinVariant.getInternalName()).append("\r\n");
         printwriter.append("--" + s1).append("\r\n");
         printwriter.append("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"").append("\r\n");
         printwriter.append("Content-Type: " + URLConnection.guessContentTypeFromName(file1.getName())).append("\r\n");
         printwriter.append("\r\n");
         printwriter.flush();
         httpsurlconnection.getOutputStream().write(abyte);
         printwriter.append("\r\n");
         printwriter.append("--" + s1 + "--").append("\r\n");
         printwriter.flush();
         httpsurlconnection.setConnectTimeout(5000);
         httpsurlconnection.setReadTimeout(5000);
         httpsurlconnection.connect();
         int i = httpsurlconnection.getResponseCode();
         httpsurlconnection.disconnect();
         return i;
      } catch (IOException ioexception) {
         return -1;
      }
   }
   public static int createProfile(String s, String s1) {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create("https://api.minecraftservices.com/minecraft/profile").toURL().openConnection();
         httpsurlconnection.setRequestProperty("Authorization", "Bearer " + s);
         httpsurlconnection.setRequestProperty("Content-Type", "application/json");
         httpsurlconnection.setRequestMethod("POST");
         httpsurlconnection.setDoOutput(true);
         httpsurlconnection.getOutputStream().write(("{\"profileName\":\"" + s1 + "\"}").getBytes(StandardCharsets.UTF_8));
         httpsurlconnection.setConnectTimeout(5000);
         httpsurlconnection.setReadTimeout(5000);
         httpsurlconnection.connect();
         int i = httpsurlconnection.getResponseCode();
         httpsurlconnection.disconnect();
         return i;
      } catch (IOException ioexception) {
         return -1;
      }
   }
   public static boolean ownsMinecraft(String s) {
      try {
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create("https://api.minecraftservices.com/entitlements/mcstore")
            .toURL()
            .openConnection();
         httpsurlconnection.setRequestProperty("Accept", "application/json");
         httpsurlconnection.setRequestProperty("Authorization", "Bearer " + s);
         httpsurlconnection.setConnectTimeout(15000);
         httpsurlconnection.setReadTimeout(15000);
         httpsurlconnection.connect();
         if (httpsurlconnection.getResponseCode() / 100 != 2) {
            return false;
         }

         String s1 = IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8);
         JsonObject jsonobject = new JsonParser().parse(s1).getAsJsonObject();

         for (JsonElement jsonelement : jsonobject.getAsJsonArray("items")) {
            JsonObject jsonobject1 = jsonelement.getAsJsonObject();
            String s2 = jsonobject1.get("name").getAsString();
            if (OWNERSHIP_ENTITLEMENTS.contains(s2)) {
               return true;
            }
         }

         return false;
      } catch (IOException ioexception) {
         return false;
      }
   }
   public static String post(String s, String s1, boolean flag) {
      try {
         HttpURLConnection httpurlconnection = (HttpURLConnection)URI.create(s).toURL().openConnection();
         httpurlconnection.setRequestProperty(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36"
         );
         httpurlconnection.setRequestProperty("Content-Type", flag ? "application/json" : "application/x-www-form-urlencoded; charset=UTF-8");
         httpurlconnection.setRequestProperty("Accept", "application/json");
         httpurlconnection.setRequestMethod("POST");
         httpurlconnection.setDoOutput(true);
         httpurlconnection.getOutputStream().write(s1.getBytes(StandardCharsets.UTF_8));
         httpurlconnection.setConnectTimeout(5000);
         httpurlconnection.setReadTimeout(5000);
         httpurlconnection.connect();
         int i = httpurlconnection.getResponseCode();
         InputStream inputstream = i / 100 != 2 && i / 100 != 3 ? httpurlconnection.getErrorStream() : httpurlconnection.getInputStream();
         if (inputstream == null) {
            return null;
         }

         BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(inputstream));
         StringBuilder stringbuilder = new StringBuilder();

         String s2;
         while ((s2 = bufferedreader.readLine()) != null) {
            stringbuilder.append(s2);
         }

         bufferedreader.close();
         httpurlconnection.disconnect();
         return stringbuilder.toString();
      } catch (Exception exception) {
         return null;
      }
   }
   public static String getUuidForUsername(String s) throws IOException {
      try {
         return new JsonParser()
            .parse(
               IOUtils.toString(
                  URI.create("https://api.mojang.com/users/profiles/minecraft/" + s).toURL().openConnection().getInputStream(), StandardCharsets.UTF_8
               )
            )
            .getAsJsonObject()
            .get("id")
            .getAsString();
      } catch (IOException ioexception) {
         throw ioexception;
      } catch (Throwable throwable) {
         throw new IOException(throwable);
      }
   }
}
