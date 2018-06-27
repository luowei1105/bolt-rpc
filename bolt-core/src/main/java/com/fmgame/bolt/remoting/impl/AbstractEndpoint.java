package com.fmgame.bolt.remoting.impl;

import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.remoting.Endpoint;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.rpc.URL;

/**
 * 抽象端点
 * 
 * @author luowei
 * @date 2017年10月19日 上午11:56:51
 */
public abstract class AbstractEndpoint implements Endpoint {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractEndpoint.class);

	/** 统一资源定位符 */
    protected final URL url;
    /** 消息处理 */
    protected final MessageHandler handler;
    /** 本地地址 */
    protected InetSocketAddress localAddress;

	/** 客户端是否关闭 */
    protected volatile boolean closed;

	public AbstractEndpoint(URL url, MessageHandler handler) {
		if (url == null) {
			throw new IllegalArgumentException("url == null");
		}
		if (handler == null) {
			throw new IllegalArgumentException("handler == null");
		}
		this.url = url;
		this.handler = handler;
		this.localAddress = getUrl().toInetSocketAddress();
	}

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	@Override
	public void close() {
        if (logger.isInfoEnabled()) {
            logger.info("Close " + getClass().getSimpleName() + " bind " + getLocalAddress());
        }
		close(0);
	}

	@Override
	public void close(int timeout) {
		this.closed = true;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

}
