package dev.pugrilla.altmanager.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionRefreshPolicyTest {
   private static final long NOW = 1700000000000L;

   public static String token(long expiry) {
      return "header." + Base64.getUrlEncoder().withoutPadding().encodeToString(
         ("{\"exp\":" + expiry + "}").getBytes(StandardCharsets.UTF_8)) + ".signature";
   }

   @Test public void expiredAndNearlyExpiredTokensRefreshBeforeUse() {
      assertTrue(SessionRefreshPolicy.needsRefresh(token(NOW / 1000L - 1L), NOW, NOW));
      assertTrue(SessionRefreshPolicy.needsRefresh(token(NOW / 1000L + 60L), NOW, NOW));
      assertFalse(SessionRefreshPolicy.needsRefresh(token(NOW / 1000L + 3600L), 0L, NOW));
   }

   @Test public void opaqueAndMalformedTokensUseConservativeFallback() {
      assertTrue(SessionRefreshPolicy.needsRefresh("", NOW, NOW));
      assertTrue(SessionRefreshPolicy.needsRefresh("opaque", NOW - 3600000L, NOW));
      assertTrue(SessionRefreshPolicy.needsRefresh("broken.%%%.token", 0L, NOW));
      assertTrue(SessionRefreshPolicy.needsRefresh("opaque", NOW + 1L, NOW));
      assertFalse(SessionRefreshPolicy.needsRefresh("opaque", NOW - 1000L, NOW));
   }
}
