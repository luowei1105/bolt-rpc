package com.fmgame.bolt.remoting.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.remoting.Channel;
import com.fmgame.bolt.remoting.FutureListener;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.remoting.TimeoutException;

/**
 * default future
 * 
 * @author luowei
 * @date 2017年10月20日 下午3:11:13
 */
public class DefaultFuture implements ResponseFuture {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

    // invoke id.
    private final long id;
    private final Channel channel;
    private final Request request;
    private final int timeout;
    private volatile List<FutureListener> listeners;
    private final long start = System.currentTimeMillis();
    
    // lock
    private final Lock lock = new ReentrantLock();
    private final Condition done = lock.newCondition();
    
    // 异步map
    private static final Map<Long, DefaultFuture> FUTURES = new ConcurrentHashMap<>();
    // <requestId, channel>
    private static final Map<Long, Channel> CHANNELS = new ConcurrentHashMap<>();
    
    // 返回
    private volatile long sent;
    private volatile Response response;
    
    static {
        Thread th = new Thread(new TimeoutMonitor(), "BoltTimeoutMonitor");
        th.setDaemon(true);
        th.start();
    }
    
    public DefaultFuture(Channel channel, Request request, int timeout) {
		this.id = request.getRequestId();
        this.channel = channel;
        this.request = request;
        this.timeout = timeout > 0 ? timeout : channel.getUrl().getIntParameter(URLParamType.REQUEST_TIMEOUT.getName(), URLParamType.REQUEST_TIMEOUT.getIntValue());
        
        // put into waiting map.
        FUTURES.put(id, this);
        CHANNELS.put(id, channel);
    }

	@Override
	public Object get() throws RemotingException {
		return get(timeout);
	}

	@Override
	public Object get(int timeout) throws RemotingException {
		if (timeout <= 0) {
			timeout = URLParamType.REQUEST_TIMEOUT.getIntValue();
		}
		if (!isDone()) {
			long start = System.currentTimeMillis();
			lock.lock();
			try {
				while (!isDone()) {
					done.await(timeout, TimeUnit.MILLISECONDS);
					if (isDone() || System.currentTimeMillis() - start > timeout) {
						break;
					}
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				lock.unlock();
			}
			if (!isDone()) {
				throw new TimeoutException(sent > 0, channel, getTimeoutMessage(false));
			}
		}
		return returnFromResponse();
	}

	private Object returnFromResponse() throws RemotingException {
		Response res = response;
		if (res == null) {
			throw new IllegalStateException("response cannot be null");
		}
		
		Exception exception = res.getException();
		if (exception != null) {
			throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RuntimeException(
					exception.getMessage(), exception);
		}

		return res.getResult();
	}

	@Override
	public boolean isDone() {
		return response != null;
	}

	@Override
	public void addListener(FutureListener listener) {
		if (listener == null) {
			throw new NullPointerException("FutureListener is null");
		}
		
		if (isDone()) {
			notifyListener(listener);
		} else {
            boolean notifyNow = false;
            lock.lock();
            try {
                if (isDone()) {
                	notifyNow = true;
                } else {
    				if (listeners == null) {
    					listeners = new ArrayList<>(1);
    				}
    				listeners.add(listener);
                }
            } finally {
                lock.unlock();
            }
            if (notifyNow) {
            	notifyListener(listener);
            }
		}
	}
	
	private void notifyListener(FutureListener listener) {
		try {
			listener.operationComplete(this);
		} catch (Throwable t) {
			logger.error("ResponseFuture notifyListener Error: " + listener.getClass().getSimpleName(), t);
		}
	}
	
	private void notifyListeners() {
		if (listeners == null) 
			return;
		
		for (FutureListener listener : listeners) {
			notifyListener(listener);
		}
	}

	@Override
	public long getRequestId() {
		return request.getRequestId();
	}

	@Override
	public long getProcessTime() {
		return response.getProcessTime();
	}

	@Override
	public void setProcessTime(long time) {
		response.setProcessTime(time);
	}

	@Override
	public Object getResult() {
		try {
			return get();
		} catch (RemotingException e) {
			return e;
		}
	}

	@Override
	public Exception getException() {
		return response != null ? response.getException() : null;
	}

	@Override
	public Map<String, Object> getAttachments() {
		return response != null ? response.getAttachments() : null;
	}

	@Override
	public void setAttachment(String key, Object value) {
		if (response == null) {
			throw new IllegalArgumentException("response is null");
		}
		response.setAttachment(key, value);
	}
	
    private String getTimeoutMessage(boolean scan) {
        long nowTimestamp = System.currentTimeMillis();
        return (sent > 0 ? "Waiting server-side response timeout" : "Sending request timeout in client-side")
                + (scan ? " by scan timer" : "") + ". start time: "
                + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(start))) + ", end time: "
                + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())) + ","
                + (sent > 0 ? " client elapsed: " + (sent - start)
                + " ms, server elapsed: " + (nowTimestamp - sent)
                : " elapsed: " + (nowTimestamp - start)) + " ms, timeout: "
                + timeout + " ms, request: " + request + ", channel: " + channel.getLocalAddress()
                + " -> " + channel.getRemoteAddress();
    }
	
    private void doReceived(Response res) {
        lock.lock();
        try {
            response = res;
            if (done != null) {
                done.signal();
            }
        } finally {
            lock.unlock();
        }
        // 通知所有监听
        notifyListeners();
    }
	
    public static void received(Channel channel, Response response) {
        try {
        	DefaultFuture future = FUTURES.remove(response.getRequestId());
            if (future != null) {
                future.doReceived(response);
            } else {
                logger.warn("The timeout response finally returned at "
                        + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
                        + ", response " + response
                        + (channel == null ? "" : ", channel: " + channel.getLocalAddress()
                        + " -> " + channel.getRemoteAddress()));
            }
        } finally {
            CHANNELS.remove(response.getRequestId());
        }
    }
    
	/**
	 * 超时监听
	 * 
	 * @author luowei
	 * @date 2017年10月20日 下午3:44:53
	 */
	static class TimeoutMonitor implements Runnable {

		public void run() {
			while (true) {
				try {
					for (DefaultFuture future : FUTURES.values()) {
						if (future == null || future.isDone()) 
							continue;
						
						if (System.currentTimeMillis() - future.start > future.timeout) {
							// create exception response.
							DefaultResponse timeoutResponse = new DefaultResponse(future.getRequestId());
							timeoutResponse.setException(new TimeoutException(future.start > 0, future.channel, future.getTimeoutMessage(true)));
							// handle response.
							DefaultFuture.received(future.channel, timeoutResponse);
						}
					}
					Thread.sleep(30);
				} catch (Throwable e) {
					logger.error("Exception when scan the timeout invocation of remoting.", e);
				}
			}
		}
	}
	
}
