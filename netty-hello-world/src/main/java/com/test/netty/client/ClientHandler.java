package com.test.netty.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler extends SimpleChannelInboundHandler<String> {
	private static Logger logger = LoggerFactory.getLogger(ClientHandler.class);

	/*
	 * Print chat message received from server.
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		logger.info("Message: " + msg);
	}

}
