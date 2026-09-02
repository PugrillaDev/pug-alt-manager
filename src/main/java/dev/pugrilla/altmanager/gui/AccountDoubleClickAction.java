package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AbstractAccount;

import java.util.function.Consumer;
public enum AccountDoubleClickAction {
   LOGIN("Login", "set your Minecraft session to it", false, AbstractAccount::setMinecraftSessionAndRefreshIfNeeded),
   REFRESH_SESSION("Refresh Session", "log into it again", true, AbstractAccount::login),
   CHECK_BAN("Ban Check", "check for a server ban", true, AbstractAccount::startBanCheck),
   DELETE("Delete", "delete the account", false, account -> account.getRepository().deleteAccount(account)),
   VIEW("View", "open the view account menu", false, account -> net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
      new ViewAccountScreen(account.getRepository().getAltManager(), net.minecraft.client.Minecraft.getMinecraft().currentScreen, account))),
   SKINS("Skins", "open the skin customization menu", false, account -> net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
      new SkinCustomizationScreen(net.minecraft.client.Minecraft.getMinecraft().currentScreen, account, account.getRepository().getAltManager())));
   private final String displayName;
   private final String description;
   private final boolean async;
   private final Consumer<AbstractAccount> action;

   AccountDoubleClickAction(String s1, String s2, boolean flag, Consumer<AbstractAccount> consumer) {
      this.displayName = s1;
      this.description = s2;
      this.async = flag;
      this.action = consumer;
   }

   public boolean isAsync() {
      return this.async;
   }

   public String getDescription() {
      return this.description;
   }

   public String getName() {
      return this.displayName;
   }

   public Consumer<AbstractAccount> getAction() {
      return this.action;
   }
}
