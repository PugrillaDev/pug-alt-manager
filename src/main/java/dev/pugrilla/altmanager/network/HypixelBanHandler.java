package dev.pugrilla.altmanager.network;

import dev.pugrilla.altmanager.AltManager;

import java.util.List;
import java.util.UUID;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.util.IChatComponent;
public final class HypixelBanHandler {
   private final AltManager altManager;

   public HypixelBanHandler(AltManager altmanager) {
      this.altManager = altmanager;
   }

   public void handleDisconnectMessage(UUID uuid, IChatComponent ichatcomponent) {
      if (ichatcomponent != null) {
         List list = ichatcomponent.getSiblings();
         if (!list.isEmpty()) {
            IChatComponent ichatcomponent1 = (IChatComponent)list.get(0);
            boolean flag = false;
            boolean flag1 = false;
            String s = ichatcomponent1.getUnformattedText();
            long i;
            if (!s.contains("blocked") && !s.contains("banned")) {
               if (s.equals("Failed to authenticate your connection!")) {
                  return;
               }

               i = -1L;
            } else if (s.startsWith("You are permanently banned from this server!")) {
               i = -2L;
               flag = true;
            } else {
               if (!s.equals("You are temporarily banned for ")) {
                  return;
               }

               try {
                  i = System.currentTimeMillis() + parseBanDuration(list);
                  String s1 = ((IChatComponent)list.get(1)).getUnformattedText();
                  if (!s1.contains("0h 0m 0s") && !this.isRecentLengthText(s1)) {
                     boolean flag2 = false;
                  } else {
                     boolean flag3 = true;
                  }
               } catch (Throwable throwable) {
                  this.altManager.getLogger().warn("Unable to parse ban expiry length", throwable);
                  return;
               }

               flag = true;
            }

            this.altManager.getStorageManager().setBanExpiry(uuid, i);
         }
      }
   }

   private boolean isRecentLengthText(String s) {
      for (int i = 60; i >= 0; i--) {
         if (s.contains(String.format("23h 59m %ss", i))) {
            return true;
         }
      }

      return false;
   }
   private static long parseBanDuration(List<IChatComponent> list) throws NumberInvalidException {
      String s = ((IChatComponent)list.get(1)).getUnformattedText();
      long i = 0L;

      for (String s1 : s.split(" ")) {
         char c0 = s1.charAt(s1.length() - 1);
         int j = Integer.parseInt(s1.substring(0, s1.length() - 1));
         if (j < 0) {
            throw new NumberInvalidException();
         }

         if (c0 == 's') {
            i += j * 1000L;
         } else if (c0 == 'm') {
            i += j * 1000L * 60L;
         } else if (c0 == 'h') {
            i += j * 1000L * 60L * 60L;
         } else if (c0 == 'd') {
            i += j * 1000L * 60L * 60L * 24L;
         }
      }

      if (i <= 0L) {
         throw new NumberInvalidException();
      } else {
         return i;
      }
   }
}
