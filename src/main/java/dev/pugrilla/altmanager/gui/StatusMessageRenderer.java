package dev.pugrilla.altmanager.gui;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public final class StatusMessageRenderer {
   private String message;
   private long shownAt;
   private int durationMs;

   public synchronized void setStatus(String message, int durationMs) {
      this.message = message;
      this.shownAt = System.currentTimeMillis();
      this.durationMs = durationMs;
   }

   private int getAlpha() {
      if (this.message == null) {
         return 0;
      }
      long elapsed = System.currentTimeMillis() - this.shownAt - this.durationMs;
      int alpha = elapsed <= 0L ? 255 : (int)Math.max(0L, 255L - 255L * elapsed / 100L);
      if (alpha == 0) {
         this.message = null;
      }
      return alpha;
   }

   public synchronized void draw(int centerX, int y) {
      int alpha = this.getAlpha();
      if (alpha > 0) {
         Minecraft minecraft = Minecraft.getMinecraft();
         GlStateManager.enableBlend();
         minecraft.fontRendererObj.drawStringWithShadow(this.message,
            centerX - minecraft.fontRendererObj.getStringWidth(this.message) / 2.0F, y, new Color(201, 62, 62, alpha).getRGB());
      }
   }

   public synchronized void drawFooter(int centerX, int y, int maxWidth) {
      int alpha = this.getAlpha();
      if (alpha > 0) {
         Minecraft minecraft = Minecraft.getMinecraft();
         String text = minecraft.fontRendererObj.trimStringToWidth(this.message, maxWidth * 2);
         GlStateManager.pushMatrix();
         GlStateManager.scale(0.5F, 0.5F, 0.5F);
         GlStateManager.enableBlend();
         minecraft.fontRendererObj.drawStringWithShadow(text,
            centerX * 2.0F - minecraft.fontRendererObj.getStringWidth(text) / 2.0F, y * 2.0F, new Color(230, 160, 143, alpha).getRGB());
         GlStateManager.popMatrix();
      }
   }
}
