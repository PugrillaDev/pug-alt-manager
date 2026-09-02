package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.util.ColorUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
public final class EmptyAccountListEntry implements IGuiListEntry {
   private final String message;

   public EmptyAccountListEntry(String s) {
      this.message = s;
   }

   public void setSelected(int i, int j, int k) {
   }

   public void drawEntry(int l, int i, int j, int k, int i1, int j1, int k1, boolean flag) {
      Minecraft minecraft = Minecraft.getMinecraft();
      minecraft.fontRendererObj
         .drawStringWithShadow(this.message, i + k / 2.0F - minecraft.fontRendererObj.getStringWidth(this.message) / 2.0F, j + 4, ColorUtils.grayscale(170));
   }

   public boolean mousePressed(int i, int j, int k, int l, int i1, int j1) {
      return false;
   }

   public void mouseReleased(int i, int j, int k, int l, int i1, int j1) {
   }
}
