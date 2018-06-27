package com.fmgame.bolt.remoting.mina;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.impl.AbstractClient;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.threadpool.ThreadPool;
import com.fmgame.bolt.utils.NetUtils;

/**
 * rpc by mina client
 * 
 * @author luowei
 * @date 2017年10月16日 下午6:21:07
 */
public class MinaClient extends AbstractClient {
	
	private static final Logger logger = LoggerFactory.getLogger(MinaClient.class);

	/** Nio连接对象 */
	private NioSocketConnector connector;
	/** netty通道 */
	private volatile IoSession session;
	
	public MinaClient(URL url, MessageHandler handler) throws RemotingException {
		super(url, handler);
	}

    @Override
    protected com.fmgame.bolt.remoting.Channel getChannel() {
    	IoSession c = session;
        if (c == null || !c.isConnected())
            return null;
        return MinaChannel.getOrAddChannel(c, url, handler);
    }

	@Override
	protected void doOpen() throws Throwable {
		this.connector = new NioSocketConnector(url.getIntParameter(URLParamType.IO_THREADS.name(), URLParamType.IO_THREADS.getIntValue()));// Nio连接对象
		// 设置连接超时
		this.connector.setConnectTimeoutMillis(getTimeout());
		this.connector.getSessionConfig().setTcpNoDelay(true);
		this.connector.getSessionConfig().setKeepAlive(true);
		// 断线重连回调拦截器
		this.connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30000); // 读写都空闲时间:30秒
		this.connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(this.url, this.handler, this.codec)));
		
		String threadPoolName = url.getParameter(URLParamType.THREAD_POOL.getName(), URLParamType.THREAD_POOL.getValue());
		ThreadPool pool = ExtensionLoader.getExtensionLoader(ThreadPool.class).getExtension(threadPoolName);
		this.connector.setHandler(new MinaHandler(url, handler, pool.getExecutor(url)));
		
		this.connector.setDefaultRemoteAddress(getRemoteAddress());
	}

	@Override
	protected void doClose() throws Throwable {
		if (session != null) {
			session.closeOnFlush();
		}
		if (connector != null) {
			connector.dispose();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();
        ConnectFuture future = this.connector.connect(connector.getDefaultRemoteAddress());
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        final CountDownLatch finish = new CountDownLatch(1); // resolve future.awaitUninterruptibly() dead lock
		future.addListener(new IoFutureListener() {
			public void operationComplete(IoFuture future) {
				try {
					if (future.isDone()) {
			        	IoSession newSession = future.getSession();
		                try {
		                    // 关闭旧的连接
		                	IoSession oldSession = MinaClient.this.session; // copy reference
		                    if (oldSession != null) {
		                        try {
		                            if (logger.isInfoEnabled()) {
		                                logger.info("Close old netty channel " + oldSession + " on create new netty channel " + newSession);
		                            }
		                            oldSession.closeOnFlush();
		                        } finally {
		                            MinaChannel.removeChannelIfDisconnected(oldSession);
		                        }
		                    }
		                } finally {
		                    if (MinaClient.this.isClosed()) {
		                        try {
		                            if (logger.isInfoEnabled()) {
		                                logger.info("Close new netty channel " + newSession + ", because the client closed.");
		                            }
		                            newSession.closeOnFlush();
		                        } finally {
		                            MinaClient.this.session = null;
		                            MinaChannel.removeChannelIfDisconnected(newSession);
		                        }
		                    } else {
		                        MinaClient.this.session = newSession;
		                    }
		                }
					}
				} catch (Exception e) {
					exception.set(e);
				} finally {
					finish.countDown();
				}
			}
		});
		
        try {
            finish.await(getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        	throw new RemotingException(this, "client(url: " + url.getUri() + ") failed to connect to server "
                    + getRemoteAddress() + " client-side timeout "
                    + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                    + NetUtils.getLocalHost());
        }
        
        Throwable e = exception.get();
        if (e != null) {
            throw e;
        }
	}

	@Override
	protected void doDisConnect() throws Throwable {
        try {
            MinaChannel.removeChannelIfDisconnected(session);
        } catch (Throwable t) {
            logger.warn(t.getMessage());
        }
	}
	
}
