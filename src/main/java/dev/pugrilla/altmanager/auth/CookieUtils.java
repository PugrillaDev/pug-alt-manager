package dev.pugrilla.altmanager.auth;


import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
public final class CookieUtils {
   public static final String MINECRAFT_SERVICES_BASE_URL = "https://api.minecraftservices.com/";
   public static List<Cookie> parseCookieHeader(String s) {
      String[] astring = splitLines(s);
      ArrayList arraylist = new ArrayList();

      for (String s1 : astring) {
         if (!s1.startsWith("#") || s1.startsWith("#HttpOnly_")) {
            String[] astring1 = s1.replace("#HttpOnly_", "").split("\t", -1);
            if (astring1.length >= 6) {
               String s2 = astring1[0];
               if (isLiveDomain(s2)) {
                  arraylist.add(new Cookie(astring1[5], astring1.length == 6 ? "" : astring1[6]));
               }
            }
         }
      }

      return arraylist;
   }
   public static boolean isLiveDomain(String domain) {
      String normalized = domain.replace("#HttpOnly_", "").toLowerCase(java.util.Locale.ROOT);
      return normalized.equals("live.com") || normalized.endsWith(".live.com");
   }

   public static String[] splitLines(String s) {
      return !s.contains("\n") ? new String[]{s} : s.split("\r?\n");
   }
   public static void configureConnection(HttpURLConnection httpurlconnection) {
      httpurlconnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0");
      httpurlconnection.setConnectTimeout(5000);
      httpurlconnection.setReadTimeout(5000);
   }
}
