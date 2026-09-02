package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.storage.RepositoryFileCodec;
import dev.pugrilla.altmanager.util.AltManagerUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import dev.pugrilla.jnafilechooser.api.WindowsFileChooser;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.util.EnumChatFormatting;
public final class ManageRepositoryScreen extends GuiScreen {
   private final GuiScreen previousScreen;
   private final AltManager altManager;
   private final AccountRepository repository;
   private String originalName;
   private GuiTextField nameField;
   private GuiButton clearAllButton;
   private GuiButton exportButton;
   private GuiButton confirmButton;

   public ManageRepositoryScreen(GuiScreen guiscreen, AltManager altmanager, AccountRepository AccountRepository) {
      this.previousScreen = guiscreen;
      this.altManager = altmanager;
      this.repository = Objects.requireNonNull(AccountRepository);
   }

   public void initGui() {
      if (this.nameField == null) {
         this.nameField = new GuiTextField(0, this.fontRendererObj, 0, 68, 200, 20);
         this.nameField.setText(this.originalName = this.repository.getName());
      }

      this.nameField.xPosition = this.width / 2 - 100;
      this.nameField.setMaxStringLength(32);
      this.nameField.setFocused(true);
      this.buttonList.clear();
      int i = this.nameField.yPosition + 24;
      String s = AltManagerUtils.truncate(this.repository.getName(), 5, true);
      this.buttonList.add(new GuiButton(2, this.width / 2 - 100, i, 98, 20, EnumChatFormatting.RED + "Delete " + EnumChatFormatting.GRAY + s));
      this.buttonList
         .add(
            this.clearAllButton = new GuiButton(
               3,
               this.width / 2 + 2,
               i,
               98,
               20,
               EnumChatFormatting.RED + "Clear All" + EnumChatFormatting.GRAY + " (" + this.repository.getAccountCount() + ")"
            )
         );
      this.buttonList.add(this.exportButton = new GuiButton(4, this.width / 2 - 100, i + 24, "Export Repository"));
      this.buttonList.add(this.confirmButton = new GuiButton(0, this.width / 2 - 100, this.height / 4 + 96, "Confirm changes"));
      this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 120, "Cancel"));
      this.setButtonEnabled();
   }

   public void updateScreen() {
      this.setButtonEnabled();
      this.nameField.updateCursorCounter();
   }

   private void setButtonEnabled() {
      this.clearAllButton.enabled = this.repository.getAccountCount() != 0;
      String s = this.nameField.getText();
      this.confirmButton.enabled = AltManagerUtils.isValidRepositoryName(s) && !this.originalName.equals(s);
   }

   protected void mouseClicked(int i, int j, int k) throws IOException {
      this.nameField.mouseClicked(i, j, k);
      super.mouseClicked(i, j, k);
   }

   protected void keyTyped(char c0, int i) throws IOException {
      if (this.nameField.textboxKeyTyped(c0, i)) {
         this.setButtonEnabled();
      } else if (i == 1) {
         this.back();
      }
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton == this.confirmButton) {
         this.repository.setName(this.nameField.getText());
         this.repository.getAltManager().getStorageManager().setAutoSaveRequired();
         this.back();
      } else if (guibutton.id == 1) {
         this.back();
      } else if (guibutton.id == 2) {
         this.mc
            .displayGuiScreen(
               new GuiYesNo(
                  (confirmed, id) -> {
                     if (confirmed) {
                        this.altManager.getStorageManager().deleteRepository(this.repository);
                        this.back();
                     } else {
                        this.mc.displayGuiScreen(this);
                     }
                  },
                  "Are you sure you want to delete repository " + EnumChatFormatting.GRAY + this.repository.getName() + EnumChatFormatting.RESET + "?",
                  "This action is not reversible.",
                  1337
               )
            );
      } else if (guibutton.id == 3) {
         this.mc
            .displayGuiScreen(
               new GuiYesNo(
                  (confirmed, id) -> {
                     if (confirmed) {
                        this.repository.deleteAllAccounts();
                        this.back();
                     } else {
                        this.mc.displayGuiScreen(this);
                     }
                  },
                  "Are you sure you want to clear all accounts from repository "
                     + EnumChatFormatting.GRAY
                     + this.repository.getName()
                     + EnumChatFormatting.RESET
                     + "?",
                  "This action is not reversible.",
                  1337
               )
            );
      } else if (guibutton == this.exportButton) {
         this.altManager.getThreadPool().execute(() -> {
            WindowsFileChooser chooser = new WindowsFileChooser(Objects.requireNonNull(System.getProperty("user.home"), new File("").getAbsolutePath()));
            chooser.setMultiSelectionEnabled(false);
            chooser.setTitle("Export Repository");
            chooser.showSaveDialog(null);
            File[] selectedFiles = chooser.getSelectedFiles();
            if (selectedFiles.length > 0) {
               File destination = selectedFiles[0];
               try {
                  Files.write(destination.toPath(), RepositoryFileCodec.exportRepository(this.repository));
               } catch (Throwable throwable) {
                  this.altManager.getLogger().warn(
                     "Failed to save account repository {} to {}: {}",
                     new Object[]{this.repository.getName(), destination.getAbsolutePath(), throwable}
                  );
               }
            }
         });
      }
   }

   private void back() {
      this.mc.displayGuiScreen(this.previousScreen);
   }

   public void drawScreen(int i, int j, float f) {
      this.drawDefaultBackground();
      this.drawCenteredString(this.fontRendererObj, "Manage local repository " + EnumChatFormatting.DARK_GRAY + this.originalName, this.width / 2, 17, 16777215);
      this.drawString(
         this.fontRendererObj,
         "Repository name " + EnumChatFormatting.DARK_GRAY + "(" + this.nameField.getText().length() + "/32)",
         this.width / 2 - 100,
         this.nameField.yPosition - 12,
         10526880
      );
      this.nameField.drawTextBox();
      super.drawScreen(i, j, f);
   }
}
