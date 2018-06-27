package com.fmgame.bolt.remoting.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.codec.Codec;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.Server;
import com.fmgame.bolt.rpc.URL;

/**
 * 抽象的server
 * 
 * @author luowei
 * @date 2017年10月16日 下午4:23:51
 */
public abstract class AbstractServer extends AbstractEndpoint implements Server {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractServer.class);
	
    /** 加解密 */
    protected Codec codec;

    public AbstractServer(URL url, MessageHandler handler) throws RemotingException {
    	super(url, handler);
    	
        this.localAddress = getUrl().toInetSocketAddress();
    	this.codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(url.getParameter(URLParamType.CODEC.getName(), URLParamType.CODEC.getValue()));
        // 开启
        try {
        	if (logger.isInfoEnabled()) {
        		logger.info("Start " + getClass().getSimpleName() + " bind " + getLocalAddress());
        	}
            doOpen();
        } catch (Throwable t) {
            throw new RemotingException(url.toInetSocketAddress(), null, "Failed to bind " + getClass().getSimpleName()
                    + " on " + getLocalAddress() + ", cause: " + t.getMessage(), t);
        }
    }
    
	@Override
	public void close(int timeout) {
		super.close(timeout);
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
	}

	/**
     * 开启服务
     * 
     * @throws Throwable
     */
    protected abstract void doOpen() throws Throwable;
    
    /**
     * 关闭服务
     * 
     * @throws Throwable
     */
    protected abstract void doClose() throws Throwable;
    

	@Override
	public Response request(Request request) throws RemotingException {
		throw new RemotingException(getLocalAddress(), null, new UnsupportedOperationException("server 不支持主动下发消息"));
	}
    
    @Override
    public String toString() {
        return getClass().getName() + " [" + getLocalAddress() + "]";
    }
    
}
