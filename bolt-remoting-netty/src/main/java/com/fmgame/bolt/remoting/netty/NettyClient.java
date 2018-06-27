package com.fmgame.bolt.remoting.netty;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * rpc by netty client
 * 
 * @author luowei
 * @date 2017年10月16日 下午6:21:07
 */
public class NettyClient extends AbstractClient {
	
	private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

	/** netty引导类 */
	private Bootstrap bootstrap;
	/** netty通道 */
	private volatile Channel channel;
	
	public NettyClient(URL url, MessageHandler handler) throws RemotingException {
		super(url, handler);
	}

    @Override
    protected com.fmgame.bolt.remoting.Channel getChannel() {
    	Channel c = channel;
        if (c == null || !c.isActive())
            return null;
        return NettyChannel.getOrAddChannel(c, url, handler);
    }

	@Override
	protected void doOpen() throws Throwable {
		// Configure the client.
		bootstrap = new Bootstrap();
		EventLoopGroup group = new NioEventLoopGroup(url.getIntParameter(URLParamType.IO_THREADS.getName(), URLParamType.IO_THREADS.getIntValue()));
		// 配置worker线程池
		String threadPoolName = url.getParameter(URLParamType.THREAD_POOL.getName(), URLParamType.THREAD_POOL.getValue());
		ThreadPool pool = ExtensionLoader.getExtensionLoader(ThreadPool.class).getExtension(threadPoolName);
		Executor executor = pool.getExecutor(url);
		
		bootstrap.group(group).channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.handler(new ChannelInitializer<SocketChannel>() {
					
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						NettyCodecAdapter adapter = new NettyCodecAdapter(url, handler, codec);
						ChannelPipeline p = ch.pipeline();
						
						p.addLast(adapter.getDecoder());
						p.addLast(adapter.getEncoder());
						p.addLast(new NettyHandler(NettyClient.this.getUrl(), handler, executor));
					}
				});
	}

	@Override
	protected void doClose() throws Throwable {
		if (channel != null) {
			channel.close();
		}
	}

	@Override
	protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();
        ChannelFuture future = bootstrap.connect(getRemoteAddress()).sync();
        try {
            boolean ret = future.awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);
            if (ret && future.isSuccess()) {
            	Channel newChannel = future.channel();
                try {
                    // 关闭旧的连接
                	Channel oldChannel = NettyClient.this.channel; // copy reference
                    if (oldChannel != null) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + newChannel);
                            }
                            oldChannel.close();
                        } finally {
                            NettyChannel.removeChannelIfDisconnected(oldChannel);
                        }
                    }
                } finally {
                    if (NettyClient.this.isClosed()) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close new netty channel " + newChannel + ", because the client closed.");
                            }
                            newChannel.close();
                        } finally {
                            NettyClient.this.channel = null;
                            NettyChannel.removeChannelIfDisconnected(newChannel);
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("client(url: " + url.getUri() + ") succeeded to connect to server ");
                        }
                        NettyClient.this.channel = newChannel;
                    }
                }
            } else if (future.cause() != null) {
                throw new RemotingException(this, "client(url: " + url.getUri() + ") failed to connect to server " + getRemoteAddress(), future.cause());
            } else {
                throw new RemotingException(this, "client(url: " + url.getUri() + ") failed to connect to server "
                        + getRemoteAddress() + " client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                        + NetUtils.getLocalHost());
            }
        } finally {
            if (!isConnected()) {
                future.cancel(true);
            }
        }
	}

	@Override
	protected void doDisConnect() throws Throwable {
        try {
            NettyChannel.removeChannelIfDisconnected(channel);
        } catch (Throwable t) {
            logger.warn(t.getMessage());
        }
	}
	
}
