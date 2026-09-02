package dev.pugrilla.altmanager.account;


import java.util.UUID;
public interface AccountFactory {
   AbstractAccount create(String s, UUID uuid, String s1);
}
