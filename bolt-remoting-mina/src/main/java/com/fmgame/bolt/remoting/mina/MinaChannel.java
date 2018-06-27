package com.fmgame.bolt.remoting.mina;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.remoting.impl.AbstractChannel;
import com.fmgame.bolt.remoting.impl.DefaultFuture;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.rpc.RpcContext;
import com.fmgame.bolt.rpc.URL;

/**
 * mina通道
 * 
 * @author luowei
 * @date 2017年10月16日 下午7:50:21
 */
public class MinaChannel extends AbstractChannel {
	
	private static final Logger logger = LoggerFactory.getLogger(MinaChannel.class);
	
	/** mina session */
	private final IoSession session;
	/** mina channel映射rpc声明key */
	private static final String CHANNEL_KEY = MinaChannel.class.getName() + ".CHANNEL";
	
	public MinaChannel(IoSession session, URL url, MessageHandler handler) {
		super(url, handler);
        if (session == null) {
            throw new IllegalArgumentException("mina session == null;");
        }
        this.session = session;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return (InetSocketAddress) session.getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return (InetSocketAddress) session.getRemoteAddress();
	}

	@Override
	public boolean isConnected() {
		return this.session.isConnected() && !this.session.isClosing();
	}

	@Override
	public void close(int timeout) {
		super.close(timeout);
		try {
			removeChannelIfDisconnected(session);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
        try {
            session.closeOnFlush();
            if (logger.isInfoEnabled()) {
            	logger.info("Close mina session " + session);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
	}

	@Override
	public Response request(Request request) throws RemotingException {
		MinaChannel minaChannel = getOrAddChannel(this.session, url, handler);
		int timeout = minaChannel.getUrl().getIntParameter(URLParamType.CONNECT_TIMEOUT.getName(),
				URLParamType.CONNECT_TIMEOUT.getIntValue());
		int requstTimtout = minaChannel.getUrl().getIntParameter(URLParamType.REQUEST_TIMEOUT.getName(),
				URLParamType.REQUEST_TIMEOUT.getIntValue());
		
		boolean isOneway = RpcContext.getContext().isOneway();
		Response response = null;
		if (isOneway) {
			response = new DefaultResponse();
		} else {
			DefaultFuture responseFuture = new DefaultFuture(this, request, requstTimtout);
			responseFuture.addListener((ResponseFuture future) -> {
				if (future.isDone()) {
					// 成功的调用 
				} else {
					// 失败的调用 
				}
			});
			response = responseFuture;
		}
		
		WriteFuture writeFuture = this.session.write(request);
		boolean result = writeFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
		if (result && writeFuture.isDone()) {
			return response;
		}

		if (writeFuture.getException() != null) {
			throw new RemotingException(minaChannel, "MinaChannel send request to server Error: url="
					+ url.getUri() + " local=" + localAddress + " request=" + request, writeFuture.getException());
		} else {
			throw new RemotingException(minaChannel, "MinaChannel send request to server Timeout: url="
					+ url.getUri() + " local=" + localAddress + " request=" + request);
		}	
	}
	
	/**
	 * 获取通道，如果不存在则添加通道到缓存中.下次获取直接从缓存中获取
	 * 
	 * @param session
	 * @param url
	 * @param handler
	 * @return
	 */
	static MinaChannel getOrAddChannel(IoSession session, URL url, MessageHandler handler) {
        if (session == null) {
            return null;
        }
        MinaChannel ret = (MinaChannel) session.getAttribute(CHANNEL_KEY);
        if (ret == null) {
        	ret = new MinaChannel(session, url, handler);
            if (session.isConnected()) {
            	 MinaChannel old = (MinaChannel) session.setAttribute(CHANNEL_KEY, ret);
                 if (old != null) {
                     session.setAttribute(CHANNEL_KEY, old);
                     ret = old;
                 }
            }
        }
        return ret;
    }

	/**
	 * 删除通道
	 * @param session
	 */
    static void removeChannelIfDisconnected(IoSession session) {
        if (session != null && !session.isConnected()) {
        	session.removeAttribute(CHANNEL_KEY);
        }
    }

}
