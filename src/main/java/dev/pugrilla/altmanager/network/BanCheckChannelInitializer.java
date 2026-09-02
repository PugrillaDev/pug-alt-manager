package dev.pugrilla.altmanager.network;


import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.util.MessageDeserializer;
import net.minecraft.util.MessageDeserializer2;
import net.minecraft.util.MessageSerializer;
import net.minecraft.util.MessageSerializer2;
final class BanCheckChannelInitializer extends ChannelInitializer<Channel> {
   private final BanCheckNetworkManager networkManager;

   BanCheckChannelInitializer(BanCheckNetworkManager BanCheckNetworkManager) {
      this.networkManager = BanCheckNetworkManager;
   }

   protected void initChannel(Channel channel) throws Exception {
      try {
         channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
      } catch (ChannelException channelexception) {
      }

      ChannelPipeline channelpipeline = channel.pipeline().addLast("timeout", new ReadTimeoutHandler(4)).addLast("splitter", new MessageDeserializer2());
      channelpipeline.addLast("decoder", new MessageDeserializer(EnumPacketDirection.CLIENTBOUND));
      channelpipeline.addLast("prepender", new MessageSerializer2());
      channelpipeline.addLast("encoder", new MessageSerializer(EnumPacketDirection.SERVERBOUND));
      channelpipeline.addLast("packet_handler", this.networkManager);
   }
}
