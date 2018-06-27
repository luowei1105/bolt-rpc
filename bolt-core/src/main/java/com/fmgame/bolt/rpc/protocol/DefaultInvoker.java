package com.fmgame.bolt.rpc.protocol;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.remoting.Client;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.TimeoutException;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * 默认调用者
 * 
 * @author luowei
 * @date 2017年10月27日 下午8:44:42
 * @param <T>
 */
public class DefaultInvoker<T> extends AbstractInvoker<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultExporter.class);
	
	/** 调用列表 */
	private final Set<Invoker<?>> invokers;
	private final Client[] clients;
	private final ReentrantLock destroyLock = new ReentrantLock();
	private final AtomicInteger index = new AtomicInteger();
	
	public DefaultInvoker(Class<T> serviceType, URL url, Client[] clients, Set<Invoker<?>> invokers) {
		super(url, serviceType);
		
		this.clients = clients;
		this.invokers = invokers;
	}

	@Override
	public Response invoke(Request request) throws RpcException {
        Client currentClient = null;
        if (clients.length == 1) {
            currentClient = clients[0];
        } else {
            currentClient = clients[incrementAndGet() % clients.length];
        }
        
        try {
			return currentClient.request(request);
		} catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + request.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage());
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + request.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage());
        }
	}
	
	private final int incrementAndGet() {
        for ( ; ; ) {
    		int current = index.get();
    		int next = (current >= Integer.MAX_VALUE ? 0 : current + 1);
    		if (index.compareAndSet(current, next)) {
    			return next;
    		}
        }
	}

	@Override
	public void destroy() {
		// 防止client被关闭多次.在connect per jvm的情况下，client.close方法会调用计数器-1，当计数器小于等于0的情况下，才真正关闭
        if (super.isDestroyed()) {
            return;
        } else {
			// check ,避免多次关闭
            destroyLock.lock();
            try {
                if (super.isDestroyed()) {
                    return;
                }
                super.destroy();
                if (invokers != null) {
                    invokers.remove(this);
                }
                for (Client client : clients) {
                    try {
                        client.close(Constants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT);
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }
            } finally {
                destroyLock.unlock();
            }
        }
	}

}
