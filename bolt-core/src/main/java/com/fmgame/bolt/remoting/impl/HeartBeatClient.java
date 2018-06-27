package com.fmgame.bolt.remoting.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.remoting.Client;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.rpc.RpcContext;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.NamedThreadFactory;
import com.fmgame.bolt.utils.RequestIdGenerator;

/**
 * 支持心跳检测client
 * 
 * @author luowei
 * @date 2017年10月28日 下午2:17:34
 */
public class HeartBeatClient implements Client {
	
	private static final Logger logger = LoggerFactory.getLogger(HeartBeatClient.class);
	
	/** 心跳调度处理 */
	private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2, new NamedThreadFactory("bolt-remoting-client-heartbeat", true));
	
	/** 封装client */
	private final Client client;
	/** 心跳定时器 */
	private ScheduledFuture<?> heatbeatTimer;
	/** 心跳超时，毫秒。缺省0，不会执行心跳。*/
	private int heartbeat;
	/** 心跳超时时间 */
	private int heartbeatTimeout;
	
	/** 上次写消息时间戳 */
	private final AtomicLong lastRead_timestamp = new AtomicLong(System.currentTimeMillis());
	/** 上次读消息时间戳 */
	private final AtomicLong lastWrite_timestamp = new AtomicLong(0);
	
	public HeartBeatClient(Client client, boolean needHeartbeat) {
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }
        this.client = client;
        this.heartbeat = client.getUrl().getIntParameter(URLParamType.HEARTBEAT.getName(), URLParamType.HEARTBEAT.getIntValue());
        this.heartbeatTimeout = client.getUrl().getIntParameter(URLParamType.HEARTBEAT_TIMEOUT.getName(), URLParamType.HEARTBEAT_TIMEOUT.getIntValue());
        
        if (needHeartbeat) {
            startHeatbeatTimer();
        }
	}

	@Override
	public URL getUrl() {
		return client.getUrl();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return client.getLocalAddress();
	}

	@Override
	public Response request(Request request) throws RemotingException {
		return client.request(request);
	}

	@Override
	public void close() {
		this.close(0);
	}

	@Override
	public void close(int timeout) {
		client.close(timeout);
		stopHeartbeatTimer();
	}

	@Override
	public boolean isClosed() {
		return client.isClosed();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return client.getRemoteAddress();
	}

	@Override
	public boolean isConnected() {
		return client.isConnected();
	}

	@Override
	public void reconnect() throws RemotingException {
		client.reconnect();
	}

    private void startHeatbeatTimer() {
        stopHeartbeatTimer();
        if (heartbeat > 0) {
            heatbeatTimer = executorService.scheduleWithFixedDelay(() -> {
                try {
                    long now = System.currentTimeMillis();
                    try {
        				long lastRead = HeartBeatClient.this.lastRead_timestamp.get();
        				long lastWrite = HeartBeatClient.this.lastWrite_timestamp.get();
                        if ((now - lastRead > heartbeat) || (now - lastWrite > heartbeat)) {
                			RpcContext.getContext().putAttribute(Constants.ASYNC_SUFFIX, true);
                			ResponseFuture response = (ResponseFuture) client.request(createRequest());
                			response.addListener(future -> {
								if (future.isDone()) {
									HeartBeatClient.this.lastRead_timestamp.getAndSet(System.currentTimeMillis());
									
									long interval = future.getProcessTime();
					                if (logger.isDebugEnabled()) {
				                        logger.debug("Received heartbeat response. interval time:" + interval + "ms");
				                    }
								}
                			});
                        	HeartBeatClient.this.lastWrite_timestamp.getAndSet(now);
                			
                            if (logger.isDebugEnabled()) {
                                logger.debug("Send heartbeat to remote client " + client.getRemoteAddress()
                                        + ", heartbeat period: " + heartbeat + "ms");
                            }
                        }
                        if (now - lastRead > heartbeatTimeout) {
                            logger.warn("Close client " + client  + ", because heartbeat read idle time out: " + heartbeatTimeout + "ms");
                            client.reconnect();
                        }
                    } catch (Throwable t) {
                        logger.warn("Exception when heartbeat to remote client " + client.getRemoteAddress(), t);
                    }
                } catch (Throwable t) {
                    logger.warn("Unhandled exception when heartbeat, cause: " + t.getMessage(), t);
                }
            }, heartbeat, heartbeat, TimeUnit.MILLISECONDS);
        }
    }
    
    private void stopHeartbeatTimer() {
        if (heatbeatTimer != null && !heatbeatTimer.isCancelled()) {
            try {
                heatbeatTimer.cancel(true);
            } catch (Throwable e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        heatbeatTimer = null;
    }
    
    private Request createRequest() {
        DefaultRequest request = new DefaultRequest();

        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(Constants.HEARTBEAT_INTERFACE_NAME);
        request.setMethodName(Constants.HEARTBEAT_METHOD_NAME);
        request.setParamtersDesc(Constants.HHEARTBEAT_PARAM);

        return request;
    }

	@Override
	public String toString() {
		return "HeartBeatClient [client=" + client + "]";
	}
    
}
