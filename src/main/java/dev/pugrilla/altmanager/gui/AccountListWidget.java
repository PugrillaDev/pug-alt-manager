package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.storage.StorageManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
public final class AccountListWidget extends GuiListExtended {
   private final StorageManager storageManager;
   private static final EmptyAccountListEntry EMPTY_REPOSITORY = new EmptyAccountListEntry("Repository contains no accounts");
   private static final EmptyAccountListEntry NO_SEARCH_RESULTS = new EmptyAccountListEntry("No accounts match your search");
   private static final EmptyAccountListEntry NO_REPOSITORY_SELECTED = new EmptyAccountListEntry("Create or select an account repository to get started");

   public AccountListWidget(Minecraft minecraft, int i, int j, StorageManager StorageManager) {
      super(minecraft, i, j, 32, j - 64, 36);
      this.storageManager = StorageManager;
      this.field_148163_i = false;
   }

   protected boolean isSelected(int i) {
      return false;
   }

   public IGuiListEntry getListEntry(int i) {
      AccountRepository AccountRepository = this.storageManager.getSelectedRepository();
      return AccountRepository == null
         ? NO_REPOSITORY_SELECTED
         : (AccountRepository.getViewingAccountList().isEmpty() ? (AccountRepository.getAccountCount() == 0 ? EMPTY_REPOSITORY : NO_SEARCH_RESULTS) : AccountRepository.getViewingAccountList().get(i));
   }

   protected int getSize() {
      AccountRepository AccountRepository = this.storageManager.getSelectedRepository();
      if (AccountRepository == null) {
         return 1;
      } else {
         return !AccountRepository.getEncryption().isDecrypted() ? 0 : (AccountRepository.getViewingAccountList().isEmpty() ? 1 : AccountRepository.getViewingAccountList().size());
      }
   }
}
