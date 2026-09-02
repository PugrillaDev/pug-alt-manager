package dev.pugrilla.altmanager.skin;

public enum SkinVariant {
   CLASSIC("Classic"),
   SLIM("Slim");
   private final String displayName;
   private final String internalName;

   SkinVariant(String s1) {
      this.displayName = s1;
      this.internalName = s1.toLowerCase();
   }
   public static SkinVariant fromApiName(String s) {
      return s.toLowerCase().contains("slim") ? SLIM : CLASSIC;
   }

   public String getInternalName() {
      return this.internalName;
   }

   public String getName() {
      return this.displayName;
   }
}
