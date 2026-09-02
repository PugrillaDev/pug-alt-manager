package dev.pugrilla.jnafilechooser.api;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.awt.Window;
import java.io.File;
import dev.pugrilla.jnafilechooser.win32.Ole32;
import dev.pugrilla.jnafilechooser.win32.Shell32;
import dev.pugrilla.jnafilechooser.win32.Shell32.BrowseInfo;

public class WindowsFolderBrowser {
   private String title;

   public WindowsFolderBrowser() {
      this.title = null;
   }

   public WindowsFolderBrowser(String s) {
      this.title = s;
   }

   public void setTitle(String s) {
      this.title = s;
   }

   public File showDialog(Window window) {
      Ole32.OleInitialize(null);
      BrowseInfo shell32$browseinfo = new BrowseInfo();
      shell32$browseinfo.hwndOwner = Native.getWindowPointer(window);
      shell32$browseinfo.ulFlags = 81;
      if (this.title != null) {
         shell32$browseinfo.lpszTitle = this.title;
      }

      Pointer pointer = Shell32.SHBrowseForFolder(shell32$browseinfo);
      if (pointer != null) {
         Memory memory = new Memory(4096L);
         Shell32.SHGetPathFromIDListW(pointer, memory);
         String s = memory.getString(0L, true);
         File file1 = new File(s);
         Ole32.CoTaskMemFree(pointer);
         return file1;
      } else {
         return null;
      }
   }
}
