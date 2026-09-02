package dev.pugrilla.altmanager.util;


import java.awt.Color;
public final class ColorUtils {
   public static int grayscale(int i) {
      return grayscaleWithAlpha(i, 255);
   }
   public static int grayscaleWithAlpha(int i, int j) {
      return new Color(i, i, i, j).getRGB();
   }
   public static int darker(int i) {
      return new Color(i).darker().getRGB();
   }
   public static int withAlpha(int i, int j) {
      Color color = new Color(i, false);
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), j).getRGB();
   }
}
