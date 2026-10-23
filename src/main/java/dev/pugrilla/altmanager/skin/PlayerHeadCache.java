package dev.pugrilla.altmanager.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import dev.pugrilla.altmanager.AltManager;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public final class PlayerHeadCache {
   private final AltManager manager;
   private final SkinProfileCache disk;
   private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

   private static final class Entry {
      private volatile ResourceLocation resource;
      private final java.util.concurrent.atomic.AtomicBoolean loading = new java.util.concurrent.atomic.AtomicBoolean();
      private volatile long nextCheck;
      private volatile int generation;
      private boolean invalidated;
   }

   public PlayerHeadCache(AltManager manager) {
      this.manager = manager;
      this.disk = new SkinProfileCache(manager.getStorageManager().getDirectory().toPath().resolve("heads"));
   }

   public ResourceLocation get(UUID uuid, String username) {
      Entry entry = this.entries.computeIfAbsent(uuid, key -> new Entry());
      if (System.currentTimeMillis() >= entry.nextCheck && entry.loading.compareAndSet(false, true)) {
         int generation = entry.generation;
         try {
            this.manager.getSkinThreadPool().execute(() -> this.load(uuid, username, entry, generation));
         } catch (RuntimeException failure) {
            entry.nextCheck = System.currentTimeMillis() + 300000L;
            entry.loading.set(false);
         }
      }
      return entry.resource;
   }

   private void load(UUID uuid, String username, Entry entry, int generation) {
      long retry = 300000L;
      try {
         MinecraftProfileTexture cached;
         synchronized (entry) {
            if (generation != entry.generation) {
               return;
            }
            cached = entry.invalidated ? null : this.disk.read(uuid);
         }
         if (cached != null) {
            this.loadTexture(cached, entry, generation);
            if (this.disk.isFresh(uuid, System.currentTimeMillis())) {
               retry = 86400000L;
               return;
            }
         }
         Minecraft minecraft = Minecraft.getMinecraft();
         GameProfile profile = minecraft.getSessionService().fillProfileProperties(new GameProfile(uuid, username), true);
         MinecraftProfileTexture texture = minecraft.getSessionService().getTextures(profile, false).get(Type.SKIN);
         if (texture != null) {
            synchronized (entry) {
               if (generation != entry.generation) {
                  return;
               }
               try {
                  this.disk.write(uuid, texture);
               } catch (IOException failure) {
                  this.manager.getLogger().debug("Could not cache player skin metadata");
               }
               entry.invalidated = false;
            }
            this.loadTexture(texture, entry, generation);
            retry = 86400000L;
         }
      } catch (Exception failure) {
         this.manager.getLogger().debug("Player skin unavailable; keeping cached/default head");
      } finally {
         synchronized (entry) {
            if (generation == entry.generation) {
               entry.loading.set(false);
               entry.nextCheck = System.currentTimeMillis() + retry;
            }
         }
      }
   }

   private void loadTexture(MinecraftProfileTexture texture, Entry entry, int generation) {
      Minecraft minecraft = Minecraft.getMinecraft();
      minecraft.addScheduledTask(() -> {
         synchronized (entry) {
            if (generation != entry.generation) {
               return;
            }
            minecraft.getSkinManager().loadSkin(texture, Type.SKIN, (type, resource, loaded) -> {
               synchronized (entry) {
                  if (generation == entry.generation) {
                     entry.resource = resource;
                  }
               }
            });
         }
      });
   }

   public void invalidate(UUID uuid) {
      Entry entry = this.entries.computeIfAbsent(uuid, key -> new Entry());
      synchronized (entry) {
         ++entry.generation;
         entry.invalidated = true;
         entry.loading.set(false);
         entry.nextCheck = 0L;
      }
      this.manager.getSkinThreadPool().execute(() -> {
         synchronized (entry) {
            if (!entry.invalidated) {
               return;
            }
            try {
               this.disk.invalidate(uuid);
            } catch (IOException failure) {
               this.manager.getLogger().debug("Could not invalidate player head cache");
            }
         }
      });
   }
}
