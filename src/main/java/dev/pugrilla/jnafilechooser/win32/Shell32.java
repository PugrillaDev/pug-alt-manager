package dev.pugrilla.jnafilechooser.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public class Shell32 {
   public static class BrowseInfo extends Structure {
      public Pointer hwndOwner;
      public Pointer pidlRoot;
      public String pszDisplayName;
      public String lpszTitle;
      public int ulFlags;
      public Pointer lpfn;
      public Pointer lParam;
      public int iImage;

      @Override
      protected List<String> getFieldOrder() {
         return Arrays.asList("hwndOwner", "pidlRoot", "pszDisplayName", "lpszTitle", "ulFlags", "lpfn", "lParam", "iImage");
      }
   }

   public static final int BIF_RETURNONLYFSDIRS = 1;
   public static final int BIF_DONTGOBELOWDOMAIN = 2;
   public static final int BIF_NEWDIALOGSTYLE = 64;
   public static final int BIF_EDITBOX = 16;
   public static final int BIF_USENEWUI = 80;
   public static final int BIF_NONEWFOLDERBUTTON = 512;
   public static final int BIF_BROWSEINCLUDEFILES = 16384;
   public static final int BIF_SHAREABLE = 32768;
   public static final int BIF_BROWSEFILEJUNCTIONS = 65536;

   public static native Pointer SHBrowseForFolder(BrowseInfo browseInfo);

   public static native boolean SHGetPathFromIDListW(Pointer pointer, Pointer pointer1);

   static {
      Native.register("shell32");
   }
}
