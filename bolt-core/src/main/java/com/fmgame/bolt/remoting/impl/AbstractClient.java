package com.fmgame.bolt.remoting.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.codec.Codec;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.remoting.Channel;
import com.fmgame.bolt.remoting.Client;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.rpc.RpcContext;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.NamedThreadFactory;
import com.fmgame.bolt.utils.NetUtils;

/**
 * 抽象的client
 * 
 * @author luowei
 * @date 2017年10月16日 下午4:30:43
 */
public abstract class AbstractClient extends AbstractEndpoint implements Client {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);
	
	/** 加解码 */
    protected Codec codec;	
    /** 远程地址 */
    protected InetSocketAddress remoteAddress;
    
    /** 连接锁 */
    private final Lock connectLock = new ReentrantLock();
    /** 是否重连 */
    private final int timeout; 
    /** 连接超时时间 */
    private final int connectTimeout;
    
    /********************************* 重连  ****************************/
    /** 请求消息时未连接则自动重连 */
    private final boolean request_reconnect;
    /** 重连总计 */
    private final AtomicInteger reconnect_count = new AtomicInteger(0);
    /** 重连周期 */
    private final int reconnect_period;
    /** 重连warning的间隔.(waring多少次之后，warning一次) //for test */
    private final int reconnect_waring_period;
    /** 重连调度 */
    private volatile ScheduledFuture<?> reconnectExecutorFuture = null;
    private static final ScheduledThreadPoolExecutor reconnectExecutorService = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("bolt-client-reconnectTimer", true));
    
    public AbstractClient(URL url, MessageHandler handler) throws RemotingException {
    	super(url, handler);
    	
    	this.codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(url.getParameter(URLParamType.CODEC.getName(), URLParamType.CODEC.getValue()));
    	this.remoteAddress = url.toInetSocketAddress();
    	
		// 如果为了严格意义的timeout，那么需要应用端进行控制。
    	this.timeout = url.getIntParameter(URLParamType.REQUEST_TIMEOUT.getName(), URLParamType.REQUEST_TIMEOUT.getIntValue());
    	this.connectTimeout = url.getIntParameter(URLParamType.CONNECT_TIMEOUT.getName(), URLParamType.CONNECT_TIMEOUT.getIntValue());
    	
    	this.request_reconnect = url.getBooleanParameter(URLParamType.REQUEST_RECONNECT.getName(), URLParamType.REQUEST_RECONNECT.getBooleanValue());
    	// 默认重连间隔2s
    	this.reconnect_period = url.getIntParameter(URLParamType.RECONNECT_PERIOD.getName(), URLParamType.RECONNECT_PERIOD.getIntValue());
    	// 默认重连间隔2s，30表示1分钟warning一次.
    	this.reconnect_waring_period = url.getIntParameter(URLParamType.RECONNECT_WARING_PERIOD.getName(), URLParamType.RECONNECT_WARING_PERIOD.getIntValue());
        
    	try {
    		// 开启
    		doOpen();
    	} catch (Throwable t) {
    		close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
		}
    	
        try {
        	// 连接
            connect();
            if (logger.isInfoEnabled()) {
                logger.info("Start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress() + " connect to the server " + getRemoteAddress());
            }
        } catch (RemotingException t) {
            if (url.getBooleanParameter(URLParamType.RECONNECT.getName(), URLParamType.RECONNECT.getBooleanValue())) {
            	 logger.warn("Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
            	 	+ " connect to the server " + getRemoteAddress() + " (, retry later!), cause: " + t.getMessage(), t);
            } else {
                close();
                throw t;
            }
        } catch (Throwable t) {
            close();
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }
    }
    
	/**
	 * 获取连接超时时间
	 * 
	 * @return
	 */
    protected int getTimeout() {
        return timeout;
    }
    
    /**
     * 获取连接超时时间
     * 
     * @return
     */
    protected int getConnectTimeout() {
    	return connectTimeout;
    }

	@Override
	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}
	
	@Override
	public boolean isConnected() {
        Channel channel = getChannel();
        if (channel == null)
            return false;
        return channel.isConnected();
	}

	@Override
    public void close(int timeout) {
        try {
            disconnect();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
       super.close(timeout);
        try {
            doClose();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }
	
	@Override
	public void reconnect() throws RemotingException {
        disconnect();
        connect();
	}
	
	/**
	 * 断开连接
	 */
    protected void disconnect() {
        connectLock.lock();
        try {
        	destroyReconnectThread();
            try {
                Channel channel = getChannel();
                if (channel != null) {
                    channel.close();
                }
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
            try {
                doDisConnect();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        } finally {
            connectLock.unlock();
        }
    }

	/**
	 * 连接
	 * 
	 * @throws RemotingException
	 */
    protected void connect() throws RemotingException {
        connectLock.lock();
        try {
            if (isConnected()) 
                return;
            
            initReconnectThread();
            doConnect();
            if (!isConnected()) {
                throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                        + NetUtils.getLocalHost() + ", cause: Connect wait timeout: " + getTimeout() + "ms.");
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Successed connect to server " + getRemoteAddress() + " from " + getClass().getSimpleName() + " "
                            + NetUtils.getLocalHost() + ", channel is " + this.getChannel());
                }
            }
            reconnect_count.set(0);
        } catch (RemotingException e) {
            throw e;
        } catch (Throwable e) {
            throw new RemotingException(this, "Failed connect to server " + getRemoteAddress() + " from " 
            		+ getClass().getSimpleName() + " " + NetUtils.getLocalHost(), e);
        } finally {
            connectLock.unlock();
        }
    }
    
    /**
     * 初始化重连线程
     */
    private synchronized void initReconnectThread() {
		// reconnect=false to close reconnect
        if (reconnect_period > 0 && (reconnectExecutorFuture == null || reconnectExecutorFuture.isCancelled())) {
            Runnable connectStatusCheckCommand = new Runnable() {
                public void run() {
                    try {
                        if (!isConnected()) {
                            connect();
                        } 
                    } catch (Throwable t) {
                        if (reconnect_count.getAndIncrement() % reconnect_waring_period == 0) {
                        	// 重连warning的间隔
                        	String errorMsg = "client reconnect to " + getUrl().toInetSocketAddress() + " find error . url: " + getUrl();
                            logger.error(errorMsg, t);
                        }
                    }
                }
            };
            reconnectExecutorFuture = reconnectExecutorService.scheduleWithFixedDelay(connectStatusCheckCommand,reconnect_period, reconnect_period, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 释放重连线程
     */
    private synchronized void destroyReconnectThread() {
        try {
            if (reconnectExecutorFuture != null && !reconnectExecutorFuture.isDone()) {
                reconnectExecutorFuture.cancel(true);
                reconnectExecutorService.purge();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }
    
    /**
     * Get the connected channel.
     *
     * @return channel
     */
    protected abstract Channel getChannel();
    
    /**
     * Open client.
     *
     * @throws Throwable
     */
    protected abstract void doOpen() throws Throwable;

    /**
     * Close client.
     *
     * @throws Throwable
     */
    protected abstract void doClose() throws Throwable;
    
    /**
     * Connect to server.
     *
     * @throws Throwable
     */
    protected abstract void doConnect() throws Throwable;
    
    /**
     * disConnect to server.
     *
     * @throws Throwable
     */
    protected abstract void doDisConnect() throws Throwable;
    

	@Override
	public Response request(Request request) throws RemotingException {
        if (request_reconnect && !isConnected()) {
            connect();
        }
		return request(request, RpcContext.getContext().isAsync());
	}
	
	/**
	 * 请求remote service
	 * <pre>
	 * 		1)  async requset
	 * 		2)  check if async return response, true: return ResponseFuture;  false: return result
	 * </pre>
	 * 
	 * @param request
	 * @param async
	 * @return
	 * @throws RemotingException
	 */
	private Response request(Request request, boolean async) throws RemotingException {
		com.fmgame.bolt.remoting.Channel channel = null;
		Response response = null;
		try {
			// return channel or throw exception(timeout or connection_fail)
			channel = getChannel();
			if (channel == null) {
				throw new RemotingException(this, "message can not send, because channel is closed . url:" + getUrl());
			}

			// async request
			response = channel.request(request);
		} catch (Throwable e) {
			throw new RemotingException(channel, "Client request Error: url=" + url.getUri() + " " + request, e);
		}

		// aysnc or sync result
		return asyncResponse(response, async);
	}
	
	/**
	 * 如果async是false，那么同步获取response的数据
	 * 
	 * @param response
	 * @param async
	 * @return
	 */
	private Response asyncResponse(Response response, boolean async) {
		if (async || response instanceof ResponseFuture) {
			return response;
		}
		return new DefaultResponse(response);
	}
	
    @Override
    public String toString() {
        return getClass().getName() + " [" + getLocalAddress() + " -> " + getRemoteAddress() + "]";
    }

}
