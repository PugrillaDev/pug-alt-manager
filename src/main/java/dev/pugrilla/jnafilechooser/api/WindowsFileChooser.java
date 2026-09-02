package dev.pugrilla.jnafilechooser.api;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.WString;
import java.awt.Window;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import dev.pugrilla.jnafilechooser.win32.Comdlg32;
import dev.pugrilla.jnafilechooser.win32.Comdlg32.OpenFileName;

public class WindowsFileChooser {
   public static final int MAX_PATH = 260;
   protected File selectedFile;
   protected File currentDirectory;
   protected ArrayList<String[]> filters;
   protected String defaultFilename = "";
   protected String dialogTitle = "";
   private int filterIndex = 1;
   private boolean addToRecent = false;
   private boolean multipleSelection = false;
   private int maxNumberOfFiles = 10000;
   private File[] selectedFiles = null;

   public WindowsFileChooser() {
      this.filters = new ArrayList<>();
   }

   public WindowsFileChooser(File file1) {
      this.filters = new ArrayList<>();
      if (file1 != null) {
         this.currentDirectory = file1.isDirectory() ? file1 : file1.getParentFile();
      }
   }

   public WindowsFileChooser(String s) {
      this(s != null ? new File(s) : null);
   }

   void setFilters(ArrayList<String[]> arraylist) {
      this.filters = arraylist;
   }

   public void addFilter(String s, String... astring) {
      if (astring.length < 1) {
         throw new IllegalArgumentException();
      }

      ArrayList<String> arraylist = new ArrayList<>();
      arraylist.add(s);
      Collections.addAll(arraylist, astring);
      this.filters.add(arraylist.toArray(new String[0]));
   }

   public void setTitle(String s) {
      this.dialogTitle = s;
   }

   public boolean showOpenDialog(Window window) {
      return this.showDialog(window, true);
   }

   public boolean showSaveDialog(Window window) {
      return this.showDialog(window, false);
   }

   boolean showDialog(Window window, boolean flag) {
      OpenFileName comdlg32$openfilename = new OpenFileName();
      comdlg32$openfilename.Flags = 8912908;
      comdlg32$openfilename.hwndOwner = window == null ? null : Native.getWindowPointer(window);
      if (!this.addToRecent) {
         comdlg32$openfilename.Flags |= 33554432;
      }

      if (this.multipleSelection) {
         comdlg32$openfilename.Flags |= 512;
      }

      int i = this.multipleSelection ? this.maxNumberOfFiles * 260 : 260;
      int j = 4 * i + 1;
      comdlg32$openfilename.lpstrFile = new Memory(j);
      if (!this.defaultFilename.isEmpty()) {
         comdlg32$openfilename.lpstrFile.setString(0L, this.defaultFilename, true);
      } else {
         comdlg32$openfilename.lpstrFile.clear(j);
      }

      if (!this.dialogTitle.isEmpty()) {
         comdlg32$openfilename.lpstrTitle = new WString(this.dialogTitle);
      }

      comdlg32$openfilename.nMaxFile = i;
      if (this.currentDirectory != null) {
         comdlg32$openfilename.lpstrInitialDir = new WString(this.currentDirectory.getAbsolutePath());
      }

      if (!this.filters.isEmpty()) {
         comdlg32$openfilename.lpstrFilter = new WString(this.buildFilterString());
         comdlg32$openfilename.nFilterIndex = this.filterIndex;
      }

      boolean flag1 = flag ? Comdlg32.GetOpenFileNameW(comdlg32$openfilename) : Comdlg32.GetSaveFileNameW(comdlg32$openfilename);
      this.selectedFiles = null;
      if (flag1) {
         this.filterIndex = comdlg32$openfilename.nFilterIndex;
         if (this.multipleSelection) {
            byte[] abyte = comdlg32$openfilename.lpstrFile.getByteArray(0L, j);
            List list = bytesToFilePaths(abyte);
            if (list.size() == 1) {
               this.selectedFile = new File((String)list.get(0));
               this.currentDirectory = this.selectedFile.getParentFile();
               (this.selectedFiles = new File[1])[0] = this.selectedFile;
            } else if (list.size() > 1) {
               this.selectedFiles = new File[list.size() - 1];
               this.currentDirectory = new File((String)list.get(0));

               for (int k = 1; k < list.size(); k++) {
                  this.selectedFiles[k - 1] = new File(this.currentDirectory, (String)list.get(k));
               }

               this.selectedFile = this.selectedFiles[0];
            }
         } else {
            String s = comdlg32$openfilename.lpstrFile.getString(0L, true);
            this.selectedFile = new File(s);
            this.currentDirectory = this.selectedFile.getParentFile();
            (this.selectedFiles = new File[1])[0] = this.selectedFile;
         }
      } else {
         int l = Comdlg32.CommDlgExtendedError();
         if (l != 0) {
            throw new RuntimeException("GetOpenFileName failed with error " + l);
         }
      }

      return flag1;
   }

   private String buildFilterString() {
      StringBuilder stringbuilder = new StringBuilder();

      for (String[] astring : this.filters) {
         String s = astring[0];
         stringbuilder.append(s);
         stringbuilder.append('\u0000');

         for (int i = 1; i < astring.length; i++) {
            stringbuilder.append("*.");
            stringbuilder.append(astring[i]);
            stringbuilder.append(';');
         }

         stringbuilder.deleteCharAt(stringbuilder.length() - 1);
         stringbuilder.append('\u0000');
      }

      stringbuilder.append('\u0000');
      return stringbuilder.toString();
   }

   public File getSelectedFile() {
      return this.selectedFile;
   }

   public File getCurrentDirectory() {
      return this.currentDirectory;
   }

   public void setDefaultFilename(String s) {
      this.defaultFilename = s;
   }

   public int getFilterIndex() {
      return this.filterIndex;
   }

   public void setFilterIndex(int i) {
      this.filterIndex = i;
   }

   public boolean isAddToRecent() {
      return this.addToRecent;
   }

   public void setAddToRecent(boolean flag) {
      this.addToRecent = flag;
   }

   public boolean isMultipleSelection() {
      return this.multipleSelection;
   }

   public void setMultiSelectionEnabled(boolean flag) {
      this.multipleSelection = flag;
   }

   public int getMaxNumberOfFiles() {
      return this.maxNumberOfFiles;
   }

   public void setMaxNumberOfFiles(int i) {
      this.maxNumberOfFiles = i;
   }

   public File[] getSelectedFiles() {
      return this.selectedFiles;
   }

   public static List<String> bytesToFilePaths(byte[] abyte) {
      ArrayList arraylist = new ArrayList();
      int i = 0;

      for (byte b0 = 0; b0 < abyte.length - 1; b0 += 2) {
         if (abyte[b0] == 0 && abyte[b0 + 1] == 0) {
            if (b0 <= i) {
               break;
            }

            arraylist.add(new String(Arrays.copyOfRange(abyte, i, b0), StandardCharsets.UTF_16LE));
            i = b0 + 2;
         }
      }

      return arraylist;
   }
}
