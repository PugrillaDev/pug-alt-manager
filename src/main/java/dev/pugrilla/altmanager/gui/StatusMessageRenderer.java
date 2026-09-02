package dev.pugrilla.altmanager.gui;


import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
public final class StatusMessageRenderer {
   private String message;
   private long shownAt;
   private int durationMs;

   public void setStatus(String s, int i) {
      this.message = s;
      this.shownAt = System.currentTimeMillis();
      this.durationMs = i;
   }

   public void draw(int i, int j) {
      if (this.message != null) {
         byte b0 = 100;
         long k = System.currentTimeMillis();
         int l = k < this.shownAt + this.durationMs
            ? 255
            : (k < this.shownAt + this.durationMs + 100L ? (int)Math.max(0L, 255L - 255L * (k - (this.shownAt + this.durationMs)) / 100L) : 0);
         if (l <= 0) {
            this.message = null;
         } else {
            Minecraft minecraft = Minecraft.getMinecraft();
            GlStateManager.enableBlend();
            minecraft.fontRendererObj
               .drawStringWithShadow(this.message, i - minecraft.fontRendererObj.getStringWidth(this.message) / 2.0F, j, new Color(201, 62, 62, l).getRGB());
         }
      }
   }
}
