/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.microbenchmark;

import java.util.Map;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.wcm.caravan.hal.resource.HalResource;

class NettyHttpServer {

  private static final int NETTY_PORT_NR = 12345;

  private final Map<String, byte[]> responseBodyMap;

  private ChannelFuture channel;
  private final EventLoopGroup masterGroup;
  private final EventLoopGroup slaveGroup;

  public NettyHttpServer(Map<String, byte[]> responseBodyMap) {
    this.responseBodyMap = responseBodyMap;
    masterGroup = new NioEventLoopGroup();
    slaveGroup = new NioEventLoopGroup();
  }

  public int getPort() {
    return NETTY_PORT_NR;
  }

  public void start() // #1
  {
    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        shutdown();
      }
    });

    try {
      // #3
      final ServerBootstrap bootstrap = new ServerBootstrap().group(masterGroup, slaveGroup)
          .channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() // #4
          {

            @Override
            public void initChannel(final SocketChannel ch) throws Exception {
              ch.pipeline().addLast("codec", new HttpServerCodec());
              ch.pipeline().addLast("aggregator", new HttpObjectAggregator(512 * 1024));
              ch.pipeline().addLast("request", new ChannelInboundHandlerAdapter() // #5
              {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                  if (msg instanceof FullHttpRequest) {
                    final FullHttpRequest request = (FullHttpRequest)msg;

                    final byte[] body = responseBodyMap.get(request.uri());

                    FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(body));

                    if (HttpHeaders.isKeepAlive(request)) {
                      response.headers().set(HttpHeaders.Names.CONNECTION,
                          HttpHeaders.Values.KEEP_ALIVE);
                    }
                    response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
                        HalResource.CONTENT_TYPE);
                    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
                        body.length);
                    response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=3600");

                    ctx.writeAndFlush(response);
                  }
                  else {
                    super.channelRead(ctx, msg);
                  }
                }

                @Override
                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                  ctx.flush();
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                    throws Exception {
                  ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                      HttpResponseStatus.INTERNAL_SERVER_ERROR,
                      Unpooled.copiedBuffer(cause.getMessage().getBytes())));
                }
              });
            }
          }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
      channel = bootstrap.bind(NETTY_PORT_NR).sync();
    }
    catch (final InterruptedException e) {
    }
  }

  public void shutdown() // #2
  {
    slaveGroup.shutdownGracefully();
    masterGroup.shutdownGracefully();

    try {
      channel.channel().closeFuture().sync();
    }
    catch (InterruptedException e) {
    }
  }

}