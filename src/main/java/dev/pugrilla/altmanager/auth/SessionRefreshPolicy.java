package dev.pugrilla.altmanager.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Reads expiry only as a refresh hint. The server still validates the token. */
public final class SessionRefreshPolicy {
   private static final long EXPIRY_MARGIN_MS = 120000L;
   private static final long UNKNOWN_TOKEN_MAX_AGE_MS = 3600000L;

   private SessionRefreshPolicy() {
   }

   public static boolean needsRefresh(String token, long lastRefresh, long now) {
      if (token == null || token.isEmpty()) {
         return true;
      }
      try {
         String[] parts = token.split("\\.");
         if (parts.length == 3) {
            JsonObject payload = new JsonParser().parse(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)).getAsJsonObject();
            if (payload.has("exp")) {
               return payload.get("exp").getAsLong() <= (now + EXPIRY_MARGIN_MS) / 1000L;
            }
         }
      } catch (RuntimeException ignored) {
         // Opaque and legacy tokens use a conservative age limit.
      }
      return lastRefresh <= 0L || now < lastRefresh || now - lastRefresh >= UNKNOWN_TOKEN_MAX_AGE_MS;
   }
}
