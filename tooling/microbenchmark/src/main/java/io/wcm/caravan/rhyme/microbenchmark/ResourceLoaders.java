package io.wcm.caravan.rhyme.microbenchmark;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class ResourceLoaders {

	private static NettyHttpServer nettyServer;

	static void init() {
		nettyServer = new NettyHttpServer();
		nettyServer.start();
	}

	static void tearDown() {
		if (nettyServer != null) {
			nettyServer.shutdown();
		}
	}

	final static class PrebuiltResponseLoader implements HalResourceLoader {

		private final HalResponse response;

		PrebuiltResponseLoader() {
			response = RhymeBuilder.create().buildForRequestTo("/foo").renderResponse(new DynamicResourceImpl())
					.blockingGet();
		}

		@Override
		public Single<HalResponse> getHalResource(String uri) {
			return Single.just(response);
		}
	}

	static HalResourceLoader preBuilt() {
		return new PrebuiltResponseLoader();
	}

	static HalResourceLoader parsing() {

		String json = getJsonResponse();

		byte[] bytes = json.getBytes(Charsets.UTF_8);

		return HalResourceLoader.create(new HttpClientSupport() {

			@Override
			public void executeGetRequest(URI uri, HttpClientCallback callback) {
				callback.onHeadersAvailable(200, ImmutableMap.of());
				callback.onBodyAvailable(new ByteArrayInputStream(bytes));
			}
		});
	}

	static HalResourceLoader network() {

		HalResourceLoader loader = HalResourceLoader.create();

		return new HalResourceLoader() {

			@Override
			public Single<HalResponse> getHalResource(String path) {

				String uri = "http://localhost:" + nettyServer.getPort();
				return loader.getHalResource(uri);
			}
		};
	}

	private static String getJsonResponse() {
		return preBuilt().getHalResource("/foo").blockingGet().getBody().getModel().toString();
	}

	static class HttpServer extends Thread {

		private final ServerSocket ss;

		private final String body = getJsonResponse();

		HttpServer() throws IOException {
			ss = new ServerSocket(0);
		}

		int getPort() {
			return ss.getLocalPort();
		}

		@Override
		public void run() {
			// Now enter an infinite loop, waiting for & handling connections.
			for (;;) {
				try {
					handleNextRequest();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

		private void handleNextRequest() throws IOException {
			// Wait for a client to connect. The method will block;
			// when it returns the socket will be connected to the client
			Socket client = ss.accept();

			// Get input and output streams to talk to the client
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintWriter out = new PrintWriter(client.getOutputStream());

			// Start sending our reply, using the HTTP 1.1 protocol
			out.print("HTTP/1.1 200 \r\n"); // Version & status code
			out.print("Content-Type: " + HalResource.CONTENT_TYPE + "\r\n"); // The type of data
			out.print("Connection: close\r\n"); // Will close stream
			out.print("\r\n"); // End of headers

			// Now, read the HTTP request from the client, and send it
			// right back to the client as part of the body of our
			// response. The client doesn't disconnect, so we never get
			// an EOF. It does sends an empty line at the end of the
			// headers, though. So when we see the empty line, we stop
			// reading. This means we don't mirror the contents of POST
			// requests, for example. Note that the readLine() method
			// works with Unix, Windows, and Mac line terminators.
			String line;
			while ((line = in.readLine()) != null) {
				if (line.length() == 0)
					break;
			}

			out.print(body + "\r\n");

			// Close socket, breaking the connection to the client, and
			// closing the input and output streams
			out.close();
			in.close();
			client.close();
		}
	}

	static class NettyHttpServer {
		private static final int NETTY_PORT_NR = 12345;

		private final String body = getJsonResponse();

		private ChannelFuture channel;
		private final EventLoopGroup masterGroup;
		private final EventLoopGroup slaveGroup;

		public NettyHttpServer() {
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
											final FullHttpRequest request = (FullHttpRequest) msg;

											final String responseMessage = body;

											FullHttpResponse response = new DefaultFullHttpResponse(
													HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
													Unpooled.copiedBuffer(responseMessage.getBytes()));

											if (HttpHeaders.isKeepAlive(request)) {
												response.headers().set(HttpHeaders.Names.CONNECTION,
														HttpHeaders.Values.KEEP_ALIVE);
											}
											response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
													HalResource.CONTENT_TYPE);
											response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
													responseMessage.length());

											ctx.writeAndFlush(response);
										} else {
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
			} catch (final InterruptedException e) {
			}
		}

		public void shutdown() // #2
		{
			slaveGroup.shutdownGracefully();
			masterGroup.shutdownGracefully();

			try {
				channel.channel().closeFuture().sync();
			} catch (InterruptedException e) {
			}
		}

	}
}
