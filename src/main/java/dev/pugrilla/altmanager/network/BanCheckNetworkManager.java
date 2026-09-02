package dev.pugrilla.altmanager.network;

import dev.pugrilla.altmanager.AltManager;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.crypto.SecretKey;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.CryptManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.LazyLoadBase;
import net.minecraft.util.Session;
public final class BanCheckNetworkManager extends NetworkManager {
   private final AltManager altManager;
   private final Session session;
   private final Consumer<IChatComponent> disconnectCallback;
   private final AtomicBoolean disconnected = new AtomicBoolean(false);

   private BanCheckNetworkManager(AltManager altmanager, Session session, Consumer<IChatComponent> consumer) {
      super(EnumPacketDirection.CLIENTBOUND);
      this.altManager = altmanager;
      this.session = session;
      this.disconnectCallback = consumer;
   }

   protected void channelRead0(ChannelHandlerContext channelhandlercontext, Packet packet) throws Exception {
      if (this.isChannelOpen()) {
         if (packet instanceof S00PacketDisconnect) {
            System.out.println(((S00PacketDisconnect)packet).func_149603_c().getFormattedText());
            this.altManager
               .getEventListener()
               .getServerBanHandler()
               .handleDisconnectMessage(this.session.getProfile().getId(), ((S00PacketDisconnect)packet).func_149603_c());
            this.closeChannel(((S00PacketDisconnect)packet).func_149603_c());
            return;
         }

         if (!(packet instanceof S01PacketEncryptionRequest)) {
            this.altManager.getStorageManager().setBanExpiry(this.session.getProfile().getId(), -1L);
            System.out.println("LOLzxc$!");
            this.closeChannel(new ChatComponentText("Finished"));
            return;
         }

         System.out.println("LOL#$!");
         S01PacketEncryptionRequest s01packetencryptionrequest = (S01PacketEncryptionRequest)packet;
         this.handleEncryptionRequest(s01packetencryptionrequest);
      }
   }

   private void handleEncryptionRequest(S01PacketEncryptionRequest s01packetencryptionrequest) {
      SecretKey secretkey = CryptManager.createNewSharedKey();
      String s = s01packetencryptionrequest.getServerId();
      PublicKey publickey = s01packetencryptionrequest.getPublicKey();
      String s1 = new BigInteger(CryptManager.getServerIdHash(s, publickey, secretkey)).toString(16);
      Minecraft minecraft = Minecraft.getMinecraft();

      try {
         minecraft.getSessionService().joinServer(minecraft.getSession().getProfile(), minecraft.getSession().getToken(), s1);
      } catch (AuthenticationUnavailableException authenticationunavailableexception) {
         this.closeChannel(
            new ChatComponentTranslation(
               "disconnect.loginFailedInfo", new Object[]{new ChatComponentTranslation("disconnect.loginFailedInfo.serversUnavailable", new Object[0])}
            )
         );
         return;
      } catch (InvalidCredentialsException invalidcredentialsexception) {
         this.closeChannel(
            new ChatComponentTranslation(
               "disconnect.loginFailedInfo", new Object[]{new ChatComponentTranslation("disconnect.loginFailedInfo.invalidSession", new Object[0])}
            )
         );
         return;
      } catch (AuthenticationException authenticationexception) {
         this.closeChannel(new ChatComponentTranslation("disconnect.loginFailedInfo", new Object[]{authenticationexception.getMessage()}));
         return;
      }

      this.sendPacket(
         new C01PacketEncryptionResponse(secretkey, publickey, s01packetencryptionrequest.getVerifyToken()),
         new EncryptionEnableListener(this, secretkey),
         new GenericFutureListener[0]
      );
   }

   public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) throws Exception {
      this.closeChannel(new ChatComponentText("Exception Caught"));
   }

   public void closeChannel(IChatComponent ichatcomponent) {
      if (this.disconnected.compareAndSet(false, true) && this.disconnectCallback != null) {
         this.disconnectCallback.accept(ichatcomponent);
      }

      super.closeChannel(ichatcomponent);
   }
   public static BanCheckNetworkManager connect(AltManager altmanager, Session session, InetAddress inetaddress, int i, Consumer<IChatComponent> consumer) {
      BanCheckNetworkManager BanCheckNetworkManager = new BanCheckNetworkManager(altmanager, session, consumer);
      Class<? extends Channel> oclass;
      LazyLoadBase lazyloadbase;
      if (Epoll.isAvailable()) {
         oclass = EpollSocketChannel.class;
         lazyloadbase = CLIENT_EPOLL_EVENTLOOP;
      } else {
         oclass = NioSocketChannel.class;
         lazyloadbase = CLIENT_NIO_EVENTLOOP;
      }

      ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group((EventLoopGroup)lazyloadbase.getValue())).handler(new BanCheckChannelInitializer(BanCheckNetworkManager))).channel(oclass))
         .connect(inetaddress, i)
         .syncUninterruptibly();
      return BanCheckNetworkManager;
   }
}
