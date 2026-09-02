package dev.pugrilla.jnafilechooser.api;

import com.sun.jna.Platform;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class JnaFileChooser {
   private enum Action {
      Open,
      Save
   }

   public enum Mode {
      Files(0),
      Directories(1),
      FilesAndDirectories(2);

      private final int jFileChooserValue;

      Mode(int jFileChooserValue) {
         this.jFileChooserValue = jFileChooserValue;
      }

      public int getJFileChooserValue() {
         return this.jFileChooserValue;
      }
   }

   protected File[] selectedFiles;
   protected File currentDirectory;
   protected ArrayList<String[]> filters = new ArrayList<>();
   protected boolean multiSelectionEnabled = false;
   protected Mode mode = Mode.Files;
   protected String defaultFile;
   protected String dialogTitle;
   protected String openButtonText;
   protected String saveButtonText;

   public JnaFileChooser() {
      this.selectedFiles = new File[]{null};
      this.defaultFile = "";
      this.dialogTitle = "";
      this.openButtonText = "";
      this.saveButtonText = "";
   }

   public JnaFileChooser(File file1) {
      this();
      if (file1 != null) {
         this.currentDirectory = file1.isDirectory() ? file1 : file1.getParentFile();
      }
   }

   public JnaFileChooser(String s) {
      this(s != null ? new File(s) : null);
   }

   public boolean showOpenDialog(Window window) {
      return this.showDialog(window, Action.Open);
   }

   public boolean showSaveDialog(Window window) {
      return this.showDialog(window, Action.Save);
   }

   private boolean showDialog(Window window, Action action) {
      if (Platform.isWindows()) {
         if (this.mode == Mode.Files) {
            return this.showWindowsFileChooser(window, action);
         }

         if (this.mode == Mode.Directories) {
            return this.showWindowsFolderBrowser(window);
         }
      }

      return this.showSwingFileChooser(window, action);
   }

   private boolean showSwingFileChooser(Window window, Action action) {
      JFileChooser jfilechooser = new JFileChooser(this.currentDirectory);
      jfilechooser.setMultiSelectionEnabled(this.multiSelectionEnabled);
      jfilechooser.setFileSelectionMode(this.mode.getJFileChooserValue());
      if (!this.defaultFile.isEmpty() & action == Action.Save) {
         File file1 = new File(this.defaultFile);
         jfilechooser.setSelectedFile(file1);
      }

      if (!this.dialogTitle.isEmpty()) {
         jfilechooser.setDialogTitle(this.dialogTitle);
      }

      if (action == Action.Open & !this.openButtonText.isEmpty()) {
         jfilechooser.setApproveButtonText(this.openButtonText);
      } else if (action == Action.Save & !this.saveButtonText.isEmpty()) {
         jfilechooser.setApproveButtonText(this.saveButtonText);
      }

      if (!this.filters.isEmpty()) {
         boolean flag = false;

         for (String[] astring : this.filters) {
            if (astring[1].equals("*")) {
               flag = true;
            } else {
               jfilechooser.addChoosableFileFilter(new FileNameExtensionFilter(astring[0], Arrays.copyOfRange(astring, 1, astring.length)));
            }
         }

         jfilechooser.setAcceptAllFileFilterUsed(flag);
      }

      int i;
      if (action == Action.Open) {
         i = jfilechooser.showOpenDialog(window);
      } else if (this.saveButtonText.isEmpty()) {
         i = jfilechooser.showSaveDialog(window);
      } else {
         i = jfilechooser.showDialog(window, null);
      }

      if (i == 0) {
         File[] afile;
         if (this.multiSelectionEnabled) {
            afile = jfilechooser.getSelectedFiles();
         } else {
            afile = new File[]{jfilechooser.getSelectedFile()};
         }

         this.selectedFiles = afile;
         this.currentDirectory = jfilechooser.getCurrentDirectory();
         return true;
      } else {
         return false;
      }
   }

   private boolean showWindowsFileChooser(Window window, Action action) {
      WindowsFileChooser windowsfilechooser = new WindowsFileChooser(this.currentDirectory);
      windowsfilechooser.setFilters(this.filters);
      windowsfilechooser.setMultiSelectionEnabled(this.multiSelectionEnabled);
      if (!this.defaultFile.isEmpty()) {
         windowsfilechooser.setDefaultFilename(this.defaultFile);
      }

      if (!this.dialogTitle.isEmpty()) {
         windowsfilechooser.setTitle(this.dialogTitle);
      }

      boolean flag = windowsfilechooser.showDialog(window, action == Action.Open);
      if (flag) {
         File[] afile;
         if (this.multiSelectionEnabled) {
            afile = windowsfilechooser.getSelectedFiles();
         } else {
            afile = new File[]{windowsfilechooser.getSelectedFile()};
         }

         this.selectedFiles = afile;
         this.currentDirectory = windowsfilechooser.getCurrentDirectory();
      }

      return flag;
   }

   private boolean showWindowsFolderBrowser(Window window) {
      WindowsFolderBrowser windowsfolderbrowser = new WindowsFolderBrowser();
      if (!this.dialogTitle.isEmpty()) {
         windowsfolderbrowser.setTitle(this.dialogTitle);
      }

      File file1 = windowsfolderbrowser.showDialog(window);
      if (file1 != null) {
         this.selectedFiles = new File[]{file1};
         this.currentDirectory = file1.getParentFile() != null ? file1.getParentFile() : file1;
         return true;
      } else {
         return false;
      }
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

   public void setMode(Mode mode) {
      this.mode = mode;
   }

   public Mode getMode() {
      return this.mode;
   }

   public void setCurrentDirectory(String s) {
      this.currentDirectory = s != null ? new File(s) : null;
   }

   public void setMultiSelectionEnabled(boolean flag) {
      this.multiSelectionEnabled = flag;
   }

   public boolean isMultiSelectionEnabled() {
      return this.multiSelectionEnabled;
   }

   public void setDefaultFileName(String s) {
      this.defaultFile = s;
   }

   public void setTitle(String s) {
      this.dialogTitle = s;
   }

   public void setOpenButtonText(String s) {
      this.openButtonText = s;
   }

   public void setSaveButtonText(String s) {
      this.saveButtonText = s;
   }

   public File[] getSelectedFiles() {
      return this.selectedFiles;
   }

   public File getSelectedFile() {
      return this.selectedFiles[0];
   }

   public File getCurrentDirectory() {
      return this.currentDirectory;
   }
}
