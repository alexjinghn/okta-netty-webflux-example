package com.test.netty.server;

import com.test.netty.common.FutureListener;
import com.test.netty.common.RestoreHealer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public final class AppServer {
	private static Logger logger = LoggerFactory.getLogger(AppServer.class);


	// Port where chat server will listen for connections.
	static final int PORT = 8007;

	public void run() throws Exception {

		/*
		 * Configure the server.
		 */
		Server server = new Server("localhost", PORT);
		try {
			server.start();
			server.awaitTerminate();
		} finally {
			server.shutdown();
		}
	}

	public static void main(String[] args) throws Exception {
		new AppServer().run();
	}

	private class Server {
		String address;
		int port;

		EventLoopGroup bossGroup;
		EventLoopGroup workerGroup;

		ChannelFuture f;
		ServerBootstrap b;

		private CountDownLatch terminated = new CountDownLatch(1);

		Server(String _address, int _port) {
			address = _address;
			port = _port;

			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();

			b = new ServerBootstrap();
			b.group(bossGroup, workerGroup) // Set boss & worker groups
					.channel(NioServerSocketChannel.class)// Use NIO to accept new connections.
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline p = ch.pipeline();
							/*
							 * Socket/channel communication happens in byte streams. String decoder &
							 * encoder helps conversion between bytes & String.
							 */
							p.addLast(new StringDecoder());
							p.addLast(new StringEncoder());

							// This is our custom server handler which will have logic for chat.
							p.addLast(new ServerHandler());
						}
					});

			RestoreHealer.registerCallback(() -> {
				logger.info("start resetting server");

				try {
					bossGroup.shutdownGracefully();
					workerGroup.shutdownGracefully();

					bossGroup = new NioEventLoopGroup(1);
					workerGroup = new NioEventLoopGroup();
					b = new ServerBootstrap();
					b.group(bossGroup, workerGroup) // Set boss & worker groups
						.channel(NioServerSocketChannel.class)// Use NIO to accept new connections.
						.childHandler(new ChannelInitializer<SocketChannel>() {
							@Override
							public void initChannel(SocketChannel ch) throws Exception {
								ChannelPipeline p = ch.pipeline();
								/*
								 * Socket/channel communication happens in byte streams. String decoder &
								 * encoder helps conversion between bytes & String.
								 */
								p.addLast(new StringDecoder());
								p.addLast(new StringEncoder());

								// This is our custom server handler which will have logic for chat.
								p.addLast(new ServerHandler());
							}
						});
					logger.info("re-binding to address " + address + ":" + PORT);
					f = b.bind(address, port);
					f.addListener(new FutureListener());
					f = f.sync();
					logger.info("successfully bound to address " + address + ":" + PORT);
				} catch (Exception e) {
					logger.warn("Failed to restart server", e);
				}
				logger.info("Chat Server started. Ready to accept chat clients.");

			}, 0);
		}

		public void start() throws InterruptedException {
			// Start the server.
			f = b.bind(PORT);
			f.addListener(new FutureListener());
			f = f.sync();
			logger.info("Chat Server started. Ready to accept chat clients.");
		}

		public void awaitTerminate() throws InterruptedException {
						// Wait until the server socket is closed.
			f.channel().closeFuture().sync();
			logger.info("channel is closed " + f);
			terminated.await();
		}

		public void shutdown() throws InterruptedException {
			f.channel().closeFuture().sync();
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

}