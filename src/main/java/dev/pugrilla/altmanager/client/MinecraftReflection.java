package dev.pugrilla.altmanager.client;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Session;
public final class MinecraftReflection {
   private static Field minecraftSessionField = findFieldByType(Minecraft.class, Session.class);
   private static Field connectingNetworkManagerField;
   private static Field disconnectReasonField = findFieldByType(GuiDisconnected.class, IChatComponent.class);
   private static Field findFieldByType(Class<?> oclass, Class<?> oclass1) {
      Field[] afield = oclass.getDeclaredFields();

      for (Field field : afield) {
         if (!Modifier.isStatic(field.getModifiers()) && field.getType() == oclass1) {
            try {
               field.setAccessible(true);
               return field;
            } catch (Throwable throwable) {
               throw new IllegalStateException("Cannot access field " + oclass1 + " in " + oclass);
            }
         }
      }

      throw new IllegalStateException("Cannot find field " + oclass1 + " in " + oclass);
   }
   public static IChatComponent getDisconnectReason(GuiDisconnected guidisconnected) {
      try {
         return (IChatComponent)disconnectReasonField.get(guidisconnected);
      } catch (IllegalAccessException illegalaccessexception) {
         throw new RuntimeException("Getting disconnected message", illegalaccessexception);
      }
   }
   public static NetworkManager getConnectingNetworkManager(GuiConnecting guiconnecting) {
      try {
         return (NetworkManager)connectingNetworkManagerField.get(guiconnecting);
      } catch (IllegalAccessException illegalaccessexception) {
         throw new RuntimeException("Getting network manager", illegalaccessexception);
      }
   }
   public static void setMinecraftSession(Minecraft minecraft, Session session) {
      try {
         minecraftSessionField.set(minecraft, session);
      } catch (IllegalAccessException illegalaccessexception) {
         throw new IllegalStateException("Cannot access session field");
      }
   }

   static {
      try {
         Minecraft minecraft = Minecraft.getMinecraft();
         Session session = (Session)minecraftSessionField.get(minecraft);
         setMinecraftSession(minecraft, session);
      } catch (RuntimeException runtimeexception) {
         throw runtimeexception;
      } catch (Throwable throwable) {
         throw new IllegalStateException("Cannot access session field");
      }
   }
}
