package com.fmgame.bolt.remoting.netty;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.remoting.Channel;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ServiceException;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.rpc.RpcContext;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.NetUtils;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * netty 处理
 * 
 * @author luowei
 * @date 2017年10月16日 下午5:09:47
 */
@Sharable
public class NettyHandler extends ChannelInboundHandlerAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger(NettyHandler.class);
	
	/** <ip:port, channel> */
    private final Map<String, Channel> channels = new ConcurrentHashMap<>(); 
	/** 服务 */
    private final URL url;
    /** 消息处理 */
    private final MessageHandler handler;
	/** 线程执行器 */
    private final Executor executor;

	public NettyHandler(URL url, MessageHandler handler, Executor executor) {
		this.url = url;
		this.handler = handler;
		this.executor = executor;
	}

	public Map<String, Channel> getChannels() {
		return channels;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelActive();
		
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
    		logger.info("NettyHandler channelActive: remote=" + ctx.channel().remoteAddress()
    				+ " local=" + ctx.channel().localAddress());
            if (channel != null) {
                channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), channel);
            }
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelInactive();
		
        try {
        	logger.info("NettyHandler channelInactive: remote=" + ctx.channel().remoteAddress()
    				+ " local=" + ctx.channel().localAddress());
            channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        try {
    		logger.error("NettyHandler exceptionCaught: remote=" + ctx.channel().remoteAddress()
    				+ " local=" + ctx.channel().localAddress() + " cause=" + cause.getMessage());
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		executor.execute(() -> {
			if (msg instanceof Request) {
				processRequest(ctx, msg);
			} else if (msg instanceof Response) {
				processResponse(ctx, msg);
			} else {
				logger.error("NettyHandler messageReceived type not support: class=" + msg.getClass());
				throw new ServiceException("NettyHandler messageReceived type not support: class="
						+ msg.getClass());
			}
		});
	}
	
	/**
	 * <pre>
	 *  request process: 主要来自于client的请求，需要使用threadPoolExecutor进行处理，避免service message处理比较慢导致iothread被阻塞
	 * </pre>
	 * 
	 * @param ctx
	 * @param msg
	 */
	private void processRequest(final ChannelHandlerContext ctx, Object msg) {
		final Request request = (Request) msg;

		final long processStartTime = System.currentTimeMillis();
		try {
			try {
				RpcContext.init(request);
				processRequest(ctx, request, processStartTime);
			} finally {
				RpcContext.destroy();
			}
		} catch (RejectedExecutionException rejectException) {
			DefaultResponse response = new DefaultResponse(request.getRequestId());
			response.setException(rejectException);
			response.setProcessTime(System.currentTimeMillis() - processStartTime);
			ctx.channel().writeAndFlush(response);
		}
	}

	/**
	 * 处理client请求
	 * 
	 * @param ctx
	 * @param request
	 * @param processStartTime
	 */
	private void processRequest(ChannelHandlerContext ctx, Request request, long processStartTime) {
		NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
		Object result = handler.handle(channel, request);
		// 方法本身没有返回值，同时只有在非异常情况下才直接退出
		if (RpcContext.getContext().isOneway() || result instanceof Throwable) {
			return;
		}

		DefaultResponse response = null;
		if (!(result instanceof DefaultResponse)) {
			response = new DefaultResponse(result);
		} else {
			response = (DefaultResponse) result;
		}

		response.setRequestId(request.getRequestId());
		response.setProcessTime(System.currentTimeMillis() - processStartTime);
		if (ctx.channel().isActive()) {
			ctx.channel().writeAndFlush(response);
		}
	}

	/**
	 * <pre>
	 *  response process: 主要来自于server的响应
	 * </pre>
	 * 
	 * @param ctx
	 * @param msg
	 */
	private void processResponse(ChannelHandlerContext ctx, Object msg) {
		NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
		handler.handle(channel, msg);
	}
	
}
