package dev.pugrilla.altmanager.storage;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.account.AccountSortMode;
import dev.pugrilla.altmanager.account.AccountType;
import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.gui.AccountDoubleClickAction;
import dev.pugrilla.altmanager.skin.SkinVariant;
import dev.pugrilla.altmanager.util.AltManagerUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
public final class FileStorageManager implements StorageManager {
   private static final int STORAGE_VERSION = 0;
   private static final int STORAGE_MAGIC = 15837929;
   private static final String STORAGE_DIRECTORY_NAME = ".pugaltmanager";
   private static final String LEGACY_STORAGE_DIRECTORY_NAME = ".micsaltman";
   private final AltManager altManager;
   private final File storageDirectory;
   private final File accountsFile;
   private final List<AccountRepository> repositories = new ArrayList<>();
   private final List<AccountRepository> readOnlyRepositories = Collections.unmodifiableList(this.repositories);
   private Long autoSaveRequestedAt;
   private AccountRepository selectedRepository;
   private AccountType selectedAddAccountType = AccountType.values()[0];
   private boolean loaded;
   private String lastCookieFilePath = Objects.requireNonNull(System.getProperty("user.home"), new File("").getAbsolutePath());
   private String lastSkinFilePath = Objects.requireNonNull(System.getProperty("user.home"), new File("").getAbsolutePath());
   private final Map<UUID, Long> banExpiries = new HashMap<>();
   private AccountDoubleClickAction accountDoubleClickAction = AccountDoubleClickAction.LOGIN;
   private boolean showLoggedInUser = true;
   private boolean multiplayerButtonVisible = true;
   private boolean autoRefreshSession = true;
   private SkinVariant skinVariant = SkinVariant.CLASSIC;
   private AccountSortMode accountSortMode = AccountSortMode.values()[0];
   private String searchTerm = "";

   public FileStorageManager(AltManager altmanager) {
      this.altManager = altmanager;
      File appDataDirectory = new File(Objects.requireNonNull(System.getenv("APPDATA"), System.getProperty("user.home")));
      this.storageDirectory = new File(appDataDirectory, STORAGE_DIRECTORY_NAME);
      if (!this.storageDirectory.exists() && !this.storageDirectory.mkdirs()) {
         throw new RuntimeException("Failed to create directory: " + this.storageDirectory.getAbsolutePath());
      }

      this.migrateLegacyStorage(new File(appDataDirectory, LEGACY_STORAGE_DIRECTORY_NAME));

      this.accountsFile = new File(this.storageDirectory, "accounts.dat");

      try {
         this.load();
      } catch (IOException ioexception) {
         throw new RuntimeException(
            String.format("Account list is corrupted. Delete the file at %s if you want to continue.", this.accountsFile.getAbsolutePath()), ioexception
         );
      }

      try {
         if (!this.repositories.isEmpty()) {
            Files.copy(this.accountsFile.toPath(), new File(this.storageDirectory, "accounts.dat.backup").toPath(), StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Throwable throwable) {
      }

      this.loaded = true;
      new Thread(this::startPeriodicAutoSaveThread, "Pug Alt Manager Periodic Saving").start();
      Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownSave, "Pug Alt Manager Save"));
   }

   private void migrateLegacyStorage(File legacyDirectory) {
      if (!legacyDirectory.isDirectory()) {
         return;
      }

      String[] storageFiles = new String[]{"accounts.dat", "accounts.dat.backup"};
      for (String fileName : storageFiles) {
         File legacyFile = new File(legacyDirectory, fileName);
         File destinationFile = new File(this.storageDirectory, fileName);
         if (!legacyFile.isFile() || destinationFile.exists()) {
            continue;
         }

         try {
            Files.copy(legacyFile.toPath(), destinationFile.toPath());
            this.altManager.getLogger().info("Migrated legacy storage file {}", fileName);
         } catch (IOException migrationFailure) {
            this.altManager.getLogger().warn("Could not migrate legacy storage file " + fileName, migrationFailure);
         }
      }
   }

   private void startPeriodicAutoSaveThread() {
      while (true) {
         try {
            Thread.sleep(1000L);
         } catch (InterruptedException interruptedexception) {
            return;
         }

         Long olong = this.autoSaveRequestedAt;
         if (olong != null && System.currentTimeMillis() - olong > 120000L) {
            this.autoSaveRequestedAt = null;
            Minecraft.getMinecraft().addScheduledTask(() -> {
               try {
                  this.save();
                  this.altManager.getLogger().info("Successful account auto-save");
               } catch (Throwable throwable) {
                  this.altManager.getLogger().error("Failed to auto-save", throwable);
                  this.autoSaveRequestedAt = System.currentTimeMillis();
               }
            });
         }
      }
   }

   public void setAutoSaveNowIfRequired() {
      if (this.autoSaveRequestedAt != null) {
         this.autoSaveRequestedAt = 0L;
         this.altManager.getLogger().info("Forced auto-save sequence to start next second");
      }
   }

   public void setAutoSaveRequired() {
      if (this.autoSaveRequestedAt == null) {
         this.autoSaveRequestedAt = System.currentTimeMillis();
         this.altManager.getLogger().info("Started auto-save sequence");
      }
   }

   private void load() throws IOException {
      if (!this.accountsFile.exists()) {
         this.altManager.getLogger().info("Account list doesn't exist. Starting from scratch");
      } else {
         byte[] abyte = Files.readAllBytes(this.accountsFile.toPath());
         this.fromBytes(abyte);
      }
   }

   private void fromBytes(byte[] abyte) throws IOException {
      DataInputStream datainputstream = new DataInputStream(new ByteArrayInputStream(abyte));
      int i = datainputstream.readInt();
      if (i != 15837929) {
         throw new IOException(String.format("Corrupted magic header: %s", Integer.toHexString(i)));
      }

      int j = datainputstream.readInt();
      if (j > 0) {
         throw new IOException(String.format("Loading accounts from a newer version: %d, current is %d", j, 0));
      }

      long k = datainputstream.readLong();
      this.selectedAddAccountType = AccountType.values()[datainputstream.readUnsignedByte()];
      this.lastCookieFilePath = datainputstream.readUTF();
      this.lastSkinFilePath = datainputstream.readUTF();
      this.skinVariant = SkinVariant.values()[datainputstream.readUnsignedByte()];
      this.accountSortMode = AccountSortMode.values()[datainputstream.readUnsignedByte()];
      this.searchTerm = datainputstream.readUTF();
      int l = datainputstream.readInt();
      int i1 = datainputstream.readInt();

      for (int j1 = 0; j1 < i1; j1++) {
         AccountRepository AccountRepository = readRepository(this.altManager, datainputstream);
         this.repositories.add(AccountRepository);
         if (j1 == l) {
            this.setSelectedRepository(AccountRepository);
         }
      }

      if (!this.repositories.isEmpty() && this.getSelectedRepository() == null) {
         this.setSelectedRepository((AccountRepository)this.repositories.get(0));
      }

      int k1 = datainputstream.readInt();

      for (int l1 = 0; l1 < k1; l1++) {
         UUID uuid = new UUID(datainputstream.readLong(), datainputstream.readLong());
         this.banExpiries.put(uuid, datainputstream.readLong());
      }

      this.accountDoubleClickAction = AccountDoubleClickAction.values()[datainputstream.readInt()];
      this.multiplayerButtonVisible = datainputstream.readBoolean();
      this.showLoggedInUser = datainputstream.readBoolean();
   }
   public static AccountRepository readRepository(AltManager altmanager, DataInputStream datainputstream) throws IOException {
      RepositoryEncryption RepositoryEncryption = new RepositoryEncryption(datainputstream.readBoolean());
      if (RepositoryEncryption.isEnabled()) {
         RepositoryEncryption.setTestString(datainputstream.readUTF());
      }

      AccountRepository AccountRepository = new AccountRepository(datainputstream.readUTF(), altmanager, datainputstream.readLong(), RepositoryEncryption);
      AccountRepository.setLastAccessed(datainputstream.readLong());
      if (AccountRepository.getLastAccessed() > System.currentTimeMillis() || AccountRepository.getCreationTime() > System.currentTimeMillis()) {
         altmanager.getLogger()
            .warn(
               "Repository {} is created/accessed in the future! This may break a lot of things! Last accessed: {} Last created: {}",
               new Object[]{
                  AccountRepository.getName(),
                  AltManagerUtils.DATE_TIME_FORMAT.format(Date.from(Instant.ofEpochMilli(AccountRepository.getLastAccessed()))),
                  AltManagerUtils.DATE_TIME_FORMAT.format(Date.from(Instant.ofEpochMilli(AccountRepository.getCreationTime())))
               }
            );
      }

      int i = datainputstream.readInt();
      int j = datainputstream.readInt();

      for (int k = 0; k < j; k++) {
         AccountType accountType = AccountType.values()[datainputstream.readUnsignedByte()];
         String s = datainputstream.readUTF();
         UUID uuid = new UUID(datainputstream.readLong(), datainputstream.readLong());
         String s1 = datainputstream.readUTF();
         long l = datainputstream.readLong();
         long i1 = datainputstream.readLong();
         long j1 = datainputstream.readLong();
         byte[] abyte = new byte[datainputstream.readInt()];
         datainputstream.readFully(abyte);
         AbstractAccount AbstractAccount = accountType.getSupplier().create(s, uuid, s1);
         AbstractAccount.setLastUsed(l);
         AbstractAccount.setCreationTime(i1);
         AbstractAccount.setLastRefresh(j1);
         AccountRepository.addAccount(AbstractAccount);
         AbstractAccount.deserializeDataFromBytes(abyte);
         if (i == k) {
            AccountRepository.setSelectedAccount(AbstractAccount);
         }
      }

      return AccountRepository;
   }
   public static void writeRepository(AccountRepository AccountRepository, DataOutputStream dataoutputstream) throws IOException {
      dataoutputstream.writeBoolean(AccountRepository.getEncryption().isEnabled());
      if (AccountRepository.getEncryption().isEnabled()) {
         dataoutputstream.writeUTF(AccountRepository.getEncryption().getTestString());
      }

      dataoutputstream.writeUTF(AccountRepository.getName());
      dataoutputstream.writeLong(AccountRepository.getCreationTime());
      dataoutputstream.writeLong(AccountRepository.getLastAccessed());
      List<AbstractAccount> list = AccountRepository.getAccountList();
      dataoutputstream.writeInt(list.indexOf(AccountRepository.getSelectedAccount()));
      dataoutputstream.writeInt(list.size());

      for (AbstractAccount AbstractAccount : list) {
         dataoutputstream.writeByte(AbstractAccount.getAccountType().ordinal());
         dataoutputstream.writeUTF(AbstractAccount.getUsername());
         dataoutputstream.writeLong(AbstractAccount.getUuid().getMostSignificantBits());
         dataoutputstream.writeLong(AbstractAccount.getUuid().getLeastSignificantBits());
         dataoutputstream.writeUTF(AbstractAccount.getAccessToken());
         dataoutputstream.writeLong(AbstractAccount.getLastUsed());
         dataoutputstream.writeLong(AbstractAccount.getCreationTime());
         dataoutputstream.writeLong(AbstractAccount.getLastRefresh());
         byte[] abyte = AbstractAccount.serializeDataToBytes();
         dataoutputstream.writeInt(abyte.length);
         dataoutputstream.write(abyte);
      }
   }

   public void shutdownSave() {
      try {
         this.save();
      } catch (Throwable throwable) {
         this.altManager.getLogger().error("Failed to save accounts on shutdown", throwable);
      }
   }

   public void save() throws IOException {
      byte[] abyte = this.toBytes();
      FileOutputStream fileoutputstream = new FileOutputStream(new File(this.storageDirectory, "accounts.dat"));
      fileoutputstream.write(abyte);
      fileoutputstream.close();
      this.autoSaveRequestedAt = null;
   }

   private byte[] toBytes() throws IOException {
      if (!this.loaded) {
         throw new IllegalStateException("Storage not loaded yet! can't save");
      }

      ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
      DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
      dataoutputstream.writeInt(15837929);
      dataoutputstream.writeInt(0);
      dataoutputstream.writeLong(System.currentTimeMillis());
      dataoutputstream.writeByte(this.selectedAddAccountType.ordinal());
      dataoutputstream.writeUTF(this.lastCookieFilePath);
      dataoutputstream.writeUTF(this.lastSkinFilePath);
      dataoutputstream.writeByte(this.skinVariant.ordinal());
      dataoutputstream.writeByte(this.accountSortMode.ordinal());
      dataoutputstream.writeUTF(this.searchTerm);
      dataoutputstream.writeInt(this.repositories.indexOf(this.getSelectedRepository()));
      dataoutputstream.writeInt(this.repositories.size());

      for (AccountRepository AccountRepository : this.repositories) {
         writeRepository(AccountRepository, dataoutputstream);
      }

      dataoutputstream.writeInt(this.banExpiries.size());

      for (Entry entry : this.banExpiries.entrySet()) {
         dataoutputstream.writeLong(((UUID)entry.getKey()).getMostSignificantBits());
         dataoutputstream.writeLong(((UUID)entry.getKey()).getLeastSignificantBits());
         dataoutputstream.writeLong((Long)entry.getValue());
      }

      dataoutputstream.writeInt(this.accountDoubleClickAction.ordinal());
      dataoutputstream.writeBoolean(this.multiplayerButtonVisible);
      dataoutputstream.writeBoolean(this.showLoggedInUser);
      return bytearrayoutputstream.toByteArray();
   }

   public File getDirectory() {
      return this.storageDirectory;
   }

   @Override
   public List<AccountRepository> getRepositories() {
      return this.readOnlyRepositories;
   }

   @Override
   public void createRepository(AccountRepository AccountRepository) {
      if (this.repositories.contains(AccountRepository)) {
         throw new IllegalStateException();
      }

      this.altManager.getLogger().info("Created account repository {}", new Object[]{AccountRepository.getName()});
      this.repositories.add(AccountRepository);
      this.setAutoSaveRequired();
   }

   @Override
   public int getRepositoryCount() {
      return this.repositories.size();
   }

   @Override
   public void deleteRepository(AccountRepository AccountRepository) {
      if (this.repositories.remove(AccountRepository)) {
         this.altManager.getLogger().info("Deleted account repository {}", new Object[]{AccountRepository.getName()});
         this.setAutoSaveRequired();
      }

      this.checkSelected();
   }

   @Override
   public void deleteAllRepositories() {
      this.repositories.clear();
      this.checkSelected();
      this.setAutoSaveRequired();
   }

   @Override
   public AccountRepository getSelectedRepository() {
      return this.selectedRepository;
   }

   @Override
   public void setSelectedRepository(AccountRepository AccountRepository) {
      this.selectedRepository = AccountRepository;
      if (this.altManager.getMainScreen() != null) {
         this.altManager.getMainScreen().getMultiSelectionAccounts().clear();
      }
   }

   @Override
   public AccountType getSelectedAddAccountType() {
      return this.selectedAddAccountType;
   }

   @Override
   public void setSelectedAddAccountType(AccountType AccountType) {
      this.selectedAddAccountType = Objects.requireNonNull(AccountType);
      this.setAutoSaveRequired();
   }

   @Override
   public void setLastCookieFilePath(String s) {
      this.lastCookieFilePath = s;
      this.setAutoSaveRequired();
   }

   @Override
   public long getBanExpiry(UUID uuid) {
      return this.banExpiries.getOrDefault(uuid, 0L);
   }

   @Override
   public void setBanExpiry(UUID uuid, long i) {
      this.banExpiries.put(uuid, i);
      this.setAutoSaveRequired();
   }

   @Override
   public AccountDoubleClickAction getAccountDoubleClickAction() {
      return this.accountDoubleClickAction;
   }

   @Override
   public void setAccountDoubleClickAction(AccountDoubleClickAction AccountDoubleClickAction) {
      this.accountDoubleClickAction = AccountDoubleClickAction;
   }

   @Override
   public boolean isShowLoggedInUser() {
      return this.showLoggedInUser;
   }

   @Override
   public void setShowLoggedInUser(boolean flag) {
      this.showLoggedInUser = flag;
   }

   @Override
   public boolean isMultiplayerButtonVisible() {
      return this.multiplayerButtonVisible;
   }

   @Override
   public void setMultiplayerButtonVisible(boolean flag) {
      this.multiplayerButtonVisible = flag;
   }

   @Override
   public String getLastCookieFilePath() {
      return this.lastCookieFilePath;
   }

   @Override
   public String getLastSkinFilePath() {
      return this.lastSkinFilePath;
   }

   @Override
   public void setLastSkinFilePath(String s) {
      this.lastSkinFilePath = s;
      this.setAutoSaveRequired();
   }

   @Override
   public SkinVariant getSkinVariant() {
      return this.skinVariant;
   }

   @Override
   public void setSkinVariant(SkinVariant SkinVariant) {
      this.skinVariant = SkinVariant;
      this.setAutoSaveRequired();
   }

   @Override
   public void setAccountSortMode(AccountSortMode AccountSortMode) {
      this.accountSortMode = AccountSortMode;
      this.setAutoSaveRequired();
   }

   @Override
   public AccountSortMode getAccountSortMode() {
      return this.accountSortMode;
   }

   @Override
   public String getSearchTerm() {
      return this.searchTerm;
   }

   @Override
   public void setSearchTerm(String s) {
      this.searchTerm = s;
   }

   @Override
   public boolean isAutoRefreshSession() {
      return this.autoRefreshSession;
   }

   @Override
   public void setAutoRefreshSession(boolean flag) {
      this.autoRefreshSession = flag;
   }

   private void checkSelected() {
      AccountRepository AccountRepository = this.altManager.getMainScreen().getSelectedRepository();
      if (!this.repositories.contains(AccountRepository)) {
         this.setSelectedRepository(this.repositories.isEmpty() ? null : (AccountRepository)this.repositories.get(0));
      }
   }
}
