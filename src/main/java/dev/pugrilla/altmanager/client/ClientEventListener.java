package dev.pugrilla.altmanager.client;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.network.HypixelBanHandler;
import dev.pugrilla.altmanager.util.AltManagerUtils;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IChatComponent.Serializer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
public final class ClientEventListener {
   private static final int ACCOUNT_MANAGER_BUTTON_ID = -258581536;
   private final AltManager altManager;
   private final HypixelBanHandler hypixelBanHandler;
   private boolean initialAccountApplied;

   public ClientEventListener(AltManager altmanager) {
      this.altManager = altmanager;
      this.hypixelBanHandler = new HypixelBanHandler(altmanager);
   }

   @SubscribeEvent
   public void onGuiScreenInit(Post post) {
      if (post.gui instanceof GuiMainMenu) {
         this.setMainMenuButtons((GuiMainMenu)post.gui, post.buttonList);
         if (!this.initialAccountApplied) {
            this.initialAccountApplied = true;
            AbstractAccount AbstractAccount = this.altManager.getStorageManager().getSelectedAccount();
            if (AbstractAccount != null) {
               AbstractAccount.setMinecraftSession(false);
            }
         }
      } else if (post.gui instanceof GuiDisconnected) {
         IChatComponent ichatcomponent = MinecraftReflection.getDisconnectReason((GuiDisconnected)post.gui);
         this.hypixelBanHandler.handleDisconnectMessage(Minecraft.getMinecraft().getSession().getProfile().getId(), ichatcomponent);
         if (this.altManager.getStorageManager().isMultiplayerButtonVisible() && !post.buttonList.isEmpty()) {
            post.buttonList
               .add(new GuiButton(-258581536, post.gui.width / 2 - 100, ((GuiButton)post.buttonList.get(0)).yPosition + 24, "Alt Manager"));
         }
      } else if (post.gui instanceof GuiMultiplayer && this.altManager.getStorageManager().isMultiplayerButtonVisible()) {
         GuiButton guibutton = new GuiButton(-258581536, post.gui.width - 108, post.gui.height - 28, 100, 20, "Alt Manager");
         if (!this.isConflictingButtons(post.buttonList, guibutton)) {
            post.buttonList.add(guibutton);
         }
      }
   }

   private boolean isConflictingButtons(List<GuiButton> list, GuiButton guibutton) {
      for (GuiButton guibutton1 : list) {
         if (guibutton.xPosition <= guibutton1.xPosition + guibutton1.width && guibutton.yPosition >= guibutton1.yPosition - 10) {
            return true;
         }
      }

      return false;
   }

   private void setMainMenuButtons(GuiMainMenu guimainmenu, List<GuiButton> list) {
      for (GuiButton guibutton : list) {
         if (guibutton.id == 2) {
            list.add(new GuiButtonLanguage(-258581536, guibutton.xPosition + guibutton.width + 4, guibutton.yPosition));
            return;
         }
      }

      throw new RuntimeException("Cannot find realms button. Dump of all buttons: " + AltManagerUtils.dumpGuiButtons(list));
   }

   @SubscribeEvent
   public void onClientChat(ClientChatReceivedEvent clientchatreceivedevent) {
      IChatComponent ichatcomponent = clientchatreceivedevent.message;
      if (ichatcomponent != null) {
         String s = Serializer.componentToJson(ichatcomponent);
         if (s.equals(
            "{\"italic\":false,\"extra\":[\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"extra\":[{\"color\":\"white\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"white\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_blue\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"black\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_green\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"white\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"white\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_green\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"black\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_red\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_aqua\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"blue\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_green\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"black\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"black\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_aqua\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"blue\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_green\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"black\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"black\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_aqua\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"blue\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"dark_green\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"black\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"},{\"color\":\"black\",\"text\":\" \"},\"\",{\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false,\"text\":\"\"}],\"text\":\"\"}],\"text\":\"\"}"
         )) {
            this.altManager.getStorageManager().setBanExpiry(Minecraft.getMinecraft().getSession().getProfile().getId(), -1L);
         }
      }
   }

   @SubscribeEvent
   public void onGuiScreenDraw(DrawScreenEvent drawscreenevent) {
      if ((drawscreenevent.gui instanceof GuiMainMenu || drawscreenevent.gui instanceof GuiMultiplayer || drawscreenevent.gui instanceof GuiSelectWorld)
         && this.altManager.getStorageManager().isShowLoggedInUser()) {
         String s = "logged in as " + EnumChatFormatting.GRAY + drawscreenevent.gui.mc.getSession().getUsername();
         drawscreenevent.gui
            .mc
            .fontRendererObj
            .drawStringWithShadow(s, drawscreenevent.gui.width - drawscreenevent.gui.mc.fontRendererObj.getStringWidth(s) - 4, 4.0F, -1);
      }
   }

   @SubscribeEvent
   public void onGuiScreenActionPerformed(ActionPerformedEvent actionperformedevent) {
      if (actionperformedevent.button.id == -258581536) {
         actionperformedevent.gui.mc.displayGuiScreen(this.altManager.getMainScreen());
      }
   }

   public AltManager getAltManager() {
      return this.altManager;
   }

   public HypixelBanHandler getServerBanHandler() {
      return this.hypixelBanHandler;
   }
}
