package com.fmgame.bolt.remoting.mina;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ServiceException;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.rpc.RpcContext;
import com.fmgame.bolt.rpc.URL;

/**
 * @author luowei
 * @date 2017年10月26日 下午4:32:56
 */
public class MinaHandler extends IoHandlerAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger(MinaHandler.class);

	/** 服务 */
    private final URL url;
    /** 消息处理 */
    private final MessageHandler handler;
	/** 线程执行器 */
    private final Executor executor;

	public MinaHandler(URL url, MessageHandler handler, Executor executor) {
		this.url = url;
		this.handler = handler;
		this.executor = executor;
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
    		logger.info("MinaHandler sessionCreated.channel=" + channel);
        } finally {
            MinaChannel.removeChannelIfDisconnected(session);
        }
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
        	logger.info("MinaHandler sessionClosed.channel=" + channel);
        } finally {
            MinaChannel.removeChannelIfDisconnected(session);
        }
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
        try {
    		logger.error("MinaHandler exceptionCaught.channel=" + channel + "  cause=" + cause.getMessage());
        } finally {
            MinaChannel.removeChannelIfDisconnected(session);
        }
	}
	
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		executor.execute(() -> {
			if (message instanceof Request) {
				processRequest(session, message);
			} else if (message instanceof Response) {
				processResponse(session, message);
			} else {
				logger.error("MinaHandler messageReceived type not support: class=" + message.getClass());
				throw new ServiceException("MinaHandler messageReceived type not support: class="
						+ message.getClass());
			}
		});
	}
	
	/**
	 * <pre>
	 *  request process: 主要来自于client的请求，需要使用threadPoolExecutor进行处理，避免service message处理比较慢导致iothread被阻塞
	 * </pre>
	 * 
	 * @param session
	 * @param msg
	 */
	private void processRequest(final IoSession session, Object msg) {
		final Request request = (Request) msg;

		final long processStartTime = System.currentTimeMillis();
		try {
			try {
				RpcContext.init(request);
				processRequest(session, request, processStartTime);
			} finally {
				RpcContext.destroy();
			}
		} catch (RejectedExecutionException rejectException) {
			DefaultResponse response = new DefaultResponse(request.getRequestId());
			response.setException(rejectException);
			response.setProcessTime(System.currentTimeMillis() - processStartTime);
			session.write(response);
		}
	}

	/**
	 * 处理client请求
	 * 
	 * @param session
	 * @param request
	 * @param processStartTime
	 */
	private void processRequest(IoSession session, Request request, long processStartTime) {
		MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
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
		if (session.isActive()) {
			session.write(response);
		}
	}

	/**
	 * <pre>
	 *  response process: 主要来自于server的响应
	 * </pre>
	 * 
	 * @param session
	 * @param msg
	 */
	private void processResponse(IoSession session, Object msg) {
		MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
		handler.handle(channel, msg);
	}
	
}
