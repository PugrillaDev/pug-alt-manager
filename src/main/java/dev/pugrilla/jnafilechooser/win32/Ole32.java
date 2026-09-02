package dev.pugrilla.jnafilechooser.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class Ole32 {
   public static native Pointer OleInitialize(Pointer pointer);

   public static native void CoTaskMemFree(Pointer pointer);

   static {
      Native.register("ole32");
   }
}
