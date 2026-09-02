package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.AltManager;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
public final class RepositoryListWidget extends GuiListExtended {
   private final List<AccountRepository> repositories;
   private final AltManager altManager;

   public RepositoryListWidget(Minecraft minecraft, AltManager altmanager) {
      super(minecraft, 0, 0, 32, 0, 28);
      this.repositories = altmanager.getStorageManager().getRepositories();
      this.altManager = altmanager;
   }

   protected int getScrollBarX() {
      return super.getScrollBarX() + 30;
   }

   protected boolean isSelected(int i) {
      return this.repositories.indexOf(this.altManager.getMainScreen().getSelectedRepository()) == i;
   }

   public IGuiListEntry getListEntry(int i) {
      return this.repositories.get(i);
   }

   protected int getSize() {
      return this.repositories.size();
   }
}
