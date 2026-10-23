package dev.pugrilla.altmanager.auth;

import org.junit.Test;
import static org.junit.Assert.*;

public class CookieUtilsTest {
   @Test public void shortMalformedRowsDoNotCrash() {
      assertTrue(CookieUtils.parseCookieHeader(".live.com\tTRUE\t/\tTRUE\t0").isEmpty());
   }
   @Test public void httpOnlyAndWindowsLinesAreSupported() {
      assertEquals(2, CookieUtils.parseCookieHeader("#HttpOnly_.login.live.com\tTRUE\t/\tTRUE\t0\tname\tvalue\r\n.live.com\tTRUE\t/\tTRUE\t0\tother\t\r\n").size());
   }
   @Test public void similarlyNamedDomainsAreRejected() {
      assertFalse(CookieUtils.isLiveDomain("evillive.com"));
      assertFalse(CookieUtils.isLiveDomain("live.com.example.com"));
      assertTrue(CookieUtils.isLiveDomain("login.live.com"));
   }
}
