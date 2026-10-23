package dev.pugrilla.altmanager.storage;

import dev.pugrilla.altmanager.gui.AccountDoubleClickAction;
import java.nio.file.Files;
import java.io.File;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class StorageSettingsTest {
   @Rule public TemporaryFolder temporary = new TemporaryFolder();

   @Test public void refreshAndDisplayPreferencesSurviveRestart() throws Exception {
      File directory = temporary.newFolder();
      FileStorageManager original = new FileStorageManager(null, directory, false);
      original.setAutoRefreshSession(false);
      original.setMultiplayerButtonVisible(false);
      original.setShowLoggedInUser(false);
      original.setAccountDoubleClickAction(AccountDoubleClickAction.REFRESH_SESSION);
      original.setSearchTerm("TestUser");
      original.save();
      FileStorageManager restored = new FileStorageManager(null, directory, false);
      assertFalse(restored.isAutoRefreshSession());
      assertFalse(restored.isMultiplayerButtonVisible());
      assertFalse(restored.isShowLoggedInUser());
      assertEquals(AccountDoubleClickAction.REFRESH_SESSION, restored.getAccountDoubleClickAction());
      assertEquals("TestUser", restored.getSearchTerm());
   }

   @Test public void olderStorageWithoutRefreshPreferenceDefaultsToEnabled() throws Exception {
      File directory = temporary.newFolder();
      FileStorageManager original = new FileStorageManager(null, directory, false);
      original.setAutoRefreshSession(false);
      original.save();
      File database = new File(directory, "accounts.dat");
      byte[] current = Files.readAllBytes(database.toPath());
      Files.write(database.toPath(), Arrays.copyOf(current, current.length - 1));
      assertTrue(new FileStorageManager(null, directory, false).isAutoRefreshSession());
   }

   @Test public void damagedPrimaryRecoversBackupAndPreservesDamagedFile() throws Exception {
      File directory = temporary.newFolder();
      FileStorageManager original = new FileStorageManager(null, directory, false);
      original.setAutoRefreshSession(false);
      original.save();
      Files.write(new File(directory, "accounts.dat").toPath(), new byte[]{1, 2, 3});
      FileStorageManager recovered = new FileStorageManager(null, directory, false);
      assertFalse(recovered.isAutoRefreshSession());
      assertEquals(1, directory.listFiles((parent, name) -> name.startsWith("accounts.dat.corrupt-")).length);
   }

   @Test public void corruptDatabaseWithoutBackupFailsWithoutDiscardingData() throws Exception {
      File directory = temporary.newFolder();
      File database = new File(directory, "accounts.dat");
      Files.write(database.toPath(), new byte[]{1, 2, 3});
      assertThrows(RuntimeException.class, () -> new FileStorageManager(null, directory, false));
      assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(database.toPath()));
   }
}
