package com.test.netty.server;

import com.test.netty.common.FutureListener;
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

public final class AppServer {
	private static Logger logger = LoggerFactory.getLogger(AppServer.class);


	// Port where chat server will listen for connections.
	static final int PORT = 8007;

	public void run() throws Exception {

		/*
		 * Configure the server.
		 */

		// Create boss & worker groups. Boss accepts connections from client. Worker
		// handles further communication through connections.
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap b = new ServerBootstrap();
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

			// Start the server.
			ChannelFuture f = b.bind(PORT);
			f.addListener(new FutureListener());
			f = f.sync();
			logger.info("Chat Server started. Ready to accept chat clients.");

			// Wait until the server socket is closed.
			f.channel().closeFuture().sync();
		} finally {
			// Shut down all event loops to terminate all threads.
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		new AppServer().run();
	}
}