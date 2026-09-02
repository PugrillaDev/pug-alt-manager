package dev.pugrilla.altmanager.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
public final class IOUtils {
   public static byte[] readAllBytes(InputStream inputstream) throws IOException {
      ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
      byte[] abyte = new byte[1024];

      int i;
      while ((i = inputstream.read(abyte)) != -1) {
         bytearrayoutputstream.write(abyte, 0, i);
      }

      return bytearrayoutputstream.toByteArray();
   }
}
