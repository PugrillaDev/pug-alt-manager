package dev.pugrilla.altmanager.gui;

public final class IconAction {
   private int x;
   private int y;
   private final Runnable action;
   private final String icon;

   public IconAction(Runnable runnable, String s) {
      this.action = runnable;
      this.icon = s;
   }

   public Runnable getRunnable() {
      return this.action;
   }

   public String getIcon() {
      return this.icon;
   }
}
