package com.test.netty.client;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.test.netty.common.FutureListener;
import com.test.netty.common.RestoreHealer;
import com.test.netty.server.AppServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

	private static Logger logger = LoggerFactory.getLogger(Client.class);

	static final String HOST = "127.0.0.1";
	static final int PORT = 8007;

	private AtomicInteger index = new AtomicInteger(0);
	private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	public void run() throws Exception {
		SimpleClient client = new SimpleClient(HOST, PORT);
		try {
			client.start();

			executorService.scheduleAtFixedRate(() -> {
				try {
					int idx = index.getAndIncrement();
					logger.info("index is " + idx);
					client.send("" + idx);
				} catch (Exception e) {

				}
			}, 0, 3000, TimeUnit.MILLISECONDS);
		} finally {
			client.shutdown();
		}

	}

	public static void main(String[] args) throws Exception {
		new Client().run();
	}

	private class SimpleClient {
		Bootstrap bootstrap;
		EventLoopGroup group;
		ChannelFuture f;

		String address;
		int port;

		SimpleClient(String _address, int _port) {
			address = _address;
			port = _port;
			// Since this is client, it doesn't need boss group. Create single group.
		 	group = new NioEventLoopGroup();
			bootstrap = new Bootstrap();
			bootstrap.group(group) // Set EventLoopGroup to handle all eventsf for client.
					.channel(NioSocketChannel.class)// Use NIO to accept new connections.
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline p = ch.pipeline();
							/*
							 * Socket/channel communication happens in byte streams. String decoder &
							 * encoder helps conversion between bytes & String.
							 */
							p.addLast(new StringDecoder());
							p.addLast(new StringEncoder());

							// This is our custom client handler which will have logic for chat.
							p.addLast(new ClientHandler());

						}
					});

			// Start the client.
			RestoreHealer.registerCallback(() -> {
				synchronized (f) {
					try {
						bootstrap.config().group().shutdownGracefully();
						group = new NioEventLoopGroup();
						bootstrap.group(group);
						f = bootstrap.connect(address, port);
						f.addListener(new FutureListener());
						f = f.sync();
						logger.info("restarted channel " + f);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}

			}, 1);
		}

		private void start() throws InterruptedException {
			f = bootstrap.connect(address, port);
			f.addListener(new FutureListener());
			f = f.sync();
			logger.info("obtained channel future " + f);
		}

		private void send(String msg) throws InterruptedException {
			logger.info("channel future " + f);
			Channel channel = f.sync().channel();
			logger.info("get hold of channel " + channel + " class " + channel.getClass());
			channel.writeAndFlush("[client]: " + msg);
			channel.flush();
		}

		private void shutdown() throws InterruptedException {
			// Wait until the connection is closed.
			f.channel().closeFuture().sync();
			group.shutdownGracefully();
		}
	}
}
