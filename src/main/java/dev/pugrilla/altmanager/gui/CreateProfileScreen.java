package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.auth.MinecraftServicesApi;
import dev.pugrilla.altmanager.util.AltManagerUtils;
import dev.pugrilla.altmanager.util.AsyncTaskLock;

import java.io.IOException;
import java.util.UUID;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
public final class CreateProfileScreen extends GuiScreen {
   private final GuiScreen previousScreen;
   private final AltManager altManager;
   private final AbstractAccount account;
   private final AccountRepository repository;
   private final StatusMessageRenderer statusRenderer = new StatusMessageRenderer();
   private final AsyncTaskLock taskLock;
   private GuiTextField usernameField;
   private GuiButton createProfileButton;
   private GuiButton backButton;
   private boolean creationSubmitted;

   public CreateProfileScreen(GuiScreen guiscreen, AltManager altmanager, AbstractAccount AbstractAccount, AccountRepository AccountRepository) {
      this.previousScreen = guiscreen;
      this.altManager = altmanager;
      this.account = AbstractAccount;
      this.taskLock = new AsyncTaskLock(altmanager, throwable -> this.statusRenderer.setStatus("An error occurred, please check console", 1500));
      this.repository = AccountRepository;
   }

   public void initGui() {
      this.buttonList.clear();
      Keyboard.enableRepeatEvents(true);
      this.usernameField = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 100, 66, 200, 20);
      this.usernameField.setMaxStringLength(16);
      this.buttonList.add(this.createProfileButton = new GuiButton(2, this.width / 2 - 100, 90, "Create profile"));
      this.createProfileButton.enabled = false;
      this.buttonList
         .add(
            this.backButton = new GuiButton(1, this.width / 2 - 100, this.height / 4 + 86 + 24, I18n.format("gui.back", new Object[0]))
         );
      super.initGui();
   }

   public void onGuiClosed() {
      Keyboard.enableRepeatEvents(false);
   }

   public void updateScreen() {
      this.setButtons();
      this.usernameField.updateCursorCounter();
      super.updateScreen();
   }

   public void drawScreen(int i, int j, float f) {
      this.drawDefaultBackground();
      this.drawCenteredString(this.fontRendererObj, "Create Profile", this.width / 2, 17, 16777215);
      this.usernameField.drawTextBox();
      this.drawString(this.fontRendererObj, "Username", this.width / 2 - 100, 54, 10526880);
      this.statusRenderer.draw(this.width / 2, 42);
      super.drawScreen(i, j, f);
   }

   protected void mouseClicked(int i, int j, int k) throws IOException {
      this.usernameField.mouseClicked(i, j, k);
      super.mouseClicked(i, j, k);
   }

   protected void keyTyped(char c0, int i) throws IOException {
      if (i == 1) {
         this.back();
      } else {
         this.usernameField.textboxKeyTyped(c0, i);
      }
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton.id == 1) {
         this.back();
      } else if (guibutton.id == 2) {
         this.creationSubmitted = true;
         this.taskLock.execute(() -> {
            String username = this.usernameField.getText();
            int responseCode = MinecraftServicesApi.createProfile(this.account.getAccessToken(), username);
            switch (responseCode) {
               case 200:
                  UUID uuid;
                  try {
                     uuid = AltManagerUtils.parseUuid(MinecraftServicesApi.getUuidForUsername(username));
                  } catch (IOException exception) {
                     this.statusRenderer.setStatus("Failed to get UUID. Try adding the account again.", 1500);
                     return;
                  }

                  this.account.setUsername(username);
                  this.account.setUuid(uuid);
                  this.repository.addAccount(this.account);
                  this.altManager.getStorageManager().setAutoSaveRequired();
                  this.back();
                  break;
               case 400:
                  this.statusRenderer.setStatus("This username is invalid, or something went wrong.", 1000);
                  break;
               case 401:
                  this.statusRenderer.setStatus("Unauthorized, try again later.", 1000);
                  break;
               case 429:
                  this.statusRenderer.setStatus("Too many requests, change your IP address or try again later.", 1000);
                  break;
               default:
                  this.statusRenderer.setStatus("Error creating profile: " + responseCode, 1000);
            }
         });
      }

      this.setButtons();
   }

   private void setButtons() {
      boolean flag = !this.taskLock.isLocked();
      this.createProfileButton.enabled = !this.creationSubmitted
         && flag
         && this.usernameField.getText().length() >= 3
         && AltManagerUtils.MINECRAFT_USERNAME_PATTERN.matcher(this.usernameField.getText()).matches();
      this.usernameField.setEnabled(!this.creationSubmitted && flag);
      this.backButton.enabled = flag;
   }

   private void back() {
      this.mc.displayGuiScreen(this.previousScreen);
   }
}
