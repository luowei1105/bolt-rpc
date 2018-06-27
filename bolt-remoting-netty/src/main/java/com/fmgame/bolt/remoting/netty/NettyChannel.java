package com.fmgame.bolt.remoting.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.remoting.impl.AbstractChannel;
import com.fmgame.bolt.remoting.impl.DefaultFuture;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.rpc.RpcContext;
import com.fmgame.bolt.rpc.URL;
import io.netty.channel.ChannelFuture;

/**
 * netty通道
 * 
 * @author luowei
 * @date 2017年10月16日 下午7:50:21
 */
public class NettyChannel extends AbstractChannel {
	
	private static final Logger logger = LoggerFactory.getLogger(NettyChannel.class);
	
	/** netty channel */
	private final io.netty.channel.Channel channel;
	/** netty channel映射rpc声明channel */
	private static final ConcurrentMap<io.netty.channel.Channel, NettyChannel> channelMap = new ConcurrentHashMap<>();
	
	public NettyChannel(io.netty.channel.Channel channel, URL url, MessageHandler handler) {
		super(url, handler);
        if (channel == null) {
            throw new IllegalArgumentException("netty channel == null;");
        }
        this.channel = channel;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return (InetSocketAddress) channel.localAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return (InetSocketAddress) channel.remoteAddress();
	}

	@Override
	public boolean isConnected() {
		return channel.isActive();
	}

	@Override
	public void close(int timeout) {
		super.close(timeout);
        try {
            removeChannelIfDisconnected(channel);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            channel.close();
            Thread.sleep(1);
            if (logger.isInfoEnabled()) {
            	logger.info("Close netty channel " + channel);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
	}

	@Override
	public Response request(Request request) throws RemotingException {
		NettyChannel nettyChannel = getOrAddChannel(this.channel, url, handler);
		int timeout = nettyChannel.getUrl().getIntParameter(URLParamType.CONNECT_TIMEOUT.getName(),
				URLParamType.CONNECT_TIMEOUT.getIntValue());
		int requstTimtout = nettyChannel.getUrl().getIntParameter(URLParamType.REQUEST_TIMEOUT.getName(),
				URLParamType.REQUEST_TIMEOUT.getIntValue());
		
		boolean isOneway = RpcContext.getContext().isOneway();
		Response response = null;
		if (isOneway) {
			response = new DefaultResponse();
		} else {
			DefaultFuture responseFuture = new DefaultFuture(this, request, requstTimtout);
			responseFuture.addListener((ResponseFuture future) -> {
				if (future.isDone()) {
					// 成功的调用 
				} else {
					// 失败的调用 
				}
			});
			response = responseFuture;
		}
		
		ChannelFuture writeFuture = this.channel.writeAndFlush(request);
		boolean result = writeFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
		if (result && writeFuture.isSuccess()) {
			return response;
		}

		if (writeFuture.cause() != null) {
			throw new RemotingException(nettyChannel, "NettyChannel send request to server Error: url="
					+ url.getUri() + " local=" + localAddress + " request=" + request, writeFuture.cause());
		} else {
			throw new RemotingException(nettyChannel, "NettyChannel send request to server Timeout: url="
					+ url.getUri() + " local=" + localAddress + " request=" + request);
		}	
	}
	
	/**
	 * 获取通道，如果不存在则添加通道到缓存中.下次获取直接从缓存中获取
	 * 
	 * @param ch
	 * @param url
	 * @param handler
	 * @return
	 */
	static NettyChannel getOrAddChannel(io.netty.channel.Channel ch, URL url, MessageHandler handler) {
        if (ch == null) {
            return null;
        }
        NettyChannel ret = channelMap.get(ch);
        if (ret == null) {
            NettyChannel nc = new NettyChannel(ch, url, handler);
            if (ch.isActive()) {
                ret = channelMap.putIfAbsent(ch, nc);
            }
            if (ret == null) {
                ret = nc;
            }
        }
        return ret;
    }

	/**
	 * 删除通道
	 * @param ch
	 */
    static void removeChannelIfDisconnected(io.netty.channel.Channel ch) {
        if (ch != null && !ch.isActive()) {
            channelMap.remove(ch);
        }
    }

}
