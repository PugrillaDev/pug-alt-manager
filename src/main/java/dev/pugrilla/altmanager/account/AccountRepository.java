package dev.pugrilla.altmanager.account;

import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.storage.RepositoryEncryption;
import dev.pugrilla.altmanager.util.AltManagerUtils;
import dev.pugrilla.altmanager.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
public final class AccountRepository implements IGuiListEntry {
   private final AltManager altManager;
   private String name;
   private final long creationTime;
   private long lastAccessed;
   private final List<AbstractAccount> accounts = new ArrayList<>();
   private AbstractAccount selectedAccount;
   private final RepositoryEncryption encryption;
   private List<AbstractAccount> viewingAccounts;

   public AccountRepository(String s, AltManager altmanager, long i, RepositoryEncryption RepositoryEncryption) {
      this.altManager = altmanager;
      this.creationTime = i;
      this.encryption = RepositoryEncryption;
      this.setName(s);
   }

   public void setLastAccessed(long i) {
      this.lastAccessed = i;
   }

   public long getLastAccessed() {
      return this.lastAccessed;
   }

   public RepositoryEncryption getEncryption() {
      return this.encryption;
   }

   public long getCreationTime() {
      return this.creationTime;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String s) {
      if (!AltManagerUtils.isValidRepositoryName(s)) {
         throw new IllegalArgumentException("Invalid repository name: " + s);
      }

      this.name = s;
   }

   public List<AbstractAccount> getViewingAccountList() {
      List<AbstractAccount> list = this.viewingAccounts;
      if (list == null) {
         this.createViewingAccountList();
         list = this.viewingAccounts;
      }

      return list;
   }

   private void createViewingAccountList() {
      AccountSortMode AccountSortMode = this.altManager.getStorageManager().getAccountSortMode();
      ArrayList<AbstractAccount> arraylist = new ArrayList<>(this.accounts);
      arraylist.sort(AccountSortMode.getComparator());
      String s = this.altManager.getStorageManager().getSearchTerm().toLowerCase();
      if (!s.isEmpty()) {
         arraylist.removeIf(account -> !account.getUsername().toLowerCase().contains(s));
      }

      this.viewingAccounts = arraylist;
   }

   public List<AbstractAccount> getAccountList() {
      return this.accounts;
   }

   public void addAccount(AbstractAccount AbstractAccount) {
      if (this.accounts.contains(AbstractAccount)) {
         throw new IllegalStateException("account already exists");
      }

      if (AbstractAccount.getRepository() != null) {
         throw new IllegalStateException(String.format("cannot add account %s to %s, already owned by %s", AbstractAccount, this, AbstractAccount.getRepository()));
      }

      if (AbstractAccount.getCreationTime() == 0L) {
         AbstractAccount.setCreationTime(System.currentTimeMillis());
      }

      AbstractAccount.setRepository(this);
      this.accounts.add(AbstractAccount);
      if (this.accounts.size() == 1) {
         this.setSelectedAccount(AbstractAccount);
      }

      this.invalidateViewingAccountList();
   }

   public void invalidateViewingAccountList() {
      this.viewingAccounts = null;
   }

   public void deleteAccount(AbstractAccount AbstractAccount) {
      if (this.accounts.remove(AbstractAccount)) {
         AbstractAccount.setRepository(null);
         this.altManager.getStorageManager().setAutoSaveRequired();
         this.invalidateViewingAccountList();
      }
   }

   public void deleteAllAccounts() {
      while (!this.accounts.isEmpty()) {
         this.deleteAccount((AbstractAccount)this.accounts.get(0));
      }
   }

   public AltManager getAltManager() {
      return this.altManager;
   }

   public int getAccountCount() {
      return this.accounts.size();
   }

   public void setSelected(int i, int j, int k) {
   }

   public void drawEntry(int i1, int i, int j, int k, int j1, int k1, int l1, boolean flag) {
      Minecraft minecraft = Minecraft.getMinecraft();
      boolean flag1 = this.altManager.getMainScreen().getSelectedRepository() == this;
      int l = 140;
      if (flag1) {
         l += 90;
      }

      if (flag) {
         l += 40;
      }

      l = Math.min(l, 255);
      GlStateManager.enableBlend();
      String s = (flag1 ? EnumChatFormatting.BOLD.toString() : "") + this.name;
      minecraft.fontRendererObj.drawStringWithShadow(s, i + k / 2.0F - minecraft.fontRendererObj.getStringWidth(s) / 2.0F, j + 2, ColorUtils.grayscaleWithAlpha(190, l));
      ArrayList arraylist = new ArrayList(3);
      arraylist.add(
         EnumChatFormatting.AQUA.toString() + this.getAccountCount() + EnumChatFormatting.RESET + " account" + (this.getAccountCount() == 1 ? "" : "s")
      );
      if (this.lastAccessed == 0L) {
         arraylist.add("never used");
      } else {
         arraylist.add(AltManagerUtils.formatRelativeTime(this.lastAccessed, " ago"));
      }

      if (this.encryption.isEnabled()) {
         arraylist.add("encrypted");
      }

      String s1 = String.join(EnumChatFormatting.GRAY + ", " + EnumChatFormatting.RESET, arraylist);
      minecraft.fontRendererObj.drawStringWithShadow(s1, i + k / 2.0F - minecraft.fontRendererObj.getStringWidth(s1) / 2.0F, j + 13, ColorUtils.grayscaleWithAlpha(210, l - 10));
   }

   public boolean mousePressed(int i, int j, int k, int l, int i1, int j1) {
      this.altManager.getStorageManager().setSelectedRepository(this);
      this.altManager.getStorageManager().setAutoSaveRequired();
      return true;
   }

   public void mouseReleased(int i, int j, int k, int l, int i1, int j1) {
   }

   public void setSelectedAccount(AbstractAccount AbstractAccount) {
      if (this.selectedAccount != AbstractAccount) {
         if (!this.accounts.contains(AbstractAccount)) {
            throw new IllegalArgumentException("tried to add account we dont own: " + AbstractAccount + " -> " + this);
         }

         this.selectedAccount = AbstractAccount;
      }
   }

   public AbstractAccount getSelectedAccount() {
      return this.selectedAccount;
   }

   @Override
   public String toString() {
      return "AccountRepository{altManager="
         + this.altManager
         + ", name='"
         + this.name
         + '\''
         + ", creationTime="
         + this.creationTime
         + ", accountList="
         + this.accounts
         + ", selectedAccount="
         + this.selectedAccount
         + ", encryption="
         + this.encryption
         + '}';
   }
}
