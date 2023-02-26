package com.test.netty.client;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

public class Client {

	static final String HOST = "127.0.0.1";
	static final int PORT = 8007;

	private AtomicInteger index = new AtomicInteger(0);
	private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	public void run() throws Exception {
		SimpleClient client = new SimpleClient();
		try {
			client.start(HOST, PORT);

			executorService.scheduleAtFixedRate(() -> {
				try {
					int idx = index.getAndIncrement();
					System.out.println("index is " + idx);
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

		SimpleClient() {
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
		}

		private void start(String address, int port) throws InterruptedException {
			f = bootstrap.connect(address, port).sync();
		}

		private void send(String msg) throws InterruptedException {
			Channel channel = f.sync().channel();
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
