package dev.pugrilla.altmanager.account;

import dev.pugrilla.altmanager.AltManager;
import dev.pugrilla.altmanager.client.MinecraftReflection;
import dev.pugrilla.altmanager.client.SessionController;
import dev.pugrilla.altmanager.auth.SessionRefreshPolicy;
import dev.pugrilla.altmanager.auth.MinecraftServicesApi;
import dev.pugrilla.altmanager.gui.AccountDoubleClickAction;
import dev.pugrilla.altmanager.gui.AltManagerScreen;
import dev.pugrilla.altmanager.gui.PlayerHeadRenderer;
import dev.pugrilla.altmanager.network.BanCheckNetworkManager;
import dev.pugrilla.altmanager.util.AltManagerUtils;
import dev.pugrilla.altmanager.util.ColorUtils;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Session;
public abstract class AbstractAccount implements IGuiListEntry {
   protected static final String UNINITIALIZED_USERNAME = "UninitAcc";
   protected static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
   private final AccountType accountType;
   private final Object loginLock = new Object();
   private volatile String username;
   private volatile UUID uuid;
   private volatile String accessToken;
   private long lastUsed;
   private long creationTime;
   private volatile long lastRefresh;
   private volatile AccountRepository repository;
   private PlayerHeadRenderer playerHead;
   private long lastRenderTime;
   private int consecutiveRenders;
   private long lastClickTime;
   private volatile boolean doubleClickActionRunning;
   private byte[] pendingEncryptedData;
   private boolean pendingDataContainsAccessToken;

   protected AbstractAccount(AccountType AccountType, String s, UUID uuid, String s1) {
      this.accountType = AccountType;
      this.username = s;
      this.uuid = uuid;
      this.accessToken = s1;
   }

   public final long getCreationTime() {
      return this.creationTime;
   }

   public final void setCreationTime(long i) {
      this.creationTime = i;
   }

   public final void setLastRefresh(long i) {
      this.lastRefresh = i;
   }

   public final long getLastRefresh() {
      return this.lastRefresh;
   }

   public final boolean shouldRefreshSessionAutomatically() {
      return this.repository != null && this.repository.getEncryption().isDecrypted()
         && this.repository.getAltManager().getStorageManager().isAutoRefreshSession()
         && this.getAccountType() != AccountType.SESSION
         && SessionRefreshPolicy.needsRefresh(this.accessToken, this.lastRefresh, System.currentTimeMillis());
   }

   public final LoginResult login() {
      synchronized (this.loginLock) {
         LoginResult result = this.loginImpl();
         Session session = result.getSession();
         synchronized (this) {
            if (session != null && result.getResponseType() == LoginResponseType.SUCCESS) {
               this.username = session.getUsername();
               this.setUuid(session.getProfile().getId());
               this.accessToken = session.getToken();
               this.setLastRefresh(System.currentTimeMillis());
            } else if (session != null && result.getResponseType() == LoginResponseType.NO_MINECRAFT_PROFILE) {
               this.accessToken = session.getToken();
            }
         }
         AccountRepository owner = this.repository;
         if (owner != null && owner.getAltManager() != null) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
               owner.invalidateViewingAccountList();
               owner.getAltManager().getStorageManager().setAutoSaveRequired();
            });
         }
         return result;
      }
   }

   public final void setMinecraftSessionAndRefreshIfNeeded() {
      if (this.repository != null) {
         this.repository.getAltManager().getSessionController().useAccount(this, true, false);
      }
   }

   public final LoginResult prepareSession(boolean autoRefresh, boolean forceRefresh) {
      synchronized (this.loginLock) {
         boolean refreshable = this.getAccountType() != AccountType.SESSION;
         if (autoRefresh && refreshable && (forceRefresh
            || SessionRefreshPolicy.needsRefresh(this.accessToken, this.lastRefresh, System.currentTimeMillis()))) {
            return this.login();
         }
         LoginResult validation = this.validateSession();
         if (autoRefresh && refreshable && validation.getResponseType() == LoginResponseType.INVALID_ACCOUNT) {
            return this.login();
         }
         return validation;
      }
   }

   protected LoginResult validateSession() {
      if (this.accessToken == null || this.accessToken.isEmpty()) {
         return new LoginResult(LoginResponseType.INVALID_ACCOUNT);
      }
      return MinecraftServicesApi.validateSession(this.createSessionObject());
   }

   public final void setMinecraftSession(boolean flag) {
      Minecraft minecraft = Minecraft.getMinecraft();
      if (!minecraft.isCallingFromMinecraftThread()) {
         minecraft.addScheduledTask(() -> this.setMinecraftSession(flag));
         return;
      }
      if (this.repository == null || !this.repository.getEncryption().isDecrypted()) {
         return;
      }
      MinecraftReflection.setMinecraftSession(minecraft, this.createSessionObject());
      if (flag) {
         this.setLastUsed(System.currentTimeMillis());
         this.repository.getAltManager().getStorageManager().setAutoSaveRequired();
         try {
            this.repository.getAltManager().getStorageManager().save();
         } catch (IOException failure) {
            this.repository.getAltManager().getLogger().warn("Could not persist the last-used account", failure);
         }
      }

      this.repository
         .getAltManager()
         .getLogger()
         .info("Logged into account {} ({})", new Object[]{this.username, this.getAccountType().getName().toLowerCase()});
   }

   public final Session createSessionObject() {
      return new Session(this.getUsername(), this.getUuid().toString(), this.getAccessToken(), "mojang");
   }

   protected abstract LoginResult loginImpl();

   protected abstract void deserialize(DataInputStream datainputstream) throws IOException;

   protected abstract void serialize(DataOutputStream dataoutputstream) throws IOException;

   public final void setRepository(AccountRepository AccountRepository) {
      this.repository = AccountRepository;
   }

   public final AccountRepository getRepository() {
      return this.repository;
   }

   public final AccountType getAccountType() {
      return this.accountType;
   }

   public final String getUsername() {
      return this.username;
   }

   public final void setUsername(String s) {
      this.username = s;
      if (this.repository != null) {
         this.repository.invalidateViewingAccountList();
      }
   }

   public final UUID getUuid() {
      return this.uuid;
   }

   public final void setUuid(UUID uuid) {
      if (!uuid.equals(this.uuid)) {
         this.playerHead = null;
      }
      this.uuid = uuid;
   }

   public final void setLastUsed(long i) {
      this.lastUsed = i;
      if (this.repository != null) {
         this.repository.invalidateViewingAccountList();
      }
   }

   public final long getLastUsed() {
      return this.lastUsed;
   }

   public final String getAccessToken() {
      return this.accessToken;
   }

   public final boolean isInDoubleClick() {
      return this.doubleClickActionRunning || (this.repository != null
         && this.repository.getAltManager().getSessionController().isSwitching(this));
   }

   public void setSelected(int i, int j, int k) {
   }

   public void drawEntry(int l1, int i, int j, int k, int l, int i2, int j2, boolean flag) {
      Minecraft minecraft = Minecraft.getMinecraft();
      boolean flag1 = this.repository.getSelectedAccount() == this || this.repository.getAltManager().getMainScreen().getMultiSelectionAccounts().contains(this);
      short short1 = 160;
      if (!this.repository.getAltManager().getMainScreen().getMultiSelectionAccounts().isEmpty() && this.repository.getSelectedAccount() == this) {
         short1 += 5;
      }

      if (flag1) {
         short1 += 60;
      }

      if (flag) {
         short1 += 30;
      }

      SessionController sessions = this.repository.getAltManager().getSessionController();
      boolean active = sessions.isActive(this);
      String s = (flag1 ? EnumChatFormatting.BOLD : "") + this.username;
      if (this.isInDoubleClick()) {
         short1 -= 20;
      }

      GlStateManager.enableBlend();
      byte b0 = 1;
      byte b1 = 45;
      if (flag1) {
         b1 += 28;
      }

      if (flag) {
         b1 += 10;
      }

      Gui.drawRect(i - b0, j - b0, i + k + b0, j + l + b0, new Color(b1, b1, b1 + 5, (int)(short1 / 1.5)).getRGB());
      String s1 = this.accountType.getName();
      int typeWidth = minecraft.fontRendererObj.getStringWidth(s1) / 2;
      int badgeWidth = active ? minecraft.fontRendererObj.getStringWidth("ACTIVE") / 2 + 6 : 0;
      int nameWidth = Math.max(0, k - 35 - typeWidth - badgeWidth - 12);
      minecraft.fontRendererObj.drawStringWithShadow(minecraft.fontRendererObj.trimStringToWidth(s, nameWidth), i + 35, j + 2, ColorUtils.grayscale(short1));
      if (active) {
         int badgeX = i + k - typeWidth - badgeWidth - 6;
         Gui.drawRect(i - 2, j - 1, i, j + l + 1, 0xFF61C982);
         Gui.drawRect(badgeX, j + 1, badgeX + badgeWidth, j + 9, 0xDC183D27);
         this.repository.getAltManager().getMainScreen().drawSmallStringWithShadow("ACTIVE", badgeX + 3, j + 3, 0xFF9DF3B5);
      }
      this.repository
         .getAltManager()
         .getMainScreen()
         .drawSmallStringWithShadow(s1, i + k - minecraft.fontRendererObj.getStringWidth(s1) / 2.0F - 1.0F, j + 2, ColorUtils.grayscaleWithAlpha((int)(short1 / 1.3F), short1));
      long i1 = this.getBanExpiry();
      long j1 = System.currentTimeMillis();
      if (i1 != 0L) {
         String s2 = i1 == -2L || (i1 > 0L && j1 < i1)
            ? EnumChatFormatting.RED + (i1 == -2L ? "permanent" : AltManagerUtils.formatDuration(i1 - j1, false))
            : EnumChatFormatting.GREEN + "unbanned";
         this.repository
            .getAltManager()
            .getMainScreen()
            .drawSmallStringWithShadow(
               s2, i + k - minecraft.fontRendererObj.getStringWidth(s2) / 2.0F - 1.0F, j + l - 5, ColorUtils.grayscaleWithAlpha((int)(short1 / 1.3F), short1)
            );
      }

      SessionController.Status sessionStatus = sessions.getStatus(this);
      String s4 = sessionStatus != null ? sessionStatus.getMessage()
         : this.getLastUsed() == 0L ? "never used" : AltManagerUtils.formatRelativeTime(this.getLastUsed(), " ago");
      if (flag && sessionStatus != null && sessionStatus.isError()) {
         this.repository.getAltManager().getMainScreen().setRowStatusTooltip(s4);
      }
      int secondaryColor = sessionStatus == null ? ColorUtils.grayscaleWithAlpha(short1, short1 - 30)
         : sessionStatus.isError() ? 0xFFFF9991 : sessionStatus.isPending() ? 0xFFFFD482 : 0xFF9DF3B5;
      this.repository.getAltManager().getMainScreen().drawSmallStringWithShadow(
         minecraft.fontRendererObj.trimStringToWidth(s4, Math.max(0, (k - 38) * 2)), i + 35, j + 12, secondaryColor);
      String s3 = this.getCreationTimeText();
      this.repository
         .getAltManager()
         .getMainScreen()
         .drawSmallStringWithShadow(s3, i + 35, j + l - 5, ColorUtils.grayscaleWithAlpha((int)(short1 / 1.5 - 10.0), short1 - 22));
      long k1 = j1 - this.lastRenderTime;
      if (k1 >= 50L) {
         this.lastRenderTime = j1;
         if (k1 >= 500L) {
            this.consecutiveRenders = 0;
         } else {
            this.consecutiveRenders++;
         }
      }

      if (this.consecutiveRenders >= 2) {
         this.getAndInitPlayerHead();
      }

      if (this.playerHead != null) {
         this.playerHead.ensureSkinDownloaded();
         this.playerHead.drawHead(i, j);
      }
   }

   private String getCreationTimeText() {
      long i = this.getCreationTime();
      return AltManagerUtils.formatTimestamp(i) + " (" + AltManagerUtils.formatRelativeTime(i, " ago") + ")";
   }

   public PlayerHeadRenderer getAndInitPlayerHead() {
      if (this.playerHead == null) {
         this.playerHead = new PlayerHeadRenderer(this.getUsername(), this.getUuid(), this.repository.getAltManager());
      }

      return this.playerHead;
   }

   public boolean mousePressed(int i1, int j1, int k1, int l1, int i2, int j2) {
      if (AltManagerScreen.isCtrlKeyDown()) {
         AltManagerScreen AltManagerScreen = this.repository.getAltManager().getMainScreen();
         if (this.repository.getSelectedAccount() != null && !AltManagerScreen.getMultiSelectionAccounts().contains(this.repository.getSelectedAccount())) {
            AltManagerScreen.getMultiSelectionAccounts().add(this.repository.getSelectedAccount());
         }

         if (AltManagerScreen.isShiftKeyDown() && !AltManagerScreen.getMultiSelectionAccounts().isEmpty()) {
            int i = this.repository.getViewingAccountList().indexOf(this);
            int j = this.repository.getViewingAccountList().indexOf(AltManagerScreen.getMultiSelectionAccounts().get(AltManagerScreen.getMultiSelectionAccounts().size() - 1));
            if (i != -1 && j != -1) {
               if (i > j) {
                  int k = j;
                  j = i;
                  i = k;
               }

               AltManagerScreen.getMultiSelectionAccounts().clear();

               for (int l = i; l < j; l++) {
                  AltManagerScreen.addToMultiSelection(this.repository.getViewingAccountList().get(l));
               }
            }
         }

         AltManagerScreen.addToMultiSelection(this);
         AltManagerScreen.getMultiSelectionAccounts().sort(
            Comparator.comparingInt(account -> this.repository.getAccountList().indexOf(account))
         );
      } else {
         this.repository.getAltManager().getMainScreen().getMultiSelectionAccounts().clear();
         this.repository.setSelectedAccount(this);
         if (!this.isInDoubleClick() && System.currentTimeMillis() - this.lastClickTime < 350L) {
            AccountDoubleClickAction AccountDoubleClickAction = this.repository.getAltManager().getStorageManager().getAccountDoubleClickAction();
            if (AccountDoubleClickAction.isAsync()) {
               this.doubleClickActionRunning = true;
               AccountRepository owner = this.repository;
               try {
                  owner.getAltManager().getThreadPool().execute(() -> {
                     try {
                        AccountDoubleClickAction.getAction().accept(this);
                     } catch (Throwable failure) {
                        owner.getAltManager().getLogger().warn("Account action failed ({})", failure.getClass().getSimpleName());
                        Minecraft.getMinecraft().addScheduledTask(() -> owner.getAltManager().getMainScreen().setStatus("Account action failed. Please try again."));
                     } finally {
                        this.doubleClickActionRunning = false;
                     }
                  });
               } catch (RuntimeException failure) {
                  this.doubleClickActionRunning = false;
                  owner.getAltManager().getMainScreen().setStatus("Could not start account action. Please try again.");
               }
            } else {
               AccountDoubleClickAction.getAction().accept(this);
            }
         }

         this.lastClickTime = System.currentTimeMillis();
      }

      return true;
   }

   public void mouseReleased(int i, int j, int k, int l, int i1, int j1) {
   }

   @Override
   public String toString() {
      return "AbstractAccount{accountType="
         + this.accountType
         + ", username='"
         + this.username
         + '\''
         + ", uuid="
         + this.uuid
         + ", repository="
         + (this.repository == null ? null : this.repository.getName())
         + '}';
   }

   public final void decryptIfWaitingPassword() throws IOException {
      byte[] abyte = this.pendingEncryptedData;
      if (abyte != null) {
         boolean containsAccessToken = this.pendingDataContainsAccessToken;
         this.pendingEncryptedData = null;
         this.pendingDataContainsAccessToken = false;

         try {
            this.deserializeDataFromBytes(abyte, containsAccessToken);
         } catch (IOException | RuntimeException exception) {
            this.pendingEncryptedData = abyte;
            this.pendingDataContainsAccessToken = containsAccessToken;
            throw exception;
         }
      }
   }

   public final void deserializeDataFromBytes(byte[] abyte) throws IOException {
      this.deserializeDataFromBytes(abyte, false);
   }

   public final void deserializeDataFromBytes(byte[] abyte, boolean containsAccessToken) throws IOException {
      if (this.pendingEncryptedData != null) {
         throw new RuntimeException("Attempted to deserialize while waiting for decryption");
      }

      if (!this.repository.getEncryption().isDecrypted()) {
         this.pendingEncryptedData = abyte;
         this.pendingDataContainsAccessToken = containsAccessToken;
      } else {
         DataInputStream input = new DataInputStream(new ByteArrayInputStream(this.repository.getEncryption().decryptRaw(abyte)));
         if (containsAccessToken) {
            this.accessToken = input.readUTF();
         }

         this.deserialize(input);
      }
   }

   public final byte[] serializeDataToBytes() throws IOException {
      return this.serializeDataToBytes(false);
   }

   public final synchronized byte[] serializeDataToBytes(boolean includeAccessToken) throws IOException {
      if (!this.repository.getEncryption().isDecrypted()) {
         byte[] abyte1 = this.pendingEncryptedData;
         if (abyte1 == null) {
            throw new RuntimeException("Tried to serialize encrypted account but never received encrypted data");
         } else if (this.pendingDataContainsAccessToken != includeAccessToken) {
            throw new IOException("Unlock this legacy encrypted repository before saving it in the current format");
         } else {
            return abyte1;
         }
      } else {
         ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
         DataOutputStream output = new DataOutputStream(bytearrayoutputstream);
         if (includeAccessToken) {
            output.writeUTF(this.accessToken);
         }

         this.serialize(output);
         byte[] abyte = bytearrayoutputstream.toByteArray();
         return this.repository.getEncryption().encryptRaw(abyte);
      }
   }

   public void resetSkin() {
      if (this.playerHead != null) {
         this.playerHead.resetSkin();
      }
   }

   public long getBanExpiry() {
      return this.repository.getAltManager().getStorageManager().getBanExpiry(this.getUuid());
   }

   public void setBanExpiry(long i) {
      this.repository.getAltManager().getStorageManager().setBanExpiry(this.getUuid(), i);
   }

   public void startBanCheck() {
      AccountRepository owner = this.repository;
      if (owner == null || !owner.getEncryption().isDecrypted()) {
         return;
      }
      AltManager altmanager = owner.getAltManager();
      LoginResult prepared = this.prepareSession(altmanager.getStorageManager().isAutoRefreshSession(), false);
      if (!prepared.isSuccessfulLogin()) {
         Minecraft.getMinecraft().addScheduledTask(() -> altmanager.getMainScreen().setStatus(prepared.getResponseType().getDescription()));
         return;
      }
      String s = "mc.hypixel.net.";

      InetAddress inetaddress;
      try {
         inetaddress = InetAddress.getByName("mc.hypixel.net.");
      } catch (UnknownHostException unknownhostexception) {
         altmanager.getLogger().warn("Failed to resolve Hypixel address");
         return;
      }

      CountDownLatch countdownlatch = new CountDownLatch(1);
      BanCheckNetworkManager networkManager = BanCheckNetworkManager.connect(altmanager, this.createSessionObject(), inetaddress, 25565, reason -> {
         countdownlatch.countDown();
      });
      networkManager.setNetHandler(new NetHandlerLoginClient(networkManager, Minecraft.getMinecraft(), null));
      networkManager.sendPacket(new C00Handshake(47, "mc.hypixel.net.", 25565, EnumConnectionState.LOGIN));
      networkManager.sendPacket(new C00PacketLoginStart(this.createSessionObject().getProfile()));

      try {
         countdownlatch.await(5L, TimeUnit.SECONDS);
      } catch (InterruptedException interruptedexception) {
         Thread.currentThread().interrupt();
      } finally {
         networkManager.closeChannel(new net.minecraft.util.ChatComponentText("Ban check finished"));
      }
   }
}
