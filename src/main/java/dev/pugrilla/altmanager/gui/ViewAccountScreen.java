package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.LoginResult;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.auth.MinecraftServicesApi;
import dev.pugrilla.altmanager.util.AltManagerUtils;
import dev.pugrilla.altmanager.util.AsyncTaskLock;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
public final class ViewAccountScreen extends GuiScreen {
   private static final Log LOGGER = LogFactory.getLog(ViewAccountScreen.class);
   private final AltManager altManager;
   private final GuiScreen previousScreen;
   private final AbstractAccount account;
   private GuiTextField usernameField;
   private GuiButton setUsernameButton;
   private GuiButton refreshButton;
   private GuiButton banCheckButton;
   private GuiButton copySessionButton;
   private GuiButton copyCodeButton;
   private GuiButton skinSettingsButton;
   private GuiButton deleteButton;
   private GuiButton moveButton;
   private GuiButton confirmButton;
   private final StatusMessageRenderer statusRenderer = new StatusMessageRenderer();
   private final AsyncTaskLock taskLock;
   private String originalUsername;

   public ViewAccountScreen(AltManager altmanager, GuiScreen guiscreen, AbstractAccount AbstractAccount) {
      this.altManager = altmanager;
      this.previousScreen = guiscreen;
      this.account = AbstractAccount;
      this.taskLock = new AsyncTaskLock(altmanager, throwable -> this.statusRenderer.setStatus("Exception occurred, please check console", 5000));
   }

   public void initGui() {
      if (this.usernameField == null) {
         this.usernameField = new GuiTextField(0, this.fontRendererObj, 0, 68, 200, 20);
         this.usernameField.setText(this.originalUsername = this.account.getUsername());
      }

      this.usernameField.xPosition = this.width / 2 - 100;
      this.usernameField.setMaxStringLength(16);
      this.buttonList.clear();
      this.buttonList
         .add(this.setUsernameButton = new GuiButton(0, this.width / 2 - 100 - 50, this.usernameField.yPosition + 24, 98, 20, "Set username"));
      this.buttonList.add(this.skinSettingsButton = new GuiButton(2, this.width / 2 + 2 - 50, this.usernameField.yPosition + 24, 98, 20, "Skin settings"));
      this.buttonList.add(this.refreshButton = new GuiButton(5, this.width / 2 - 100 - 50, this.setUsernameButton.yPosition + 24, 98, 20, "Refresh"));
      this.buttonList.add(this.banCheckButton = new GuiButton(6, this.width / 2 + 2 - 50, this.setUsernameButton.yPosition + 24, 98, 20, "Ban check"));
      this.buttonList.add(this.copySessionButton = new GuiButton(7, this.width / 2 + 2 + 50, this.usernameField.yPosition + 24, 98, 20, "Copy session"));
      this.buttonList.add(this.copyCodeButton = new GuiButton(8, this.width / 2 + 2 + 50, this.setUsernameButton.yPosition + 24, 98, 20, "Copy code"));
      String s = AltManagerUtils.truncate(this.account.getUsername(), 5, true);
      this.buttonList
         .add(
            this.deleteButton = new GuiButton(
               4, this.width / 2 + 2, this.height / 4 + 120 - 24, 98, 20, EnumChatFormatting.RED + "Delete " + EnumChatFormatting.GRAY + s
            )
         );
      this.buttonList.add(this.moveButton = new GuiButton(3, this.width / 2 - 100, this.height / 4 + 120 - 24, 98, 20, "Move Account"));
      this.buttonList.add(this.confirmButton = new GuiButton(1, this.width / 2 - 100, this.height / 4 + 120, "Confirm"));
      this.setVisibilityAndEnabled();
   }

   private void setVisibilityAndEnabled() {
      boolean flag = this.taskLock.isLocked();
      this.setUsernameButton.enabled = !this.usernameField.getText().equals(this.originalUsername)
         && AltManagerUtils.MINECRAFT_USERNAME_PATTERN.matcher(this.usernameField.getText()).matches()
         && !flag;
      this.skinSettingsButton.enabled = this.confirmButton.enabled = this.deleteButton.enabled = this.moveButton.enabled = this.refreshButton.enabled = this.banCheckButton
         .enabled = !flag;
   }

   public void updateScreen() {
      this.setVisibilityAndEnabled();
      this.usernameField.updateCursorCounter();
   }

   public void drawScreen(int i, int j, float f) {
      this.drawDefaultBackground();
      this.drawCenteredString(
         this.fontRendererObj, "View account " + EnumChatFormatting.DARK_GRAY + this.account.getUsername(), this.width / 2, 17, 16777215
      );
      this.statusRenderer.draw(this.width / 2, 37);
      this.drawString(
         this.fontRendererObj,
         "Username " + EnumChatFormatting.DARK_GRAY + "(" + this.usernameField.getText().length() + "/16)",
         this.width / 2 - 100,
         this.usernameField.yPosition - 12,
         10526880
      );
      this.usernameField.drawTextBox();
      super.drawScreen(i, j, f);
   }

   protected void keyTyped(char c0, int i) throws IOException {
      if (!this.usernameField.textboxKeyTyped(c0, i) && i == 1 && this.confirmButton.enabled) {
         this.back();
      }

      this.setVisibilityAndEnabled();
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton == this.confirmButton) {
         this.back();
      } else if (guibutton == this.skinSettingsButton) {
         this.mc.displayGuiScreen(new SkinCustomizationScreen(this, this.account, this.altManager));
      } else if (guibutton == this.setUsernameButton) {
         this.taskLock.execute(this::setNewAccountUsername);
         this.account.getRepository().getAltManager().getStorageManager().setAutoSaveRequired();
      } else if (guibutton == this.deleteButton) {
         this.mc
            .displayGuiScreen(
               new GuiYesNo(
                  (confirmed, id) -> {
                     if (confirmed) {
                        this.getAccount().getRepository().deleteAccount(this.getAccount());
                        this.back();
                     } else {
                        this.mc.displayGuiScreen(this);
                     }
                  },
                  "Are you sure you want to delete account " + EnumChatFormatting.GRAY + this.getAccount().getUsername() + EnumChatFormatting.RESET + "?",
                  "This action is not reversible.",
                  1337
               )
            );
      } else if (guibutton == this.refreshButton) {
         this.taskLock.execute(() -> {
            LoginResult result = this.account.login();
            if (result.isSuccessfulLogin()) {
               this.setStatus(EnumChatFormatting.GREEN + "Account session refreshed");
            } else {
               this.setStatus(result.getResponseType().getDescription());
            }
         });
      } else if (guibutton == this.moveButton) {
         this.mc.displayGuiScreen(new SelectRepositoryScreen(this, this.altManager, repository -> {
            if (repository != null) {
               this.account.getRepository().deleteAccount(this.account);
               repository.addAccount(this.account);
            }
         }));
      } else if (guibutton == this.banCheckButton) {
         this.taskLock.execute(this.account::startBanCheck);
      } else if (guibutton == this.copySessionButton || guibutton == this.copyCodeButton) {
         String s = this.account.getUsername() + ":" + this.account.getUuid().toString().replace("-", "") + ":" + this.account.getAccessToken();
         if (guibutton == this.copyCodeButton) {
            s = AltManagerUtils.encryptString(s);
         }

         GuiScreen.setClipboardString(s);
         this.statusRenderer
            .setStatus(
               EnumChatFormatting.GREEN + "Copied " + (guibutton == this.copyCodeButton ? "encrypted share code" : "account session") + " to clipboard", 1500
            );
      }
   }

   private void setStatus(String s) {
      this.statusRenderer.setStatus(s, 1500);
   }

   protected void mouseClicked(int i, int j, int k) throws IOException {
      this.usernameField.mouseClicked(i, j, k);
      super.mouseClicked(i, j, k);
   }

   private void back() {
      if (!this.taskLock.isLocked()) {
         this.mc.displayGuiScreen(this.previousScreen);
      }
   }

   public AltManager getAltManager() {
      return this.altManager;
   }

   public GuiScreen getLastScreen() {
      return this.previousScreen;
   }

   public AbstractAccount getAccount() {
      return this.account;
   }

   private void setNewAccountUsername() {
      switch (MinecraftServicesApi.getNameChangeStatus(this.getAccount().getAccessToken())) {
         case UNAUTHORIZED:
            this.setStatus("This account is invalid, refresh it and try again.");
            return;
         case RATE_LIMITED:
            this.setStatus("Too many requests, change your IP address or try again later.");
            return;
         case COOLDOWN:
            this.setStatus("This account is on name change cool-down.");
            return;
         case ERROR:
            this.setStatus("An unknown error occurred while checking name change availability.");
            return;
         default:
            String s = this.usernameField.getText();
            int i = MinecraftServicesApi.setProfileName(this.account.getAccessToken(), s);
            switch (i) {
               case -1:
                  this.setStatus("An unknown error occurred setting name.");
                  break;
               case 200:
                  this.account.setUsername(s);
                  this.setStatus(EnumChatFormatting.GREEN + "Successfully set name to " + this.account.getUsername() + ".");
                  break;
               case 401:
                  this.setStatus("This account is invalid, refresh it and try again.");
                  break;
               case 403:
                  this.setStatus("This username is invalid.");
                  break;
               case 429:
                  this.setStatus("Too many requests, change your IP address or try again later.");
                  break;
               default:
                  this.setStatus("Error setting name: " + i);
            }
      }
   }
}
