package dev.pugrilla.altmanager.skin;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SkinProfileCacheTest {
   @Rule public TemporaryFolder temporary = new TemporaryFolder();
   private final UUID id = UUID.fromString("12345678-1234-1234-1234-123456789abc");
   private final String url = "http://textures.minecraft.net/texture/abc123";

   @Test public void cachedTextureSurvivesNewCacheInstanceAndCanBeInvalidated() throws Exception {
      Path path = temporary.newFolder().toPath();
      new SkinProfileCache(path).write(id, new MinecraftProfileTexture(url, Collections.emptyMap()));
      SkinProfileCache restored = new SkinProfileCache(path);
      assertEquals(url, restored.read(id).getUrl());
      assertTrue(restored.isFresh(id, System.currentTimeMillis()));
      restored.invalidate(id);
      assertNull(restored.read(id));
   }

   @Test public void expiredMetadataRemainsUsableWhileRefreshRuns() throws Exception {
      Path path = temporary.newFolder().toPath();
      SkinProfileCache cache = new SkinProfileCache(path);
      cache.write(id, new MinecraftProfileTexture(url, Collections.emptyMap()));
      Files.setLastModifiedTime(path.resolve(id + ".json"), FileTime.fromMillis(1L));
      assertFalse(cache.isFresh(id, System.currentTimeMillis()));
      assertNotNull(cache.read(id));
   }

   @Test public void corruptAndUntrustedCacheEntriesFallBackSafely() throws Exception {
      Path path = temporary.newFolder().toPath();
      SkinProfileCache cache = new SkinProfileCache(path);
      Files.write(path.resolve(id + ".json"), "broken".getBytes(StandardCharsets.UTF_8));
      assertNull(cache.read(id));
      Files.write(path.resolve(id + ".json"), "{\"url\":\"https://example.com/texture/abc\"}".getBytes(StandardCharsets.UTF_8));
      assertNull(cache.read(id));
      assertThrows(java.io.IOException.class, () -> cache.write(id, new MinecraftProfileTexture("https://example.com/skin", Collections.emptyMap())));
   }
}
