package com.test.netty.server;

import java.util.ArrayList;
import java.util.List;

import com.test.netty.client.ClientHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles a server-side channel.
 */
public class ServerHandler extends SimpleChannelInboundHandler<String> {

	private static Logger logger = LoggerFactory.getLogger(ServerHandler.class);

	// List of connected client channels.
	static final List<Channel> channels = new ArrayList<Channel>();

	/*
	 * Whenever client connects to server through channel, add his channel to the
	 * list of channels.
	 */
	@Override
	public void channelActive(final ChannelHandlerContext ctx) {
		logger.info("Client joined - " + ctx);
		channels.add(ctx.channel());
	}

	/*
	 * When a message is received from client, send that message to all channels.
	 * FOr the sake of simplicity, currently we will send received chat message to
	 * all clients instead of one specific client. This code has scope to improve to
	 * send message to specific client as per senders choice.
	 */
	@Override
	public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		logger.info("Server received - " + msg);
		for (Channel c : channels) {
			c.writeAndFlush("-> " + msg + '\n');
		}
	}

	/*
	 * In case of exception, close channel. One may chose to custom handle exception
	 * & have alternative logical flows.
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.info("Closing connection for client - " + ctx);
		ctx.close();
	}
}
