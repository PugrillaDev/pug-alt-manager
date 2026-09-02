package dev.pugrilla.altmanager.gui;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.AccountType;
import dev.pugrilla.altmanager.account.BrowserAccount;
import dev.pugrilla.altmanager.account.CookieAccount;
import dev.pugrilla.altmanager.account.CredentialsAccount;
import dev.pugrilla.altmanager.account.LoginResponseType;
import dev.pugrilla.altmanager.account.LoginResult;
import dev.pugrilla.altmanager.account.SessionAccount;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.auth.Cookie;
import dev.pugrilla.altmanager.auth.MinecraftServicesApi;
import dev.pugrilla.altmanager.util.AltManagerUtils;
import dev.pugrilla.altmanager.util.AsyncTaskLock;
import dev.pugrilla.altmanager.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import dev.pugrilla.jnafilechooser.api.WindowsFileChooser;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
public final class AddAccountScreen extends GuiScreen {
   private final AltManager altManager;
   private final GuiScreen previousScreen;
   private final AccountRepository repository;
   private final List<AccountTypeButton> accountTypeButtons = new ArrayList<>();
   private GuiTextField primaryField;
   private GuiTextField secondaryField;
   private GuiTextField tokenField;
   private List<GuiTextField> textFields;
   private GuiButton createButton;
   private GuiButton cancelButton;
   private GuiButton browseButton;
   private GuiButton pasteButton;
   private GuiButton randomButton;
   private GuiButton copyLinkButton;
   private GuiButton openLinkButton;
   private final StatusMessageRenderer statusRenderer = new StatusMessageRenderer();
   private final AsyncTaskLock taskLock;
   private AccountType displayedAccountType;
   private boolean cookieFileValid;
   private String resolvedUuid = "";

   public AddAccountScreen(AltManager altmanager, GuiScreen guiscreen, AccountRepository AccountRepository) {
      this.altManager = altmanager;
      this.previousScreen = guiscreen;
      this.repository = Objects.requireNonNull(AccountRepository, "adding account to null repository");
      this.taskLock = new AsyncTaskLock(altmanager, throwable -> this.statusRenderer.setStatus("An error occurred, please check console", 2000));
   }

   public void initGui() {
      this.addControlButtons();
      this.setTextFields();
      this.addAccountTypeButtons();
   }

   private void addControlButtons() {
      this.buttonList.clear();
      this.buttonList.add(this.createButton = new GuiButton(0, this.width / 2 - 100, 0, "Create"));
      this.buttonList.add(this.cancelButton = new GuiButton(1, this.width / 2 - 100, 0, "Cancel"));
      this.buttonList.add(this.pasteButton = new GuiButton(2, 0, 0, 40, 20, "Paste"));
      this.buttonList.add(this.browseButton = new GuiButton(2, 0, 0, 20, 20, "..."));
      this.buttonList.add(this.randomButton = new GuiButton(3, 0, 0, 50, 20, "Random"));
      this.buttonList.add(this.copyLinkButton = new GuiButton(4, 0, 0, 98, 20, "Copy Link"));
      this.buttonList.add(this.openLinkButton = new GuiButton(5, 0, 0, 98, 20, "Open Link"));
   }

   private void setTextFields() {
      if (this.primaryField == null) {
         this.primaryField = new GuiTextField(0, this.fontRendererObj, 0, 68, 200, 20);
      }

      if (this.secondaryField == null) {
         this.secondaryField = new GuiTextField(0, this.fontRendererObj, 0, this.primaryField.yPosition + 40, 200, 20);
      }

      if (this.tokenField == null) {
         this.tokenField = new GuiTextField(0, this.fontRendererObj, 0, this.secondaryField.yPosition + 40, 200, 20);
      }

      this.textFields = Arrays.asList(this.primaryField, this.secondaryField, this.tokenField);
      this.primaryField.xPosition = this.secondaryField.xPosition = this.tokenField.xPosition = this.width / 2 - 100;
      this.browseButton.xPosition = this.randomButton.xPosition = this.pasteButton.xPosition = this.primaryField.xPosition
         + this.primaryField.width
         + 6;
      this.browseButton.yPosition = this.pasteButton.yPosition = this.primaryField.yPosition;
      this.randomButton.yPosition = this.secondaryField.yPosition;
      this.copyLinkButton.yPosition = this.openLinkButton.yPosition = this.secondaryField.yPosition + 24;
      this.copyLinkButton.xPosition = this.createButton.xPosition;
      this.openLinkButton.xPosition = this.copyLinkButton.xPosition + 100;
      this.setButtonsAndVisibility();
   }

   protected void actionPerformed(GuiButton guibutton) throws IOException {
      if (guibutton.id == 0) {
         if (this.taskLock.isLocked()) {
            return;
         }

         if (this.getAccountType() == AccountType.COOKIE) {
            List<CookieAccount> list = this.parseCookieFile();
            if (list != null) {
               AtomicInteger atomicinteger = new AtomicInteger();

               for (CookieAccount account : list) {
                  this.taskLock.execute(() -> {
                     LoginResult result = account.login();
                     this.mc.addScheduledTask(() -> this.login(account, result, atomicinteger.incrementAndGet() == list.size()));
                  });
               }
            }
         } else {
            AbstractAccount account = Objects.requireNonNull(this.createAccountInstance());
            if (account instanceof SessionAccount) {
               this.login(account, account.login(), true);
            } else {
               this.taskLock.execute(() -> {
                  LoginResult result = account.login();
                  this.mc.addScheduledTask(() -> this.login(account, result, true));
               });
            }
         }

         this.setButtonsAndVisibility();
      } else if (guibutton.id == 1) {
         this.back();
      } else if (guibutton == this.browseButton) {
         this.altManager.getThreadPool().execute(() -> {
            List<File> files = this.getCookieFiles();
            File initial = files.isEmpty() ? new File("") : files.get(0);
            WindowsFileChooser chooser = new WindowsFileChooser(initial.isDirectory() ? initial.getAbsolutePath() : initial.getParent());
            chooser.setMultiSelectionEnabled(true);
            chooser.setTitle("Select Cookie(s)");
            chooser.setMaxNumberOfFiles(32);
            chooser.showOpenDialog(null);
            File[] selectedFiles = chooser.getSelectedFiles();
            if (selectedFiles.length != 0) {
               this.primaryField.setText(Arrays.stream(selectedFiles).map(File::getAbsolutePath).collect(Collectors.joining("|")));
            }

            this.setCookieFileValid();
         });
      } else if (guibutton == this.randomButton) {
         this.secondaryField.setText(UUID.randomUUID().toString().replace("-", ""));
      } else if (guibutton == this.pasteButton) {
         String s = AltManagerUtils.getClipboardText();
         if (s != null) {
            if (this.getAccountType() == AccountType.SESSION) {
               String[] astring = this.decodeSessionAccount(s);
               if (astring != null) {
                  this.primaryField.setText(astring[0]);
                  this.secondaryField.setText(astring[1]);
                  this.tokenField.setText(astring[2]);
               }
            } else {
               String[] astring1 = AltManagerUtils.parseCredentialsLine(s);
               if (astring1 != null) {
                  this.primaryField.setText(astring1[0]);
                  this.secondaryField.setText(astring1[1]);
               }
            }
         }
      } else if (guibutton == this.copyLinkButton || guibutton == this.openLinkButton) {
         String s1 = this.retrieveBrowserLoginLink();
         if (guibutton == this.openLinkButton) {
            BrowserAccount.openUrl(s1);
         } else {
            AltManagerUtils.setClipboardText(s1);
         }
      }
   }

   private String[] decodeSessionAccount(String s) {
      try {
         String[] astring = s.split(":", 3);
         if (astring.length == 3) {
            return astring;
         }

         astring = AltManagerUtils.decryptString(s).split(":", 3);
         return astring.length == 3 ? astring : null;
      } catch (Throwable throwable) {
         return null;
      }
   }

   private void login(AbstractAccount AbstractAccount, LoginResult LoginResult, boolean flag) {
      if (LoginResult.isSuccessfulLogin()) {
         this.repository.addAccount(AbstractAccount);
         this.altManager.getStorageManager().setAutoSaveRequired();
         if (flag) {
            this.back();
         }
      } else {
         if (LoginResult.getResponseType() == LoginResponseType.NO_MINECRAFT_PROFILE) {
            this.mc.displayGuiScreen(new CreateProfileScreen(this.previousScreen, this.altManager, AbstractAccount, this.repository));
            return;
         }

         this.statusRenderer.setStatus(LoginResult.getResponseType().getDescription(), 1500);
      }
   }

   private AbstractAccount createAccountInstance() {
      switch (this.getAccountType()) {
         case SESSION:
            return new SessionAccount(this.primaryField.getText(), AltManagerUtils.parseUuid(this.secondaryField.getText()), this.tokenField.getText());
         case CREDENTIALS:
            return new CredentialsAccount(this.primaryField.getText(), this.secondaryField.getText());
         case BROWSER:
            return new BrowserAccount(this.secondaryField.getText());
         default:
            throw new IllegalStateException();
      }
   }

   private List<CookieAccount> parseCookieFile() {
      ArrayList<CookieAccount> arraylist = new ArrayList<>();
      List<File> list = this.getCookieFiles();

      for (File file1 : list) {
         byte[] abyte;
         try {
            abyte = Files.readAllBytes(file1.toPath());
         } catch (IOException ioexception) {
            continue;
         }

         if (abyte.length >= 32) {
            if (ByteBuffer.wrap(abyte, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt() == 1347093252) {
               try {
                  ZipInputStream zipinputstream;
                  ZipEntry zipentry;
                  for (zipinputstream = new ZipInputStream(new ByteArrayInputStream(abyte));
                     (zipentry = zipinputstream.getNextEntry()) != null;
                     zipinputstream.closeEntry()
                  ) {
                     if (!zipentry.isDirectory()) {
                        byte[] abyte1 = IOUtils.readAllBytes(zipinputstream);
                        arraylist.add(new CookieAccount(abyte1));
                     }
                  }

                  zipinputstream.close();
               } catch (Throwable throwable) {
                  this.altManager.getLogger().warn("Failed to process ZIP file {}", new Object[]{file1, throwable});
               }
            } else {
               arraylist.add(new CookieAccount(abyte));
            }
         }
      }

      arraylist.removeIf(CookieAccount::isEmpty);
      if (arraylist.isEmpty()) {
         this.statusRenderer.setStatus("Failed to read cookie file" + (list.size() > 1 ? "s" : ""), 1500);
         return null;
      } else {
         return arraylist;
      }
   }

   public void onGuiClosed() {
      super.onGuiClosed();
      BrowserAccount.stopOAuthServer();
   }

   protected void keyTyped(char c0, int i) throws IOException {
      boolean flag = this.primaryField.textboxKeyTyped(c0, i);
      if (!flag && !this.secondaryField.textboxKeyTyped(c0, i) && !this.tokenField.textboxKeyTyped(c0, i)) {
         if (i == 1 && this.cancelButton.enabled) {
            this.back();
         }
      } else {
         if (flag && this.getAccountType() == AccountType.COOKIE) {
            this.setCookieFileValid();
         }

         if (flag && this.getAccountType() == AccountType.SESSION) {
            this.setPlayerUuid();
         }
      }

      this.setButtonsAndVisibility();
   }

   private void setPlayerUuid() {
      String s = this.primaryField.getText();
      if (AltManagerUtils.MINECRAFT_USERNAME_PATTERN.matcher(s).matches()) {
         if (!this.resolvedUuid.equals(s)) {
            this.resolvedUuid = s;
            this.altManager.getThreadPool().execute(() -> {
               String uuid;
               try {
                  uuid = MinecraftServicesApi.getUuidForUsername(s);
               } catch (IOException exception) {
                  uuid = "";
               }

               if (this.primaryField.getText().equals(s)) {
                  final String resolved = uuid;
                  this.mc.addScheduledTask(() -> {
                     this.secondaryField.setText(resolved);
                     this.setButtonsAndVisibility();
                  });
               }
            });
         }
      }
   }

   private void back() {
      this.mc.displayGuiScreen(this.previousScreen);
   }

   private void addAccountTypeButtons() {
      this.accountTypeButtons.clear();
      byte b0 = 7;
      AccountType[] accountTypes = AccountType.values();
      int i = Arrays.stream(accountTypes).mapToInt(type -> this.fontRendererObj.getStringWidth(type.getName()) + 7).sum() - 7;
      int j = 0;

      for (AccountType accountType : accountTypes) {
         int k = this.fontRendererObj.getStringWidth(accountType.getName());
         this.accountTypeButtons.add(new AccountTypeButton(this, (int)(this.width / 2.0F - i / 2.0F + j), 32, accountType));
         j += k + 7;
      }
   }

   public void drawScreen(int i, int j, float f) {
      this.drawDefaultBackground();
      this.drawCenteredString(
         this.fontRendererObj, "Add account to repository " + EnumChatFormatting.GRAY + this.repository.getName(), this.width / 2, 17, 16777215
      );

      for (AccountTypeButton AccountTypeButton : this.accountTypeButtons) {
         AccountTypeButton.draw(i, j);
      }

      this.statusRenderer.draw(this.width / 2, 44);
      this.drawString(
         this.fontRendererObj,
         this.getAccountType() == AccountType.COOKIE
            ? "Cookie File(s) (ZIP, folder, text)"
            : (this.getAccountType() == AccountType.BROWSER ? "Link" : "Username"),
         this.width / 2 - 100,
         this.primaryField.yPosition - 12,
         10526880
      );
      this.primaryField.drawTextBox();
      if (this.secondaryField.getVisible()) {
         this.drawString(
            this.fontRendererObj,
            this.getAccountType() == AccountType.BROWSER
               ? "Refresh Token"
               : (
                  this.getAccountType() == AccountType.SESSION
                     ? (!AltManagerUtils.isUuid(this.secondaryField.getText()) ? EnumChatFormatting.RED : "") + "UUID"
                     : "Password"
               ),
            this.width / 2 - 100,
            this.secondaryField.yPosition - 12,
            10526880
         );
         this.secondaryField.drawTextBox();
      }

      if (this.tokenField.getVisible()) {
         this.drawString(this.fontRendererObj, "Token", this.width / 2 - 100, this.tokenField.yPosition - 12, 10526880);
         this.tokenField.drawTextBox();
      }

      super.drawScreen(i, j, f);
   }

   public void updateScreen() {
      this.primaryField.updateCursorCounter();
      this.secondaryField.updateCursorCounter();
      this.tokenField.updateCursorCounter();
      AccountType AccountType = this.getAccountType();
      if (AccountType == AccountType.BROWSER) {
         String s = this.retrieveBrowserLoginLink();
         this.primaryField.setText(s == null ? "Loading..." : s);
      }

      this.setButtonsAndVisibility();
   }

   private String retrieveBrowserLoginLink() {
      return BrowserAccount.startOAuthServerAndGetLoginUrl(this::addBrowserAccount);
   }

   private void addBrowserAccount(String s) {
      this.statusRenderer.setStatus(EnumChatFormatting.GREEN + "Processing refresh token...", 1500);
      this.secondaryField.setText(s);
      this.taskLock.execute(() -> {
         BrowserAccount account = new BrowserAccount(s);
         LoginResult result = account.login();
         this.mc.addScheduledTask(() -> this.login(account, result, true));
      });
   }

   private void setButtonsAndVisibility() {
      AccountType AccountType = this.getAccountType();
      this.primaryField.setMaxStringLength(AccountType == AccountType.COOKIE ? 512 : 64);
      this.tokenField.setMaxStringLength(1024);
      this.secondaryField.setMaxStringLength(1024);
      if (this.displayedAccountType != AccountType) {
         this.displayedAccountType = AccountType;

         for (GuiTextField guitextfield : this.textFields) {
            guitextfield.setFocused(false);
            guitextfield.setText("");
         }

         if (AccountType == AccountType.COOKIE) {
            this.primaryField.setText(this.altManager.getStorageManager().getLastCookieFilePath());
            this.setCookieFileValid();
         }

         this.primaryField.setFocused(true);
      }

      this.pasteButton.visible = AccountType == AccountType.SESSION || AccountType == AccountType.CREDENTIALS;
      this.browseButton.visible = AccountType == AccountType.COOKIE;
      this.randomButton.visible = AccountType == AccountType.SESSION;
      this.tokenField.setVisible(AccountType == AccountType.SESSION);
      this.secondaryField.setVisible(AccountType != AccountType.COOKIE);
      boolean flag1 = AccountType != AccountType.BROWSER;
      this.primaryField.setEnabled(flag1);
      if (!flag1 && this.primaryField.isFocused()) {
         this.primaryField.setFocused(false);
      }

      this.copyLinkButton.visible = this.openLinkButton.visible = AccountType == AccountType.BROWSER;
      boolean flag2 = true;
      if (flag2 && AccountType == AccountType.SESSION && !AltManagerUtils.isUuid(this.secondaryField.getText())) {
         flag2 = false;
      }

      if (flag2 && AccountType == AccountType.CREDENTIALS && (this.secondaryField.getText().isEmpty() || this.primaryField.getText().isEmpty())) {
         flag2 = false;
      }

      if (flag2 && AccountType == AccountType.COOKIE && !this.cookieFileValid) {
         flag2 = false;
      }

      if (flag2 && AccountType == AccountType.BROWSER && this.secondaryField.getText().isEmpty()) {
         flag2 = false;
      }

      boolean flag = this.taskLock.isLocked();
      this.createButton.enabled = !flag && flag2;
      this.cancelButton.enabled = !flag;
      int i = this.height / 4 + 96;

      while (this.isCollidingWithTextFields(i)) {
         i += 4;
      }

      this.createButton.yPosition = i;
      this.cancelButton.yPosition = i + 24;
   }

   AltManager getAltManager() {
      return altManager;
   }

   net.minecraft.client.gui.FontRenderer getFontRenderer() {
      return fontRendererObj;
   }

   void refreshAccountTypeControls() {
      setButtonsAndVisibility();
   }

   private List<File> getCookieFiles() {
      ArrayList<File> arraylist = new ArrayList<>();
      String s = this.primaryField.getText();
      String[] astring = s.split("\\|");

      for (String s1 : astring) {
         if (s1.length() >= 2) {
            arraylist.add(new File(s1));
         }
      }

      return arraylist;
   }

   public void setCookieFileValid() {
      this.cookieFileValid = this.getCookieFiles().stream().anyMatch(File::exists);
      if (this.cookieFileValid) {
         this.altManager.getStorageManager().setLastCookieFilePath(this.primaryField.getText());
      }
   }

   private boolean isCollidingWithTextFields(int i) {
      for (GuiTextField guitextfield : this.textFields) {
         if (i <= guitextfield.yPosition + guitextfield.height + 9) {
            return true;
         }
      }

      return false;
   }

   private AccountType getAccountType() {
      return this.altManager.getStorageManager().getSelectedAddAccountType();
   }

   protected void mouseClicked(int i, int j, int k) throws IOException {
      this.primaryField.mouseClicked(i, j, k);
      this.secondaryField.mouseClicked(i, j, k);
      this.tokenField.mouseClicked(i, j, k);

      for (AccountTypeButton AccountTypeButton : this.accountTypeButtons) {
         AccountTypeButton.mouseClicked(i, j, k);
      }

      super.mouseClicked(i, j, k);
   }
}
