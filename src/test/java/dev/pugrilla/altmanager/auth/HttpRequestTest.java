package dev.pugrilla.altmanager.auth;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpRequestTest {
   @Test public void errorStatusAndBodyArePreservedWithoutThrowingBeforeStatusCheck() throws Exception {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/expired", exchange -> {
         byte[] body = "{\"error\":\"expired\"}".getBytes(StandardCharsets.UTF_8);
         exchange.sendResponseHeaders(401, body.length);
         exchange.getResponseBody().write(body);
         exchange.close();
      });
      server.start();
      try {
         MinecraftServicesApi.Response result = MinecraftServicesApi.request("GET", "http://127.0.0.1:" + server.getAddress().getPort() + "/expired", "dummy", null, null);
         assertEquals(401, result.status);
         assertTrue(result.body.contains("expired"));
      } finally {
         server.stop(0);
      }
   }

   @Test public void redirectsDoNotForwardCredentialsToAnotherEndpoint() throws Exception {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      AtomicReference<String> leaked = new AtomicReference<>();
      server.createContext("/redirect", exchange -> {
         exchange.getResponseHeaders().add("Location", "/other");
         exchange.sendResponseHeaders(302, -1);
         exchange.close();
      });
      server.createContext("/other", exchange -> {
         leaked.set(exchange.getRequestHeaders().getFirst("Authorization"));
         exchange.sendResponseHeaders(200, -1);
         exchange.close();
      });
      server.start();
      try {
         MinecraftServicesApi.Response result = MinecraftServicesApi.request("GET", "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect", "dummy", null, null);
         assertEquals(302, result.status);
         assertNull(leaked.get());
      } finally {
         server.stop(0);
      }
   }
}
