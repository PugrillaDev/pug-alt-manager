package dev.pugrilla.altmanager.storage;

import dev.pugrilla.altmanager.account.AbstractAccount;
import dev.pugrilla.altmanager.account.AccountRepository;
import dev.pugrilla.altmanager.AltManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;
public final class RepositoryFileCodec {
   private static final int REPOSITORY_FILE_MAGIC = 256243359;
   private static final int REPOSITORY_FILE_VERSION = 1;
   public static void importRepository(AltManager altmanager, byte[] abyte) throws IOException {
      DataInputStream datainputstream = new DataInputStream(new ByteArrayInputStream(abyte));
      int i = datainputstream.readInt();
      if (i != 256243359) {
         throw new IOException("Mismatching repository header: " + Integer.toHexString(i));
      }

      int j = datainputstream.readInt();
      if (j >= 0 && j <= REPOSITORY_FILE_VERSION) {
         AccountRepository AccountRepository = FileStorageManager.readRepository(altmanager, datainputstream, j);
         int k = datainputstream.readInt();
         if (k < 0 || k > datainputstream.available() / 24) {
            throw new IOException("Invalid ban status count");
         }
         HashMap<UUID, Long> importedBans = new HashMap<>();

         for (int l = 0; l < k; l++) {
            UUID uuid = new UUID(datainputstream.readLong(), datainputstream.readLong());
            long i1 = datainputstream.readLong();
            importedBans.put(uuid, i1);
         }
         net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
            altmanager.getStorageManager().createRepository(AccountRepository);
            importedBans.forEach((uuid, expiry) -> {
               if (altmanager.getStorageManager().getBanExpiry(uuid) == 0L && expiry != 0L) {
                  altmanager.getStorageManager().setBanExpiry(uuid, expiry);
               }
            });
         });
      } else {
         throw new IOException("Incompatible repository file version: " + j + ", current is " + REPOSITORY_FILE_VERSION);
      }
   }
   public static byte[] exportRepository(AccountRepository AccountRepository) throws IOException {
      ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
      DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
      dataoutputstream.writeInt(256243359);
      dataoutputstream.writeInt(REPOSITORY_FILE_VERSION);
      FileStorageManager.writeRepository(AccountRepository, dataoutputstream, REPOSITORY_FILE_VERSION);
      HashMap<UUID, Long> hashmap = new HashMap<>();

      for (AbstractAccount AbstractAccount : AccountRepository.getAccountList()) {
         long i = AbstractAccount.getBanExpiry();
         if (i != 0L) {
            hashmap.put(AbstractAccount.getUuid(), i);
         }
      }

      dataoutputstream.writeInt(hashmap.size());

      for (Entry<UUID, Long> entry : hashmap.entrySet()) {
         dataoutputstream.writeLong(entry.getKey().getMostSignificantBits());
         dataoutputstream.writeLong(entry.getKey().getLeastSignificantBits());
         dataoutputstream.writeLong(entry.getValue());
      }

      return bytearrayoutputstream.toByteArray();
   }
}
