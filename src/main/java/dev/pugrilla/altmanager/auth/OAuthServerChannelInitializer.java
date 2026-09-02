package dev.pugrilla.altmanager.auth;


import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
public final class OAuthServerChannelInitializer extends ChannelInitializer<SocketChannel> {
   protected void initChannel(SocketChannel socketchannel) {
      socketchannel.pipeline().addLast(new ChannelHandler[]{new HttpRequestDecoder()});
      socketchannel.pipeline().addLast(new ChannelHandler[]{new HttpObjectAggregator(65536)});
      socketchannel.pipeline().addLast(new ChannelHandler[]{new HttpContentDecompressor()});
      socketchannel.pipeline().addLast(new ChannelHandler[]{new HttpResponseEncoder()});
      socketchannel.pipeline().addLast(new ChannelHandler[]{new OAuthCallbackHandler()});
   }
}
