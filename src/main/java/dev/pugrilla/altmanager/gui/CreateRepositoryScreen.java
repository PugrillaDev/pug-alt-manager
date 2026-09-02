package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.storage.RepositoryEncryption;
import dev.pugrilla.altmanager.storage.StorageManager;
import dev.pugrilla.altmanager.util.AltManagerUtils;
import dev.pugrilla.altmanager.util.ColorUtils;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
public final class CreateRepositoryScreen extends GuiScreen {
   private final GuiScreen previousScreen;
   private final StorageManager storageManager;
   private final AltManager altManager;
   private GuiTextField repositoryNameField;
   private GuiTextField passwordField;

   public CreateRepositoryScreen(GuiScreen guiscreen, AltManager altmanager) {
      this.previousScreen = guiscreen;
      this.storageManager = altmanager.getStorageManager();
      this.altManager = altmanager;
   }

   protected void keyTyped(char c0, int i) throws IOException {
      if (!this.repositoryNameField.textboxKeyTyped(c0, i) && !this.passwordField.textboxKeyTyped(c0, i)) {
         if (i == 1) {
            this.back();
         }
      } else {
         this.checkEnabled();
      }
   }

   private void checkEnabled() {
      ((GuiButton)this.buttonList.get(0)).enabled = AltManagerUtils.isValidRepositoryName(this.repositoryNameField.getText());
   }

   public void initGui() {
      this.buttonList.clear();
      this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 96, "Create"));
      this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 120, "Cancel"));
      if (this.repositoryNameField == null) {
         this.repositoryNameField = new GuiTextField(0, this.fontRendererObj, 0, 68, 200, 20);
      }

      this.repositoryNameField.xPosition = this.width / 2 - 100;
      this.repositoryNameField.setMaxStringLength(32);
      this.repositoryNameField.setFocused(true);
      if (this.passwordField == null) {
         this.passwordField = new GuiTextField(0, this.fontRendererObj, 0, this.repositoryNameField.yPosition + 40, 200, 20);
      }

      this.passwordField.xPosition = this.width / 2 - 100;
      this.passwordField.setMaxStringLength(128);
      this.checkEnabled();
   }

   public void drawScreen(int i, int j, float f) {
      this.drawDefaultBackground();
      this.drawCenteredString(this.fontRendererObj, "Create a new local repository", this.width / 2, 17, 16777215);
      this.drawString(
         this.fontRendererObj,
         "Repository name " + EnumChatFormatting.DARK_GRAY + "(" + this.repositoryNameField.getText().length() + "/" + this.repositoryNameField.getMaxStringLength() + ")",
         this.width / 2 - 100,
         this.repositoryNameField.yPosition - 12,
         10526880
      );
      this.repositoryNameField.drawTextBox();
      this.drawString(
         this.fontRendererObj,
         "Password " + EnumChatFormatting.DARK_GRAY + "(" + this.passwordField.getText().length() + "/" + this.passwordField.getMaxStringLength() + ")",
         this.width / 2 - 100,
         this.passwordField.yPosition - 12,
         this.passwordField.getText().isEmpty() ? ColorUtils.darker(10526880) : 10526880
      );
      this.passwordField.drawTextBox();
      super.drawScreen(i, j, f);
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton.id == 1) {
         this.back();
      } else if (guibutton.id == 0) {
         this.createRepository();
      }
   }

   protected void mouseClicked(int i, int j, int k) throws IOException {
      this.repositoryNameField.mouseClicked(i, j, k);
      this.passwordField.mouseClicked(i, j, k);
      super.mouseClicked(i, j, k);
   }

   public void updateScreen() {
      this.repositoryNameField.updateCursorCounter();
      this.passwordField.updateCursorCounter();
   }

   private void createRepository() {
      String s = this.repositoryNameField.getText();
      AccountRepository AccountRepository = new AccountRepository(s, this.altManager, System.currentTimeMillis(), this.createEncryptionHandler());
      this.storageManager.createRepository(AccountRepository);
      this.altManager.getStorageManager().setSelectedRepository(AccountRepository);
      this.back();
   }

   private RepositoryEncryption createEncryptionHandler() {
      String s = this.passwordField.getText();
      return s.isEmpty() ? new RepositoryEncryption(false) : new RepositoryEncryption(s);
   }

   private void back() {
      this.mc.displayGuiScreen(this.previousScreen);
   }
}
