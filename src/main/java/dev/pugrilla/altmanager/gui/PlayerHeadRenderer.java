package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.AltManager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
public final class PlayerHeadRenderer {
   private final String username;
   private final UUID uuid;
   private final AltManager altManager;
   private ResourceLocation skinResource;
   private MinecraftProfileTexture skinTexture;
   private GameProfile gameProfile;
   private boolean downloadStarted;
   private boolean downloadComplete;
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
      Minecraft minecraft = Minecraft.getMinecraft();
      if (!this.downloadComplete) {
         if (!this.downloadStarted) {
            this.altManager.getThreadPool().execute(() -> {
               try {
                  this.gameProfile = MinecraftServer.getServer()
                     .getMinecraftSessionService()
                     .fillProfileProperties(new GameProfile(this.uuid, this.username), true);
               } catch (Throwable throwable) {
                  this.downloadStarted = false;
               }
            });
            this.downloadStarted = true;
         }

         if (this.gameProfile != null) {
            Map map = minecraft.getSessionService().getTextures(this.gameProfile, false);
            MinecraftProfileTexture minecraftprofiletexture = (MinecraftProfileTexture)map.get(Type.SKIN);
            if (minecraftprofiletexture != null) {
               this.skinTexture = minecraftprofiletexture;
               this.loadSkin();
               this.downloadComplete = true;
            }
         }
      }
   }

   private void loadSkin() {
      Minecraft.getMinecraft().getSkinManager().loadSkin(this.skinTexture, Type.SKIN, (type, resource, texture) -> this.skinResource = resource);
   }

   public void resetSkin() {
      this.skinResource = null;
      this.gameProfile = null;
      this.downloadStarted = false;
      this.downloadComplete = false;
   }
}
