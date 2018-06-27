package com.fmgame.bolt.remoting.netty;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
import com.fmgame.bolt.utils.NamedThreadFactory;
import com.fmgame.bolt.utils.NetUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * netty remote server
 * 
 * @author luowei
 * @date 2017年10月16日 下午4:48:37
 */
public class NettyServer extends AbstractServer {
	
	private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

	/** netty引导类 */
	private ServerBootstrap bootstrap;
	/** boss线程 */
	private EventLoopGroup bossGroup;
	/** 工作线程 */
	private EventLoopGroup workerGroup;
	/** netty通道 */
	private io.netty.channel.Channel serverChannel;
	/** <ip:port, channel> */
	private Map<String, Channel> channels;

	public NettyServer(URL url, MessageHandler messageHandler) throws RemotingException {
		super(url, messageHandler);
	}

	@Override
	public void close(int timeout) {
		if (!serverChannel.isOpen()) {
			logger.info("NettyServer close fail: already close, url={}", url.getUri());
		}
		super.close(timeout);
	}

	@Override
	public boolean isBound() {
		return serverChannel.isActive();
	}

	@Override
	public Collection<Channel> getChannels() {
        Collection<Channel> chs = new HashSet<>();
        for (Channel channel : this.channels.values()) {
            if (channel.isConnected()) {
                chs.add(channel);
            } else {
                channels.remove(NetUtils.toAddressString(channel.getRemoteAddress()));
            }
        }
        return chs;
	}

	@Override
	public Channel getChannel(InetSocketAddress remoteAddress) {
		return channels.get(NetUtils.toAddressString(remoteAddress));
	}
	
	@Override
	protected void doOpen() throws Throwable {
		// Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new NamedThreadFactory("bolt-nettyServerboss", true));
        EventLoopGroup workerGroup = new NioEventLoopGroup(url.getIntParameter(URLParamType.IO_THREADS.getName(), URLParamType.IO_THREADS.getIntValue()), new NamedThreadFactory("bolt-nettyServerWorker", true));
		
    	String threadPoolName = url.getParameter(URLParamType.THREAD_POOL.getName(), URLParamType.THREAD_POOL.getValue());
    	ThreadPool pool = ExtensionLoader.getExtensionLoader(ThreadPool.class).getExtension(threadPoolName);
    	final NettyHandler nettyHandler = new NettyHandler(NettyServer.this.getUrl(), handler, pool.getExecutor(url));
    	channels = nettyHandler.getChannels();
    	
		bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
	        .channel(NioServerSocketChannel.class)
	        .childOption(ChannelOption.TCP_NODELAY, true)
	        // 地址重用
	        .childOption(ChannelOption.SO_REUSEADDR, true)
	    	// 在监听端口上排队的请求的最大长度，队列满了以后到达的客户端连接请求，会被拒绝.默认配置为50
	        .childOption(ChannelOption.SO_BACKLOG, 2048)
	        // 缓存分配方式
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
            	
                @Override
                public void initChannel(NioSocketChannel ch) throws Exception {
					NettyCodecAdapter adapter = new NettyCodecAdapter(url, handler, codec);
					ChannelPipeline p = ch.pipeline();
					
					p.addLast(adapter.getDecoder());
					p.addLast(adapter.getEncoder());
					p.addLast(nettyHandler);
                }
            });
		
		 // Start the server.
        ChannelFuture f = bootstrap.bind(url.getPort());	
        // set serverChannel
        serverChannel = f.channel();
        // sync until the server socket is closed.
        f.syncUninterruptibly();
	}

	@Override
	protected void doClose() throws Throwable {
        try {
    		if (serverChannel != null) {
    			serverChannel.close();
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
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        
        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
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
