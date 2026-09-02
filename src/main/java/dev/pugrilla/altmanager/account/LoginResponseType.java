package dev.pugrilla.altmanager.account;

public enum LoginResponseType {
   SUCCESS("Successful login.", "Successful"),
   NO_MINECRAFT_PROFILE("This account doesn't have a Minecraft profile.", "Valid, No Minecraft Profile"),
   DOES_NOT_OWN_MINECRAFT("This account doesn't own Minecraft.", "No Minecraft"),
   ABUSE_LOCKED("This account is permanently locked.", "Abuse Locked"),
   RECOVERY_LOCKED("This account is locked for recovery.", "Recovery Locked"),
   INVALID_ACCOUNT("Failed to login.", "Invalid Account"),
   RATE_LIMITED("You are sending too many requests, try again later.", "Rate Limited"),
   VALID_RATE_LIMITED("This account is valid but you are sending too many requests, try again later.", "Valid, Rate Limited"),
   CORRUPT_DATA("This account data is corrupt or empty.", "Internal Data Corruption");
   private final String description;
   private final String shortName;

   LoginResponseType(String s1, String s2) {
      this.description = s1;
      this.shortName = s2;
   }

   public String getDescription() {
      return this.description;
   }

   public String getShortName() {
      return this.shortName;
   }
}
