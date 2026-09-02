package dev.pugrilla.altmanager.auth;

public class Cookie {
   private final String name;
   private final String value;

   public Cookie(String s, String s1) {
      this.name = s;
      this.value = s1;
   }

   public String getName() {
      return this.name;
   }

   public String getValue() {
      return this.value;
   }

   @Override
   public String toString() {
      return this.name + "=" + this.value;
   }
}
