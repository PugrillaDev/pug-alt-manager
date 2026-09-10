package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.AltManager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
public final class PlayerHeadRenderer {
   private final String username;
   private final UUID uuid;
   private final AltManager altManager;
   private volatile ResourceLocation skinResource;
   private final AtomicBoolean downloadStarted = new AtomicBoolean();
   private final AtomicInteger loadGeneration = new AtomicInteger();
   private volatile boolean downloadComplete;
   private float alpha = 1.0F;

   public PlayerHeadRenderer(String s, UUID uuid, AltManager altmanager) {
      this.username = s;
      this.uuid = uuid;
      this.altManager = altmanager;
   }

   public void setAlpha(float f) {
      this.alpha = f;
   }

   public void drawHeadScaled(float f, float f1, float f2) {
      float f3 = 1.0F / f;
      GlStateManager.scale(f, f, f);
      this.drawHead((int)(f1 * f3), (int)(f2 * f3));
      GlStateManager.scale(f3, f3, f3);
   }

   public void drawHead(int i, int j) {
      Gui.drawRect(i, j, i + 32, j + 32, -1601138544);
      GlStateManager.color(1.0F, 1.0F, 1.0F, this.alpha);
      this.alpha = 1.0F;
      GlStateManager.enableBlend();
      Minecraft.getMinecraft().getTextureManager().bindTexture(this.skinResource == null ? DefaultPlayerSkin.getDefaultSkin(this.uuid) : this.skinResource);
      Gui.drawModalRectWithCustomSizedTexture(i, j, 32.0F, 32.0F, 32, 32, 256.0F, 256.0F);
      Gui.drawModalRectWithCustomSizedTexture(i, j, 160.0F, 32.0F, 32, 32, 256.0F, 256.0F);
      GlStateManager.disableBlend();
   }

   public void ensureSkinDownloaded() {
      if (this.downloadComplete || !this.downloadStarted.compareAndSet(false, true)) {
         return;
      }

      int generation = this.loadGeneration.get();
      try {
         this.altManager.getThreadPool().execute(() -> this.loadSkinAsync(generation));
      } catch (RuntimeException exception) {
         if (generation == this.loadGeneration.get()) {
            this.downloadComplete = true;
         }

         this.altManager.getLogger().debug("Could not schedule player skin loading", exception);
      }
   }

   private void loadSkinAsync(int generation) {
      try {
         Minecraft minecraft = Minecraft.getMinecraft();
         GameProfile profile = minecraft.getSessionService().fillProfileProperties(new GameProfile(this.uuid, this.username), true);
         if (generation != this.loadGeneration.get()) {
            return;
         }

         Map<Type, MinecraftProfileTexture> textures = minecraft.getSessionService().getTextures(profile, false);
         MinecraftProfileTexture skinTexture = textures.get(Type.SKIN);
         if (skinTexture != null) {
            Minecraft.getMinecraft().addScheduledTask(() -> this.loadSkin(skinTexture, generation));
         }
      } catch (Throwable throwable) {
         this.altManager.getLogger().debug("Using the default player skin after skin loading failed", throwable);
      } finally {
         if (generation == this.loadGeneration.get()) {
            this.downloadComplete = true;
         }
      }
   }

   private void loadSkin(MinecraftProfileTexture skinTexture, int generation) {
      if (generation != this.loadGeneration.get()) {
         return;
      }

      Minecraft.getMinecraft()
         .getSkinManager()
         .loadSkin(skinTexture, Type.SKIN, (type, resource, texture) -> {
            if (generation == this.loadGeneration.get()) {
               this.skinResource = resource;
            }
         });
   }

   public void resetSkin() {
      this.loadGeneration.incrementAndGet();
      this.skinResource = null;
      this.downloadStarted.set(false);
      this.downloadComplete = false;
   }
}
