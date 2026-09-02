package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.auth.MinecraftServicesApi;
import dev.pugrilla.altmanager.skin.RandomSkinProvider;
import dev.pugrilla.altmanager.skin.SkinData;
import dev.pugrilla.altmanager.skin.SkinVariant;
import dev.pugrilla.altmanager.util.AltManagerUtils;
import dev.pugrilla.altmanager.util.AsyncTaskLock;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import dev.pugrilla.jnafilechooser.api.WindowsFileChooser;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;
public final class SkinCustomizationScreen extends GuiScreen {
   private final GuiScreen previousScreen;
   private final AbstractAccount account;
   private final AltManager altManager;
   private final StatusMessageRenderer statusRenderer = new StatusMessageRenderer();
   private final AsyncTaskLock taskLock;
   private boolean fileMode = true;
   private GuiTextField inputField;
   private GuiButton randomButton;
   private GuiButton updateButton;
   private GuiButton confirmButton;
   private GuiButton browseButton;
   private GuiButton variantButton;
   private GuiButton inputTypeButton;

   public SkinCustomizationScreen(GuiScreen guiscreen, AbstractAccount AbstractAccount, AltManager altmanager) {
      this.previousScreen = guiscreen;
      this.account = AbstractAccount;
      this.altManager = altmanager;
      this.taskLock = new AsyncTaskLock(altmanager, throwable -> this.statusRenderer.setStatus("An error occurred, please check console", 5000));
   }

   public void initGui() {
      if (this.inputField == null) {
         this.inputField = new GuiTextField(0, this.fontRendererObj, 0, 0, 200, 20);
         this.inputField.setText(this.altManager.getStorageManager().getLastSkinFilePath());
      }

      this.inputField.yPosition = 96;
      this.inputField.xPosition = this.width / 2 - 100;
      this.inputField.setMaxStringLength(1000);
      this.inputField.setFocused(true);
      this.buttonList.clear();
      int i = this.inputField.yPosition + 26;
      this.buttonList.add(this.variantButton = new GuiButton(4, this.width / 2 - 100, i, 98, 20, ""));
      this.buttonList.add(this.inputTypeButton = new GuiButton(5, this.width / 2 + 2, i, 98, 20, ""));
      this.buttonList.add(this.randomButton = new GuiButton(0, this.width / 2 - 100, i + 24, 98, 20, "Random"));
      this.buttonList.add(this.updateButton = new GuiButton(1, this.width / 2 + 2, i + 24, 98, 20, "Update"));
      this.buttonList.add(this.confirmButton = new GuiButton(2, this.width / 2 - 100, this.height - 40, 200, 20, "Confirm"));
      this.buttonList.add(this.browseButton = new GuiButton(3, 0, 0, 20, 20, "..."));
      this.setButtonStates();
      this.browseButton.xPosition = this.inputField.xPosition + this.inputField.width + 6;
      this.browseButton.yPosition = this.inputField.yPosition;
   }

   private void setButtonStates() {
      boolean flag = this.taskLock.isLocked();
      this.variantButton.displayString = "Variant: " + EnumChatFormatting.GRAY + this.altManager.getStorageManager().getSkinVariant().getName();
      this.inputTypeButton.displayString = "Type: " + EnumChatFormatting.GRAY + (this.fileMode ? "File" : "Steal");
      this.inputField.setEnabled(this.browseButton.enabled = !flag);
      this.confirmButton.enabled = !flag;
      this.randomButton.enabled = !flag;
      this.updateButton.enabled = !flag
         && this.inputField.getText().length() > 1
         && (this.fileMode || AltManagerUtils.MINECRAFT_USERNAME_PATTERN.matcher(this.inputField.getText()).matches());
      this.browseButton.visible = this.fileMode;
   }

   protected void keyTyped(char c0, int i) throws IOException {
      if (!this.inputField.textboxKeyTyped(c0, i) && i == 1) {
         this.back();
      } else {
         this.setButtonStates();
      }
   }

   public void drawScreen(int i, int j, float f) {
      this.drawDefaultBackground();
      this.drawCenteredString(
         this.fontRendererObj, "Customize skin for " + EnumChatFormatting.DARK_GRAY + this.account.getUsername(), this.width / 2, 17, 16777215
      );
      PlayerHeadRenderer PlayerHeadRenderer = this.account.getAndInitPlayerHead();
      PlayerHeadRenderer.ensureSkinDownloaded();
      PlayerHeadRenderer.drawHead(this.width / 2 - 16, 31);
      this.statusRenderer.draw(this.width / 2, 70);
      this.drawString(
         this.fontRendererObj,
         this.fileMode ? "Skin file path" : "Username to steal skin from",
         this.width / 2 - 100,
         this.inputField.yPosition - 12,
         10526880
      );
      this.inputField.drawTextBox();
      super.drawScreen(i, j, f);
   }

   public void updateScreen() {
      this.inputField.updateCursorCounter();
      this.setButtonStates();
   }

   protected void mouseClicked(int i, int j, int k) throws IOException {
      this.inputField.mouseClicked(i, j, k);
      super.mouseClicked(i, j, k);
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton == this.confirmButton) {
         this.back();
      } else {
         if (guibutton == this.browseButton) {
            this.altManager.getThreadPool().execute(() -> {
               WindowsFileChooser chooser = new WindowsFileChooser(this.altManager.getStorageManager().getLastSkinFilePath());
               chooser.setMultiSelectionEnabled(false);
               chooser.setTitle("Select Skin File");
               chooser.setMaxNumberOfFiles(32);
               chooser.showOpenDialog(null);
               File[] files = chooser.getSelectedFiles();
               if (files.length > 0) {
                  String path = files[0].getAbsolutePath();
                  this.inputField.setText(path);
                  this.altManager.getStorageManager().setLastSkinFilePath(path);
               }
            });
         } else if (guibutton == this.variantButton) {
            this.altManager
               .getStorageManager()
               .setSkinVariant(SkinVariant.values()[(this.altManager.getStorageManager().getSkinVariant().ordinal() + 1) % SkinVariant.values().length]);
         } else if (guibutton == this.updateButton) {
            if (this.fileMode) {
               this.taskLock.execute(this::setSkinFromFile);
            } else {
               this.taskLock.execute(this::setSkinFromAccount);
            }
         } else if (guibutton == this.randomButton) {
            this.taskLock.execute(this::setRandomSkin);
         } else if (guibutton == this.inputTypeButton) {
            if (this.fileMode) {
               this.altManager.getStorageManager().setLastSkinFilePath(this.inputField.getText());
            }

            this.fileMode = !this.fileMode;
            this.inputField.setText(this.fileMode ? this.altManager.getStorageManager().getLastSkinFilePath() : "");
         }

         this.setButtonStates();
      }
   }

   private void setSkinFromAccount() {
      String s = this.inputField.getText();

      UUID uuid;
      try {
         uuid = Objects.requireNonNull(AltManagerUtils.parseUuid(MinecraftServicesApi.getUuidForUsername(s)));
      } catch (Throwable throwable) {
         this.statusRenderer.setStatus("Invalid account username", 2500);
         return;
      }

      GameProfile gameprofile = MinecraftServer.getServer().getMinecraftSessionService().fillProfileProperties(new GameProfile(uuid, s), true);
      Map map = this.mc.getSessionService().getTextures(gameprofile, false);
      MinecraftProfileTexture minecraftprofiletexture = (MinecraftProfileTexture)map.get(Type.SKIN);
      Objects.requireNonNull(minecraftprofiletexture);
      String s1 = minecraftprofiletexture.getMetadata("model");
      SkinVariant skinVariant = s1 == null ? SkinVariant.CLASSIC : (s1.equalsIgnoreCase("slim") ? SkinVariant.SLIM : SkinVariant.CLASSIC);
      this.setStatusFromResponseCode(MinecraftServicesApi.setSkinFromUrl(this.account.getAccessToken(), minecraftprofiletexture.getUrl(), skinVariant));
   }

   private void setSkinFromFile() {
      File file1 = new File(this.inputField.getText());

      byte[] abyte;
      try {
         abyte = Files.readAllBytes(file1.toPath());
         if (abyte.length < 16) {
            throw new IOException("Skin file too small");
         }
      } catch (Throwable throwable) {
         this.altManager.getLogger().warn("Failed to load skin from file", throwable);
         this.setStatus("Failed to load skin from file: " + Objects.requireNonNull(throwable.getMessage(), "No Message"));
         return;
      }

      this.setStatusFromResponseCode(MinecraftServicesApi.setSkinFromFile(this.account.getAccessToken(), abyte, file1, this.altManager.getStorageManager().getSkinVariant()));
   }

   private void setRandomSkin() {
      SkinData SkinData = RandomSkinProvider.getRandomSkin();
      if (SkinData == null) {
         this.setStatus("Failed to fetch random skin.");
      } else {
         this.setStatusFromResponseCode(MinecraftServicesApi.setSkinFromUrl(this.account.getAccessToken(), SkinData.getSkinUrl(), SkinVariant.fromApiName(SkinData.getVariant())));
      }
   }

   private void setStatusFromResponseCode(int i) {
      switch (i) {
         case -1:
            this.setStatus("An unknown error occurred.");
            break;
         case 200:
            this.setStatus(EnumChatFormatting.GREEN + "Successfully updated player skin.");
            this.altManager.getThreadPool().execute(() -> {
               try {
                  Thread.sleep(1500L);
               } catch (InterruptedException ignored) {
               }

               this.mc.addScheduledTask(this.account::resetSkin);
            });
            break;
         case 400:
            this.setStatus("Bad request, try again?");
            break;
         case 401:
            this.setStatus("This account is invalid, refresh the session and try again.");
            break;
         case 403:
            this.setStatus("This skin is invalid.");
            break;
         case 429:
            this.setStatus("Too many requests, change your IP address or try again later.");
            break;
         default:
            this.setStatus("Error setting skin: " + i);
      }
   }

   private void setStatus(String s) {
      this.statusRenderer.setStatus(s, 2500);
   }

   private void back() {
      this.mc.displayGuiScreen(this.previousScreen);
      if (this.fileMode) {
         this.altManager.getStorageManager().setLastSkinFilePath(this.inputField.getText());
      }
   }
}
