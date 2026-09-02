package dev.pugrilla.altmanager.util;


import com.mojang.util.UUIDTypeAdapter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
public final class AltManagerUtils {
   public static final Pattern MINECRAFT_USERNAME_PATTERN = Pattern.compile("^(?!_)(?!.*__)(?!.*_$)[a-zA-Z0-9_]{3,16}(?<!_)$");
   public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
   public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   public static boolean isValidRepositoryName(String s) {
      return !s.isEmpty() && s.length() <= 32;
   }
   public static String truncate(String s, int i, boolean flag) {
      if (s.length() <= i) {
         return s;
      }

      String s1 = s.substring(0, i);
      if (flag) {
         while (s1.length() > 1 && s1.endsWith(" ")) {
            s1 = s1.substring(0, s1.length() - 1);
         }
      }

      return s1 + (flag ? "..." : "");
   }
   public static String formatDuration(long i, boolean flag) {
      if (i <= 0L) {
         return flag ? "0.0s" : "0s";
      }

      String s;
      if (i < 60000L) {
         s = flag ? String.format("%.1fs", i / 1000.0) : i / 1000L + "s";
      } else if (i < 3600000L) {
         s = i / 60000L + "m " + i % 60000L / 1000L + "s";
      } else if (i < 86400000L) {
         s = i / 3600000L + "h " + i % 3600000L / 60000L + "m " + i % 60000L / 1000L + "s";
      } else {
         s = i / 86400000L + "d " + i % 86400000L / 3600000L + "h " + i % 3600000L / 60000L + "m " + i % 60000L / 1000L + "s";
      }

      return s;
   }
   public static String formatRelativeTime(long i, String s) {
      long j = System.currentTimeMillis();
      long k = j - i;
      if (k <= 1000L) {
         return "just now";
      }

      long l = k / 1000L;
      if (l > 60L) {
         long i1 = l / 60L;
         if (i1 > 60L) {
            long j1 = i1 / 60L;
            if (j1 > 24L) {
               long k1 = j1 / 24L;
               if (k1 > 30L) {
                  long j2 = k1 / 30L;
                  if (j2 > 12L) {
                     long i2 = j2 / 12L;
                     return i2 + " year" + (i2 == 1L ? "" : "s") + s;
                  } else {
                     return j2 + " month" + (j2 == 1L ? "" : "s") + s;
                  }
               } else if (k1 > 14L) {
                  long l1 = k1 / 7L;
                  return l1 + " weeks" + s;
               } else {
                  return k1 + " day" + (k1 == 1L ? "" : "s") + s;
               }
            } else {
               return j1 + " hour" + (j1 == 1L ? "" : "s") + s;
            }
         } else {
            return i1 + " minute" + (i1 == 1L ? "" : "s") + s;
         }
      } else {
         return l + " second" + (l == 1L ? "" : "s") + s;
      }
   }
   public static String formatTimestamp(long i) {
      Date date = new Date(i);
      SimpleDateFormat simpledateformat = new SimpleDateFormat("EEE", Locale.ENGLISH);
      SimpleDateFormat simpledateformat1 = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
      String s = simpledateformat.format(date);
      String s1 = simpledateformat1.format(date);
      Date date1 = new Date();
      SimpleDateFormat simpledateformat2 = new SimpleDateFormat("EEE", Locale.ENGLISH);
      if (System.currentTimeMillis() - i >= 604800000L) {
         return DATE_FORMAT.format(date);
      } else {
         return s.equals(simpledateformat2.format(date1)) ? s1 : s + ", " + s1;
      }
   }
   public static boolean isUuid(String s) {
      return parseUuid(s) != null;
   }
   public static String extractFirstRegexGroup(String s, String s1) {
      Pattern pattern = Pattern.compile(s);
      Matcher matcher = pattern.matcher(s1);
      return matcher.find() ? matcher.group(1) : null;
   }
   public static String getClipboardText() {
      return GuiScreen.getClipboardString();
   }
   public static void setClipboardText(String s) {
      GuiScreen.setClipboardString(s);
   }
   public static String encryptString(String s) {
      try {
         byte[] abyte = s.getBytes();

         for (int i = 0; i < abyte.length; i++) {
            abyte[i] = (byte)(abyte[i] ^ 15);
         }

         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(1, getStringEncryptionKey());
         return Base64.getEncoder().encodeToString(cipher.doFinal(abyte));
      } catch (Throwable throwable) {
         throw new RuntimeException("Encrypting string", throwable);
      }
   }
   private static Key getStringEncryptionKey() {
      return new SecretKeySpec("1pSbe7paz8vsUGkZ".getBytes(StandardCharsets.UTF_8), "AES");
   }
   public static String decryptString(String s) {
      try {
         Cipher cipher = Cipher.getInstance("AES");
         cipher.init(2, getStringEncryptionKey());
         byte[] abyte = cipher.doFinal(Base64.getDecoder().decode(s.getBytes()));

         for (int i = 0; i < abyte.length; i++) {
            abyte[i] = (byte)(abyte[i] ^ 15);
         }

         return new String(abyte);
      } catch (Throwable throwable) {
         throw new RuntimeException("Decrypting string", throwable);
      }
   }
   public static UUID parseUuid(String s) {
      try {
         return UUID.fromString(s);
      } catch (Throwable throwable1) {
         try {
            return UUIDTypeAdapter.fromString(s);
         } catch (Throwable throwable) {
            return null;
         }
      }
   }
   public static String[] parseCredentialsLine(String s) {
      String s1 = sanitizeCredentialsLine(s);
      String[] astring = new String[]{":", "\\|"};

      for (String s2 : astring) {
         if (s1.contains(String.valueOf(s2.charAt(s2.length() - 1)))) {
            String[] astring1 = s1.split(s2);
            if (astring1.length >= 2) {
               return new String[]{astring1[0], astring1[1]};
            }
         }
      }

      return null;
   }
   private static String sanitizeCredentialsLine(String s) {
      String s1 = s.trim();

      int i;
      while (s1.startsWith("[") && (i = s1.indexOf(93)) != -1) {
         s1 = s1.substring(i + 1);
      }

      i = s1.lastIndexOf(" | ");
      if (i != -1) {
         s1 = s1.substring(0, i);
      }

      return s1;
   }
   public static String dumpGuiButtons(List<GuiButton> list) {
      StringBuilder stringbuilder = new StringBuilder();

      for (GuiButton guibutton : list) {
         stringbuilder.append(guibutton.displayString).append(" [").append(guibutton.id).append("]").append('\n');
         stringbuilder.append("visible=").append(guibutton.visible).append(',');
         stringbuilder.append("enabled=").append(guibutton.enabled).append('\n');
         stringbuilder.append("x=").append(guibutton.xPosition).append(',');
         stringbuilder.append("y=").append(guibutton.yPosition).append(',');
         stringbuilder.append("w=").append(guibutton.width).append(',');
         stringbuilder.append("h=").append(guibutton.height).append('\n');
      }

      return stringbuilder.toString();
   }
}
