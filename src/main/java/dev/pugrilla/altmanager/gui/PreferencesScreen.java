package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.AltManager;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
public final class PreferencesScreen extends GuiScreen {
   private final AltManager altManager;
   private final GuiScreen previousScreen;
   private GuiButton doubleClickActionButton;
   private GuiButton multiplayerVisibilityButton;
   private GuiButton loggedInUserButton;
   private GuiButton autoRefreshButton;
   private GuiButton confirmButton;

   public PreferencesScreen(AltManager altmanager, GuiScreen guiscreen) {
      this.altManager = altmanager;
      this.previousScreen = guiscreen;
   }

   public void initGui() {
      this.buttonList.clear();
      this.buttonList.add(this.confirmButton = new GuiButton(0, this.width / 2 - 100, this.height - 40, 200, 20, "Confirm"));
      this.buttonList.add(this.doubleClickActionButton = new GuiButton(1, this.width / 2 - 110, 55, 220, 20, ""));
      this.buttonList.add(this.multiplayerVisibilityButton = new GuiButton(2, this.width / 2 - 110, 79, 220, 20, ""));
      this.buttonList.add(this.loggedInUserButton = new GuiButton(3, this.width / 2 - 110, 103, 220, 20, ""));
      this.buttonList.add(this.autoRefreshButton = new GuiButton(4, this.width / 2 - 110, 127, 220, 20, ""));
      this.setButtonText();
      super.initGui();
   }

   private void setButtonText() {
      this.doubleClickActionButton.displayString = "Account Double Click: "
         + EnumChatFormatting.GRAY
         + this.altManager.getStorageManager().getAccountDoubleClickAction().getName();
      this.multiplayerVisibilityButton.displayString = "Show Multiplayer Button: " + this.formatBooleanState(this.altManager.getStorageManager().isMultiplayerButtonVisible());
      this.loggedInUserButton.displayString = "Logged In As Text: " + this.formatBooleanState(this.altManager.getStorageManager().isShowLoggedInUser());
      this.autoRefreshButton.displayString = "Auto-Refresh Session: " + this.formatBooleanState(this.altManager.getStorageManager().isAutoRefreshSession());
   }

   private String formatBooleanState(boolean flag) {
      return flag ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
   }

   public void drawScreen(int i, int j, float f) {
      this.drawDefaultBackground();
      this.altManager.getMainScreen().drawAccountManagerHeader("Preferences", this.width);
      this.altManager
         .getMainScreen()
         .drawSmallStringWithShadow(
            EnumChatFormatting.GRAY + "Maintained by " + EnumChatFormatting.RESET + "PugrillaDev" + EnumChatFormatting.GRAY + ". Not for sale.",
            2.0F,
            this.height - 6,
            -1
         );
      super.drawScreen(i, j, f);
      if (this.doubleClickActionButton.isMouseOver()) {
         this.drawCreativeTabHoveringText(
            "Double-clicking an account in your list will "
               + EnumChatFormatting.GRAY
               + this.altManager.getStorageManager().getAccountDoubleClickAction().getDescription(),
            i,
            j
         );
      } else if (this.loggedInUserButton.isMouseOver()) {
         this.drawCreativeTabHoveringText("When enabled, your username will be shown in the main menu, single-player and multi-player screens in the top right.", i, j);
      } else if (this.multiplayerVisibilityButton.isMouseOver()) {
         this.drawCreativeTabHoveringText(
            "When enabled, a multi-player button will be shown on bottom right of the account manager screen, and account manager button in the multi-player and disconnect screen.",
            i,
            j
         );
      } else if (this.autoRefreshButton.isMouseOver()) {
         this.drawCreativeTabHoveringText("Automatically refresh accounts when the session token hasn't been updated in 24 hours.", i, j);
      }
   }

   public void updateScreen() {
      this.setButtonText();
   }

   protected void keyTyped(char c0, int i) throws IOException {
      if (i == 1) {
         this.mc.displayGuiScreen(this.previousScreen);
      }
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton == this.confirmButton) {
         this.mc.displayGuiScreen(this.previousScreen);
      } else {
         if (guibutton == this.doubleClickActionButton) {
            this.altManager
               .getStorageManager()
               .setAccountDoubleClickAction(
                  AccountDoubleClickAction.values()[(this.altManager.getStorageManager().getAccountDoubleClickAction().ordinal() + 1) % AccountDoubleClickAction.values().length]
               );
         }

         if (guibutton == this.multiplayerVisibilityButton) {
            this.altManager.getStorageManager().setMultiplayerButtonVisible(!this.altManager.getStorageManager().isMultiplayerButtonVisible());
         }

         if (guibutton == this.loggedInUserButton) {
            this.altManager.getStorageManager().setShowLoggedInUser(!this.altManager.getStorageManager().isShowLoggedInUser());
         }

         if (guibutton == this.autoRefreshButton) {
            this.altManager.getStorageManager().setAutoRefreshSession(!this.altManager.getStorageManager().isAutoRefreshSession());
         }

         this.altManager.getStorageManager().setAutoSaveRequired();
         this.setButtonText();
      }
   }
}
