package dev.pugrilla.jnafilechooser.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import java.util.Arrays;
import java.util.List;

public class Comdlg32 {
   public static class OpenFileName extends Structure {
      public int lStructSize = this.size();
      public Pointer hwndOwner;
      public Pointer hInstance;
      public WString lpstrFilter;
      public WString lpstrCustomFilter;
      public int nMaxCustFilter;
      public int nFilterIndex;
      public Pointer lpstrFile;
      public int nMaxFile;
      public String lpstrDialogTitle;
      public int nMaxDialogTitle;
      public WString lpstrInitialDir;
      public WString lpstrTitle;
      public int Flags;
      public short nFileOffset;
      public short nFileExtension;
      public String lpstrDefExt;
      public Pointer lCustData;
      public Pointer lpfnHook;
      public Pointer lpTemplateName;

      @Override
      protected List<String> getFieldOrder() {
         return Arrays.asList(
            "lStructSize",
            "hwndOwner",
            "hInstance",
            "lpstrFilter",
            "lpstrCustomFilter",
            "nMaxCustFilter",
            "nFilterIndex",
            "lpstrFile",
            "nMaxFile",
            "lpstrDialogTitle",
            "nMaxDialogTitle",
            "lpstrInitialDir",
            "lpstrTitle",
            "Flags",
            "nFileOffset",
            "nFileExtension",
            "lpstrDefExt",
            "lCustData",
            "lpfnHook",
            "lpTemplateName"
         );
      }
   }

   public static final int OFN_READONLY = 1;
   public static final int OFN_OVERWRITEPROMPT = 2;
   public static final int OFN_HIDEREADONLY = 4;
   public static final int OFN_NOCHANGEDIR = 8;
   public static final int OFN_SHOWHELP = 16;
   public static final int OFN_ENABLEHOOK = 32;
   public static final int OFN_ENABLETEMPLATE = 64;
   public static final int OFN_ENABLETEMPLATEHANDLE = 128;
   public static final int OFN_NOVALIDATE = 256;
   public static final int OFN_ALLOWMULTISELECT = 512;
   public static final int OFN_EXTENSIONDIFFERENT = 1024;
   public static final int OFN_PATHMUSTEXIST = 2048;
   public static final int OFN_FILEMUSTEXIST = 4096;
   public static final int OFN_CREATEPROMPT = 8192;
   public static final int OFN_SHAREAWARE = 16384;
   public static final int OFN_NOREADONLYRETURN = 32768;
   public static final int OFN_NOTESTFILECREATE = 65536;
   public static final int OFN_NONETWORKBUTTON = 131072;
   public static final int OFN_NOLONGNAMES = 262144;
   public static final int OFN_EXPLORER = 524288;
   public static final int OFN_NODEREFERENCELINKS = 1048576;
   public static final int OFN_LONGNAMES = 2097152;
   public static final int OFN_ENABLEINCLUDENOTIFY = 4194304;
   public static final int OFN_ENABLESIZING = 8388608;
   public static final int OFN_DONTADDTORECENT = 33554432;
   public static final int OFN_FORCESHOWHIDDEN = 268435456;
   public static final int CDERR_DIALOGFAILURE = 65535;
   public static final int CDERR_FINDRESFAILURE = 6;
   public static final int CDERR_INITIALIZATION = 2;
   public static final int CDERR_LOADRESFAILURE = 7;
   public static final int CDERR_LOADSTRFAILURE = 5;
   public static final int CDERR_LOCKRESFAILURE = 8;
   public static final int CDERR_MEMALLOCFAILURE = 9;
   public static final int CDERR_MEMLOCKFAILURE = 10;
   public static final int CDERR_NOHINSTANCE = 4;
   public static final int CDERR_NOHOOK = 11;
   public static final int CDERR_NOTEMPLATE = 3;
   public static final int CDERR_STRUCTSIZE = 1;
   public static final int FNERR_SUBCLASSFAILURE = 12289;
   public static final int FNERR_INVALIDFILENAME = 12290;
   public static final int FNERR_BUFFERTOOSMALL = 12291;

   public static native boolean GetOpenFileNameW(OpenFileName openFileName);

   public static native boolean GetSaveFileNameW(OpenFileName openFileName);

   public static native int CommDlgExtendedError();

   static {
      Native.register("comdlg32");
   }
}
