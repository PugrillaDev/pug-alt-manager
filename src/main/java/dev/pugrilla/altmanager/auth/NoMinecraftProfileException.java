package dev.pugrilla.altmanager.auth;

import dev.pugrilla.altmanager.network.HttpRequestException;
public final class NoMinecraftProfileException extends HttpRequestException {
   public NoMinecraftProfileException(String s) {
      super(s);
   }
}
