package com.fmgame.bolt.remoting.mina;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.remoting.Channel;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.impl.AbstractServer;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.threadpool.ThreadPool;

/**
 * mina remote server
 * 
 * @author luowei
 * @date 2017年10月26日 下午3:33:42
 */
public class MinaServer extends AbstractServer {
	
	private static final Logger logger = LoggerFactory.getLogger(MinaServer.class);

	/** mina acceptor */
	private NioSocketAcceptor acceptor;
	/** <ip:port, channel> */
	private Map<String, Channel> channels;

	public MinaServer(URL url, MessageHandler messageHandler) throws RemotingException {
		super(url, messageHandler);
	}

	@Override
	public void close(int timeout) {
		if (!acceptor.isActive()) {
			logger.info("MinaServer close fail: already close, url={}", url.getUri());
		}
		super.close(timeout);
	}

	@Override
	public boolean isBound() {
		return acceptor.isActive();
	}

	@Override
	public Collection<Channel> getChannels() {
		Collection<IoSession> sessions = acceptor.getManagedSessions().values();
        Collection<Channel> channels = new HashSet<Channel>();
        for (IoSession session : sessions) {
            if (session.isConnected()) {
                channels.add(MinaChannel.getOrAddChannel(session, getUrl(), this.handler));
            }
        }
        return channels;
	}

	@Override
	public Channel getChannel(InetSocketAddress remoteAddress) {
        Collection<IoSession> sessions = acceptor.getManagedSessions().values();
        for (IoSession session : sessions) {
            if (session.getRemoteAddress().equals(remoteAddress)) {
                return MinaChannel.getOrAddChannel(session, getUrl(), this.handler);
            }
        }
        return null;
	}
	
	@Override
	protected void doOpen() throws Throwable {
		this.acceptor = new NioSocketAcceptor(url.getIntParameter(URLParamType.IO_THREADS.getName(), URLParamType.IO_THREADS.getIntValue()));
		this.acceptor.getSessionConfig().setTcpNoDelay(true);
		this.acceptor.setReuseAddress(true);
		// 在监听端口上排队的请求的最大长度，队列满了以后到达的客户端连接请求，会被拒绝.默认为50
		this.acceptor.setBacklog(2048);
		this.acceptor.getSessionConfig().setKeepAlive(true);
		this.acceptor.getSessionConfig().setSoLinger(0);
		
		this.acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(this.url, this.handler, this.codec)));
		// this.acceptor.getFilterChain().addLast("threadPool", createExecutorFilter());
		
		
		String threadPoolName = url.getParameter(URLParamType.THREAD_POOL.getName(), URLParamType.THREAD_POOL.getValue());
		ThreadPool pool = ExtensionLoader.getExtensionLoader(ThreadPool.class).getExtension(threadPoolName);
		this.acceptor.setHandler(new MinaHandler(url, handler, pool.getExecutor(url)));
		
		InetSocketAddress address = new InetSocketAddress(url.getPort());
		this.acceptor.bind(address);
	}
	
//	private ExecutorFilter createExecutorFilter() {
//		ExecutorFilter fileter = new ExecutorFilter(1, 
//				url.getIntParameter(URLParamType.WORKER_THREADS.getName(), URLParamType.WORKER_THREADS.getIntValue()),
//				30, TimeUnit.SECONDS, new NamedThreadFactory("bolt-minaServerExcutor"));
//		return fileter;
//	}
	
	/**
	 * 获取IO处理执行
	 * @return
	 */
	protected ThreadPoolExecutor getIOExecutor() {
		if (this.acceptor == null)
			return null;
		
		IoFilter filter = acceptor.getFilterChain().get("threadPool");
		if (filter == null)
			return null;
		
		ExecutorFilter eFilter = (ExecutorFilter) filter;
		return (ThreadPoolExecutor) eFilter.getExecutor();
	}

	@Override
	protected void doClose() throws Throwable {
        try {
            if (acceptor != null) {
            	this.acceptor.setCloseOnDeactivation(true);
        		this.acceptor.unbind();
        		this.acceptor.dispose();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        
		try {
			Collection<Channel> channels = getChannels();
			if (channels != null && channels.size() > 0) {
				for (Channel channel : channels) {
					try {
						channel.close();
					} catch (Throwable e) {
						logger.error("Server close channel Error: " + channel.getUrl().getUri(), e);
					}
				}
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
		
		try {
			if (channels != null) {
				channels.clear();
			}
		} catch (Throwable e) {
			logger.warn(e.getMessage(), e);
		}
	}
	
}
