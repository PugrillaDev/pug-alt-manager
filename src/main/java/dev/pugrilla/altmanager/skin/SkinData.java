package dev.pugrilla.altmanager.skin;

public final class SkinData {
   private final String skinUrl;
   private final String variant;

   public SkinData(String s, String s1) {
      this.skinUrl = s;
      this.variant = s1;
   }

   public String getSkinUrl() {
      return this.skinUrl;
   }

   public String getVariant() {
      return this.variant;
   }
}
