package dev.pugrilla.altmanager.account;


import java.util.Comparator;
public enum AccountSortMode {
   CREATION_TIME_ASCENDING("Created >", Comparator.comparingLong(AbstractAccount::getCreationTime)),
   CREATION_TIME_DESCENDING("Created <", CREATION_TIME_ASCENDING.getComparator().reversed()),
   LAST_USED_ASCENDING("Used >", Comparator.comparingLong(AbstractAccount::getLastUsed)),
   LAST_USED_DESCENDING("Used <", LAST_USED_ASCENDING.getComparator().reversed()),
   USERNAME_ASCENDING("Name (A-z)", (first, second) -> -second.getUsername().compareTo(first.getUsername())),
   USERNAME_DESCENDING("Name (z-A)", USERNAME_ASCENDING.getComparator().reversed()),
   BAN_EXPIRY_ASCENDING("Ban >", Comparator.comparingLong(AbstractAccount::getBanExpiry)),
   BAN_EXPIRY_DESCENDING("Ban <", BAN_EXPIRY_ASCENDING.getComparator().reversed());
   private final String displayName;
   private final Comparator<AbstractAccount> comparator;

   AccountSortMode(String s1, Comparator<AbstractAccount> comparator) {
      this.displayName = s1;
      this.comparator = comparator;
   }

   public String getName() {
      return this.displayName;
   }

   public Comparator<AbstractAccount> getComparator() {
      return this.comparator;
   }
}
