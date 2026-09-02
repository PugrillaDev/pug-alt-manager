package dev.pugrilla.altmanager.skin;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.io.IOUtils;
public final class RandomSkinProvider {
   public static SkinData getRandomSkin() {
      try {
         String s = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:129.0) Gecko/20100101 Firefox/129.0";
         HttpsURLConnection httpsurlconnection = (HttpsURLConnection)URI.create("https://api.novaskin.me/v2/random").toURL().openConnection();
         httpsurlconnection.setRequestProperty("User-Agent", s);
         httpsurlconnection.setConnectTimeout(5000);
         String s1 = IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8);
         JsonObject jsonobject = new JsonParser().parse(s1).getAsJsonObject();
         JsonArray jsonarray = jsonobject.getAsJsonArray("posts");
         ArrayList arraylist = new ArrayList();

         for (JsonElement jsonelement : jsonarray) {
            if (jsonelement.isJsonObject()) {
               JsonObject jsonobject1 = jsonelement.getAsJsonObject();
               String s2 = jsonobject1.get("model").getAsString();
               if (s2.equals("player")) {
                  arraylist.add(jsonelement);
               }
            }
         }

         if (arraylist.isEmpty()) {
            return null;
         }

         JsonObject jsonobject2 = ((JsonElement)arraylist.get(ThreadLocalRandom.current().nextInt(arraylist.size()))).getAsJsonObject();
         String s3 = jsonobject2.get("id").getAsString();
         httpsurlconnection = (HttpsURLConnection)URI.create("https://api.novaskin.me/v2/post/" + s3).toURL().openConnection();
         httpsurlconnection.setRequestProperty("User-Agent", s);
         httpsurlconnection.setConnectTimeout(5000);
         s1 = IOUtils.toString(httpsurlconnection.getInputStream(), StandardCharsets.UTF_8);
         jsonobject = new JsonParser().parse(s1).getAsJsonObject();
         String s4 = jsonobject.get("texture").getAsString();
         return new SkinData("https://t.novaskin.me/" + s4, "classic");
      } catch (IOException ioexception) {
         return null;
      }
   }
}
