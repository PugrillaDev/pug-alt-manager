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
   private static final int REPOSITORY_FILE_VERSION = 0;
   public static void importRepository(AltManager altmanager, byte[] abyte) throws IOException {
      DataInputStream datainputstream = new DataInputStream(new ByteArrayInputStream(abyte));
      int i = datainputstream.readInt();
      if (i != 256243359) {
         throw new IOException("Mismatching repository header: " + Integer.toHexString(i));
      }

      int j = datainputstream.readInt();
      if (j >= 0 && j <= 0) {
         AccountRepository AccountRepository = FileStorageManager.readRepository(altmanager, datainputstream);
         altmanager.getStorageManager().createRepository(AccountRepository);
         int k = datainputstream.readInt();

         for (int l = 0; l < k; l++) {
            UUID uuid = new UUID(datainputstream.readLong(), datainputstream.readLong());
            long i1 = datainputstream.readLong();
            long j1 = altmanager.getStorageManager().getBanExpiry(uuid);
            if (j1 == 0L && i1 != 0L) {
               altmanager.getStorageManager().setBanExpiry(uuid, i1);
            }
         }
      } else {
         throw new IOException("Incompatible repository file version: " + j + ", current is " + 0);
      }
   }
   public static byte[] exportRepository(AccountRepository AccountRepository) throws IOException {
      ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
      DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
      dataoutputstream.writeInt(256243359);
      dataoutputstream.writeInt(0);
      FileStorageManager.writeRepository(AccountRepository, dataoutputstream);
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
