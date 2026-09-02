package dev.pugrilla.altmanager.storage;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.AccountSortMode;
import dev.pugrilla.altmanager.account.AccountType;
import dev.pugrilla.altmanager.gui.AccountDoubleClickAction;
import dev.pugrilla.altmanager.skin.SkinVariant;

import java.util.List;
import java.util.UUID;
public interface StorageManager {
   List<AccountRepository> getRepositories();

   void createRepository(AccountRepository AccountRepository);

   int getRepositoryCount();

   void deleteRepository(AccountRepository AccountRepository);

   void deleteAllRepositories();

   AccountRepository getSelectedRepository();

   void setSelectedRepository(AccountRepository AccountRepository);

   default AbstractAccount getSelectedAccount() {
      AccountRepository AccountRepository = this.getSelectedRepository();
      return AccountRepository == null ? null : AccountRepository.getSelectedAccount();
   }

   AccountType getSelectedAddAccountType();

   void setSelectedAddAccountType(AccountType AccountType);

   String getLastCookieFilePath();

   void setLastCookieFilePath(String s);

   long getBanExpiry(UUID uuid);

   void setBanExpiry(UUID uuid, long i);

   AccountDoubleClickAction getAccountDoubleClickAction();

   void setAccountDoubleClickAction(AccountDoubleClickAction AccountDoubleClickAction);

   boolean isShowLoggedInUser();

   void setShowLoggedInUser(boolean flag);

   boolean isMultiplayerButtonVisible();

   void setMultiplayerButtonVisible(boolean flag);

   String getLastSkinFilePath();

   void setLastSkinFilePath(String s);

   SkinVariant getSkinVariant();

   void setSkinVariant(SkinVariant SkinVariant);

   AccountSortMode getAccountSortMode();

   void setAccountSortMode(AccountSortMode AccountSortMode);

   String getSearchTerm();

   void setSearchTerm(String s);

   boolean isAutoRefreshSession();

   void setAutoRefreshSession(boolean flag);
}
