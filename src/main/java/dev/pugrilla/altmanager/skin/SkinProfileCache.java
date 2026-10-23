package dev.pugrilla.altmanager.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;

/** UUID-to-texture metadata. Minecraft's SkinManager caches the PNG by its hash. */
public final class SkinProfileCache {
   private final Path directory;

   public SkinProfileCache(Path directory) {
      this.directory = directory;
   }

   public MinecraftProfileTexture read(UUID uuid) {
      try {
         Path path = this.directory.resolve(uuid + ".json");
         if (Files.size(path) > 8192L) {
            return null;
         }
         JsonObject data = new JsonParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
         String url = data.get("url").getAsString();
         return isTrustedTexture(url) ? new MinecraftProfileTexture(url, Collections.emptyMap()) : null;
      } catch (IOException | RuntimeException ignored) {
         return null;
      }
   }

   public boolean isFresh(UUID uuid, long now) {
      try {
         long modified = Files.getLastModifiedTime(this.directory.resolve(uuid + ".json")).toMillis();
         return now >= modified && now - modified < 86400000L;
      } catch (IOException ignored) {
         return false;
      }
   }

   public void write(UUID uuid, MinecraftProfileTexture texture) throws IOException {
      if (!isTrustedTexture(texture.getUrl())) {
         throw new IOException("Untrusted player skin host");
      }
      Files.createDirectories(this.directory);
      JsonObject data = new JsonObject();
      data.addProperty("url", texture.getUrl());
      Path temporary = Files.createTempFile(this.directory, "skin-", ".tmp");
      try {
         Files.write(temporary, data.toString().getBytes(StandardCharsets.UTF_8));
         Files.move(temporary, this.directory.resolve(uuid + ".json"), StandardCopyOption.REPLACE_EXISTING);
      } finally {
         Files.deleteIfExists(temporary);
      }
   }

   public void invalidate(UUID uuid) throws IOException {
      Files.deleteIfExists(this.directory.resolve(uuid + ".json"));
   }

   private static boolean isTrustedTexture(String url) {
      try {
         URI uri = URI.create(url);
         return ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
            && "textures.minecraft.net".equalsIgnoreCase(uri.getHost()) && uri.getUserInfo() == null
            && uri.getPort() == -1 && uri.getPath().startsWith("/texture/");
      } catch (RuntimeException ignored) {
         return false;
      }
   }
}
