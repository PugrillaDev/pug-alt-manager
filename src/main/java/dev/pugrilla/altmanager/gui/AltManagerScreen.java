package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.AccountSortMode;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.storage.StorageManager;
import dev.pugrilla.altmanager.util.ColorUtils;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
public final class AltManagerScreen extends GuiScreen {
   private final StorageManager storageManager;
   private final AltManager altManager;
   private AccountListWidget accountList;
   private GuiButton cancelButton;
   private GuiButton loginButton;
   private GuiButton viewButton;
   private GuiButton addButton;
   private GuiButton createRepositoryButton;
   private GuiButton switchRepositoryButton;
   private GuiButton manageRepositoryButton;
   private GuiButton preferencesButton;
   private GuiButton moveSelectedButton;
   private GuiButton deleteSelectedButton;
   private GuiButton multiplayerButton;
   private GuiButton unlockButton;
   private GuiButton sortButton;
   private GuiTextField passwordField;
   private GuiTextField searchField;
   private AccountRepository previousRepository;
   private final StatusMessageRenderer statusRenderer = new StatusMessageRenderer();
   private List<AbstractAccount> multiSelectionAccounts = new ArrayList<>();

   public AltManagerScreen(StorageManager StorageManager, AltManager altmanager) {
      this.storageManager = StorageManager;
      this.altManager = altmanager;
   }

   public void initGui() {
      if (this.accountList == null) {
         this.accountList = new AccountListWidget(this.mc, this.width, this.height, this.altManager.getStorageManager());
         this.accountList.registerScrollButtons(7, 8);
      }

      this.invalidateAllViewingLists();
      this.resetButtons();
   }

   public void onGuiClosed() {
      this.multiSelectionAccounts.clear();
   }

   public List<AbstractAccount> getMultiSelectionAccounts() {
      return this.multiSelectionAccounts;
   }

   private void resetButtons() {
      this.buttonList.clear();
      this.loginButton = new GuiButton(7, this.width / 2 - 154, this.height - 28, 70, 20, "Login");
      this.buttonList.add(this.loginButton);
      this.addButton = new GuiButton(2, this.width / 2 + 4, this.height - 28, 70, 20, "Add");
      this.buttonList.add(this.addButton);
      this.viewButton = new GuiButton(8, this.width / 2 - 74, this.height - 28, 70, 20, "View");
      this.buttonList.add(this.viewButton);
      this.buttonList.add(this.cancelButton = new GuiButton(0, this.width / 2 + 4 + 76, this.height - 28, 75, 20, "Cancel"));
      this.manageRepositoryButton = new GuiButton(1, this.width / 2 - 154, this.height - 52, 110, 20, "Manage repository");
      this.buttonList.add(this.manageRepositoryButton);
      this.buttonList.add(this.switchRepositoryButton = new GuiButton(6, this.width / 2 - 40, this.height - 52, 80, 20, ""));
      this.createRepositoryButton = new GuiButton(4, this.width / 2 + 44, this.height - 52, 110, 20, "");
      if (this.passwordField == null) {
         this.passwordField = new GuiTextField(0, this.fontRendererObj, 0, 0, 200, 20);
         this.passwordField.setMaxStringLength(128);
      }

      if (this.searchField == null) {
         this.searchField = new GuiTextField(1, this.fontRendererObj, 0, 0, 100, 20);
         this.searchField.setMaxStringLength(32);
         this.searchField.setFocused(false);
         this.searchField.setText(this.altManager.getStorageManager().getSearchTerm());
      }

      this.buttonList.add(this.unlockButton = new GuiButton(11, this.width / 2 - 55, 81, 110, 20, "Unlock Repository"));
      this.passwordField.xPosition = this.width / 2 - 100;
      this.passwordField.yPosition = this.unlockButton.yPosition - 25;
      this.searchField.xPosition = this.width - 4 - this.searchField.width;
      this.searchField.yPosition = 4;
      this.buttonList.add(this.createRepositoryButton);
      this.buttonList.add(this.preferencesButton = new GuiButton(9, 3, 4, 90, 20, "Preferences"));
      this.buttonList.add(this.deleteSelectedButton = new GuiButton(13, 3, 4, 60, 20, EnumChatFormatting.RED + "Delete"));
      this.buttonList.add(this.moveSelectedButton = new GuiButton(14, 65, 4, 50, 20, "Move"));
      this.buttonList
         .add(
            this.multiplayerButton = new GuiButton(
               10, this.width - 108, this.height - 28, 100, 20, I18n.format("menu.multiplayer", new Object[0])
            )
         );
      this.buttonList.add(this.sortButton = new GuiButton(12, 8, this.height - 28, 110, 20, ""));
      this.accountList.setDimensions(this.width, this.height, 32, this.height - 60);
      this.setButtonText();
      this.setButtonEnabledStatus();
   }

   private void setButtonText() {
      this.createRepositoryButton.displayString = (this.storageManager.getRepositoryCount() == 0 ? EnumChatFormatting.UNDERLINE : "") + "Create repository";
      this.switchRepositoryButton.displayString = String.format("Switch %s(%d)", EnumChatFormatting.GRAY, this.storageManager.getRepositoryCount());
      AccountRepository AccountRepository = this.getSelectedRepository();
      boolean flag = AccountRepository != null && !AccountRepository.getEncryption().isDecrypted();
      this.passwordField.setVisible(flag);
      this.unlockButton.visible = flag;
      if (flag) {
         this.unlockButton.enabled = !this.passwordField.getText().isEmpty();
      }

      this.searchField.setVisible(!flag && AccountRepository != null);
      this.sortButton.displayString = "Sort by: " + EnumChatFormatting.GRAY + this.altManager.getStorageManager().getAccountSortMode().getName();
      boolean flag1 = this.hasMultiSelection();
      this.preferencesButton.visible = !flag1;
      this.moveSelectedButton.visible = flag1;
      this.deleteSelectedButton.visible = flag1;
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton == this.createRepositoryButton) {
         this.mc.displayGuiScreen(new CreateRepositoryScreen(this, this.altManager));
      } else if (guibutton == this.switchRepositoryButton) {
         this.mc.displayGuiScreen(new SelectRepositoryScreen(this, this.altManager, null));
      } else if (guibutton == this.manageRepositoryButton) {
         this.mc.displayGuiScreen(new ManageRepositoryScreen(this, this.altManager, this.getSelectedRepository()));
      } else if (guibutton.id == 0) {
         this.back();
      } else if (guibutton == this.deleteSelectedButton) {
         List<AbstractAccount> list = this.getMultiSelectionAccounts();
         AbstractAccount[] selectedAccounts = list.toArray(new AbstractAccount[0]);

         for (AbstractAccount account : selectedAccounts) {
            account.getRepository().deleteAccount(account);
         }

         this.getMultiSelectionAccounts().clear();
      } else if (guibutton == this.moveSelectedButton) {
         AbstractAccount[] accountsToMove = this.getMultiSelectionAccounts().toArray(new AbstractAccount[0]);
         this.mc.displayGuiScreen(new SelectRepositoryScreen(this, this.altManager, targetRepository -> {
            if (targetRepository != null) {
               for (AbstractAccount account : accountsToMove) {
                  account.getRepository().deleteAccount(account);
                  targetRepository.addAccount(account);
               }
            }
         }));
         this.getMultiSelectionAccounts().clear();
      } else if (guibutton.id == 2) {
         this.mc.displayGuiScreen(new AddAccountScreen(this.altManager, this, this.getSelectedRepository()));
      } else if (guibutton.id == 7) {
         this.getSelectedRepository().getSelectedAccount().setMinecraftSessionAndRefreshIfNeeded();
      } else if (guibutton.id == 8) {
         this.mc.displayGuiScreen(new ViewAccountScreen(this.altManager, this, this.storageManager.getSelectedAccount()));
      } else if (guibutton.id == 9) {
         this.mc.displayGuiScreen(new PreferencesScreen(this.altManager, this));
      } else if (guibutton.id == 10) {
         this.mc.displayGuiScreen(new GuiMultiplayer(this));
      } else if (guibutton.id == 11) {
         AccountRepository AccountRepository = this.getSelectedRepository();
         if (AccountRepository != null && !AccountRepository.getEncryption().isDecrypted()) {
            String s = this.passwordField.getText();
            if (!AccountRepository.getEncryption().tryDecryptWithPassword(s)) {
               this.statusRenderer.setStatus("Repository password is incorrect", 2000);
            } else {
               try {
                  for (AbstractAccount AbstractAccount : AccountRepository.getAccountList()) {
                     AbstractAccount.decryptIfWaitingPassword();
                  }
               } catch (Throwable throwable) {
                  AccountRepository.getEncryption().lock();
                  throwable.printStackTrace();
                  this.statusRenderer.setStatus("Decrypting repository failed. Check console!", 10000);
               }
            }

            this.passwordField.setText("");
         }
      } else if (guibutton == this.sortButton) {
         this.altManager
            .getStorageManager()
            .setAccountSortMode(AccountSortMode.values()[(this.altManager.getStorageManager().getAccountSortMode().ordinal() + 1) % AccountSortMode.values().length]);
         this.invalidateAllViewingLists();
         this.multiSelectionAccounts.clear();
      }
   }

   public void invalidateAllViewingLists() {
      for (AccountRepository AccountRepository : this.altManager.getStorageManager().getRepositories()) {
         AccountRepository.invalidateViewingAccountList();
      }
   }

   public void scrollToAccount(AbstractAccount AbstractAccount) {
      AccountRepository AccountRepository = this.getSelectedRepository();
      if (AccountRepository != null) {
         int i = AccountRepository.getAccountList().indexOf(AbstractAccount);
         if (i != -1) {
            int j = Math.max(i - 5, 3);
            int k = j * this.accountList.getSlotHeight() - this.accountList.getAmountScrolled();
            this.accountList.scrollBy(k);
         }
      }
   }

   private void back() {
      this.mc.displayGuiScreen(new GuiMainMenu());
      this.altManager.getStorageManager().setAutoSaveNowIfRequired();
   }

   private void setButtonEnabledStatus() {
      this.loginButton.enabled = this.storageManager.getSelectedAccount() != null;
      this.viewButton.enabled = this.storageManager.getSelectedAccount() != null;
      AccountRepository AccountRepository = this.getSelectedRepository();
      this.addButton.enabled = AccountRepository != null;
      this.manageRepositoryButton.enabled = AccountRepository != null;
      this.multiplayerButton.visible = this.altManager.getStorageManager().isMultiplayerButtonVisible()
         && this.multiplayerButton.xPosition > this.cancelButton.xPosition + this.cancelButton.width + 4;
      this.sortButton.visible = this.sortButton.xPosition + this.sortButton.width < this.loginButton.xPosition - 4
         && AccountRepository != null
         && AccountRepository.getEncryption().isDecrypted();
   }

   public void drawAccountManagerHeader(String s, int i) {
      String s1;
      if (this.getSelectedRepository() == null) {
         s1 = EnumChatFormatting.RED + "No repository selected";
      } else {
         s1 = this.getSelectedRepository().getName() + EnumChatFormatting.GRAY + " (" + this.getSelectedRepository().getAccountCount() + ")";
      }

      this.drawCenteredString(this.fontRendererObj, s, i / 2, 10, 16777215);
      this.drawSmallStringWithShadow(s1, i * 0.5F - this.fontRendererObj.getStringWidth(s1) * 0.5F * 0.5F, 21.5F, 16777215);
   }

   public int drawSmallStringWithShadow(String s, float f, float f1, int i) {
      GlStateManager.scale(0.5, 0.5, 0.5);
      int j = this.mc.fontRendererObj.drawStringWithShadow(s, f * 2.0F, f1 * 2.0F, i);
      GlStateManager.scale(2.0, 2.0, 2.0);
      return j;
   }

   private void drawMultiSelection() {
      List list = this.multiSelectionAccounts;
      GlStateManager.scale(0.5F, 0.5F, 0.5F);
      this.mc
         .fontRendererObj
         .drawStringWithShadow(
            "Selected " + EnumChatFormatting.WHITE + list.size() + EnumChatFormatting.RESET + " account" + (list.size() == 1 ? "" : "s"),
            9.0F,
            52.0F,
            new Color(135, 135, 135).getRGB()
         );
      GlStateManager.scale(2.0F, 2.0F, 2.0F);
   }

   private boolean hasMultiSelection() {
      return !this.multiSelectionAccounts.isEmpty();
   }

   public void addToMultiSelection(AbstractAccount AbstractAccount) {
      if (!this.getMultiSelectionAccounts().contains(AbstractAccount)) {
         this.getMultiSelectionAccounts().add(AbstractAccount);
      }
   }

   public void drawScreen(int i, int j, float f) {
      super.drawDefaultBackground();
      this.accountList.drawScreen(i, j, f);
      this.drawAccountManagerHeader("Pug Alt Manager", this.width);
      this.statusRenderer.draw(this.width / 2, 108);
      this.searchField.drawTextBox();
      this.passwordField.drawTextBox();
      if (this.hasMultiSelection()) {
         this.drawMultiSelection();
      }

      if (this.passwordField.getVisible()) {
         AccountRepository AccountRepository = this.getSelectedRepository();
         if (AccountRepository != null) {
            this.drawCenteredString(
               this.mc.fontRendererObj,
               "Repository " + EnumChatFormatting.GRAY + AccountRepository.getName() + EnumChatFormatting.RESET + " is locked with a password",
               this.width / 2,
               this.passwordField.yPosition - 14,
               ColorUtils.grayscale(230)
            );
         }
      }

      super.drawScreen(i, j, f);
   }

   public void updateScreen() {
      this.setButtonText();
      this.passwordField.updateCursorCounter();
      this.searchField.updateCursorCounter();
      AccountRepository AccountRepository = this.getSelectedRepository();
      if (AccountRepository != null) {
         AccountRepository.invalidateViewingAccountList();
         AccountRepository.setLastAccessed(System.currentTimeMillis());
         if (this.previousRepository != AccountRepository) {
            AbstractAccount AbstractAccount = AccountRepository.getSelectedAccount();
            if (AbstractAccount != null) {
               this.scrollToAccount(AbstractAccount);
            }

            this.previousRepository = AccountRepository;
         }
      }
   }

   public void handleMouseInput() throws IOException {
      super.handleMouseInput();
      this.accountList.handleMouseInput();
   }

   protected void mouseClicked(int i, int j, int k) throws IOException {
      this.accountList.mouseClicked(i, j, k);
      this.passwordField.mouseClicked(i, j, k);
      this.searchField.mouseClicked(i, j, k);
      super.mouseClicked(i, j, k);
   }

   protected void keyTyped(char c0, int i) throws IOException {
      if (!this.passwordField.textboxKeyTyped(c0, i)) {
         if (this.searchField.getVisible()) {
            if (i == 1) {
               if (!this.searchField.getText().isEmpty()) {
                  this.searchField.setText("");
                  return;
               }

               if (this.searchField.isFocused()) {
                  this.searchField.setFocused(false);
                  return;
               }
            } else if (c0 >= '!' && c0 <= '~') {
               this.searchField.setFocused(true);
            }
         }

         if (this.getSelectedRepository() != null && this.searchField.textboxKeyTyped(c0, i)) {
            this.getSelectedRepository().invalidateViewingAccountList();
            this.multiSelectionAccounts.clear();
            this.altManager.getStorageManager().setSearchTerm(this.searchField.getText());
         } else if (i == 1) {
            if (!this.multiSelectionAccounts.isEmpty()) {
               this.multiSelectionAccounts.clear();
            } else {
               this.back();
            }
         } else {
            super.keyTyped(c0, i);
         }
      }
   }

   public AccountRepository getSelectedRepository() {
      return this.storageManager.getSelectedRepository();
   }
}
