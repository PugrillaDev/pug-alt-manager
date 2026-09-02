package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.storage.RepositoryFileCodec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.Consumer;
import dev.pugrilla.jnafilechooser.api.WindowsFileChooser;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.util.EnumChatFormatting;
public final class SelectRepositoryScreen extends GuiScreen {
   private final GuiScreen previousScreen;
   private final AltManager altManager;
   private final Consumer<AccountRepository> selectionConsumer;
   private AccountRepository selectedRepository;
   private RepositoryListWidget repositoryList;
   private GuiButton manageButton;
   private GuiButton deleteAllButton;
   private GuiButton importButton;
   private GuiButton createButton;

   public SelectRepositoryScreen(GuiScreen guiscreen, AltManager altmanager, Consumer<AccountRepository> consumer) {
      this.previousScreen = guiscreen;
      this.altManager = altmanager;
      this.selectionConsumer = consumer;
      this.selectedRepository = altmanager.getStorageManager().getSelectedRepository();
   }

   public void initGui() {
      if (this.repositoryList == null) {
         this.repositoryList = new RepositoryListWidget(this.mc, this.altManager);
      }

      this.repositoryList.setDimensions(this.width, this.height, 32, this.height - 40);
      this.repositoryList.registerScrollButtons(7, 8);
      this.buttonList.clear();
      this.buttonList.add(this.deleteAllButton = new GuiButton(3, 3, 4, 90, 20, EnumChatFormatting.RED + "Delete All"));
      this.buttonList.add(this.importButton = new GuiButton(7, this.width - 97, 4, 90, 20, "Import..."));
      this.buttonList.add(this.manageButton = new GuiButton(1, this.width / 2 - 154, this.height - 28, 110, 20, "Manage repository"));
      this.buttonList.add(new GuiButton(6, this.width / 2 - 40, this.height - 28, 80, 20, "Confirm"));
      this.buttonList.add(this.createButton = new GuiButton(4, this.width / 2 + 44, this.height - 28, 110, 20, "Create repository"));
      this.setButtonsEnabled();
   }

   private void setButtonsEnabled() {
      this.manageButton.enabled = this.altManager.getMainScreen().getSelectedRepository() != null;
      this.deleteAllButton.visible = this.altManager.getStorageManager().getRepositoryCount() != 0;
      if (this.selectionConsumer != null) {
         this.manageButton.visible = this.deleteAllButton.visible = this.createButton.visible = false;
      }
   }

   protected void keyTyped(char c0, int i) throws IOException {
      if (i == 1 || i == 28) {
         this.back();
      }
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton.id == 4) {
         this.mc.displayGuiScreen(new CreateRepositoryScreen(this, this.altManager));
      } else if (guibutton.id == 6) {
         this.back();
      } else if (guibutton.id == 1) {
         AccountRepository AccountRepository = this.altManager.getMainScreen().getSelectedRepository();
         if (AccountRepository != null) {
            this.mc.displayGuiScreen(new ManageRepositoryScreen(this, this.altManager, AccountRepository));
         }
      } else if (guibutton.id == 3) {
         int i = this.altManager.getStorageManager().getRepositoryCount();
         this.mc
            .displayGuiScreen(
               new GuiYesNo(
                  (confirmed, id) -> {
                     this.mc.displayGuiScreen(this);
                     if (confirmed) {
                        this.altManager.getStorageManager().deleteAllRepositories();
                     }
                  },
                  "Are you sure you want to delete "
                     + EnumChatFormatting.GRAY
                     + i
                     + EnumChatFormatting.RESET
                     + " account repositor"
                     + (i == 1 ? "y" : "ies")
                     + "?",
                  "This action is not reversible.",
                  1337
               )
            );
      } else if (guibutton == this.importButton) {
         this.altManager.getThreadPool().execute(() -> {
            WindowsFileChooser chooser = new WindowsFileChooser(Objects.requireNonNull(System.getProperty("user.home"), new File("").getAbsolutePath()));
            chooser.setMultiSelectionEnabled(true);
            chooser.setTitle("Import Repository");
            chooser.setMaxNumberOfFiles(32);
            chooser.showOpenDialog(null);

            for (File file : chooser.getSelectedFiles()) {
               try {
                  RepositoryFileCodec.importRepository(this.altManager, Files.readAllBytes(file.toPath()));
               } catch (Throwable throwable) {
                  this.altManager.getLogger().warn("Failed to import repository file {}", new Object[]{file.getAbsolutePath(), throwable});
               }
            }
         });
      }
   }

   public void updateScreen() {
      this.setButtonsEnabled();
   }

   public void drawScreen(int i, int j, float f) {
      this.drawDefaultBackground();
      this.repositoryList.drawScreen(i, j, f);
      this.altManager
         .getMainScreen()
         .drawAccountManagerHeader(
            "Select Repository " + EnumChatFormatting.GRAY + "(" + this.altManager.getStorageManager().getRepositoryCount() + ")", this.width
         );
      super.drawScreen(i, j, f);
   }

   protected void mouseClicked(int i, int j, int k) throws IOException {
      this.repositoryList.mouseClicked(i, j, k);
      super.mouseClicked(i, j, k);
   }

   public void handleMouseInput() throws IOException {
      this.repositoryList.handleMouseInput();
      super.handleMouseInput();
   }

   private void back() {
      if (this.selectionConsumer != null) {
         this.selectionConsumer.accept(this.altManager.getStorageManager().getSelectedRepository());
         this.altManager.getStorageManager().setSelectedRepository(this.selectedRepository);
      }

      this.mc.displayGuiScreen(this.previousScreen);
   }

   public AltManager getAltManager() {
      return this.altManager;
   }
}
