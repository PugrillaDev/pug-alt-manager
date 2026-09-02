package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AccountType;
import dev.pugrilla.altmanager.util.ColorUtils;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.EnumChatFormatting;
class AccountTypeButton {
   private final AddAccountScreen parent;
   private final int x;
   private final int y;
   private final AccountType accountType;
   private final int textWidth;

   AccountTypeButton(AddAccountScreen parent, int x, int y, AccountType accountType) {
      this.parent = parent;
      this.x = x;
      this.y = y;
      this.accountType = accountType;
      this.textWidth = parent.mc.fontRendererObj.getStringWidth(this.getName());
   }

   public AccountType getAccountType() {
      return this.accountType;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public String getName() {
      return this.accountType.getName();
   }

   public boolean isSelected() {
      return this.parent.getAltManager().getStorageManager().getSelectedAddAccountType() == this.accountType;
   }

   public void draw(int i, int j) {
      String object = this.getName();
      boolean flag = this.isHovered(i, j);
      short short1 = 160;
      if (this.isSelected()) {
         short1 += 65;
      }

      if (flag) {
         short1 += this.isSelected() ? 30 : 35;
      }

      this.parent
         .mc
         .fontRendererObj
         .drawStringWithShadow((!this.isSelected() && !flag ? "" : EnumChatFormatting.UNDERLINE) + object, this.x, this.y, ColorUtils.grayscale(short1));
   }

   private boolean isHovered(int i, int j) {
      return j >= this.y
         && j <= this.y + this.parent.getFontRenderer().FONT_HEIGHT
         && i >= this.x
         && i <= this.x + this.textWidth;
   }

   public void mouseClicked(int i, int j, int k) {
      if (k == 0 && this.isHovered(i, j) && !this.isSelected()) {
         this.parent.getAltManager().getStorageManager().setSelectedAddAccountType(this.accountType);
         new GuiButton(0, 0, 0, null).playPressSound(this.parent.mc.getSoundHandler());
         this.parent.refreshAccountTypeControls();
      }
   }
}
