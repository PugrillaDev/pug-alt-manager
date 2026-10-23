package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.AltManager;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

public final class PlayerHeadRenderer {
   private final String username;
   private final UUID uuid;
   private final AltManager altManager;
   private ResourceLocation skinResource;

   public PlayerHeadRenderer(String username, UUID uuid, AltManager manager) {
      this.username = username;
      this.uuid = uuid;
      this.altManager = manager;
   }

   public void drawHeadScaled(float scale, float x, float y) {
      GlStateManager.pushMatrix();
      GlStateManager.scale(scale, scale, scale);
      this.drawHead((int)(x / scale), (int)(y / scale));
      GlStateManager.popMatrix();
   }

   public void drawHead(int x, int y) {
      Gui.drawRect(x, y, x + 32, y + 32, -1601138544);
      // Fade the row background, never the face and hat separately: doing so
      // makes opaque hat pixels translucent and exposes the base face underneath.
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableBlend();
      Minecraft.getMinecraft().getTextureManager().bindTexture(this.skinResource == null ? DefaultPlayerSkin.getDefaultSkin(this.uuid) : this.skinResource);
      Gui.drawModalRectWithCustomSizedTexture(x, y, 32.0F, 32.0F, 32, 32, 256.0F, 256.0F);
      Gui.drawModalRectWithCustomSizedTexture(x, y, 160.0F, 32.0F, 32, 32, 256.0F, 256.0F);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.disableBlend();
   }

   public void ensureSkinDownloaded() {
      this.skinResource = this.altManager.getPlayerHeadCache().get(this.uuid, this.username);
   }

   public void resetSkin() {
      this.altManager.getPlayerHeadCache().invalidate(this.uuid);
   }
}
