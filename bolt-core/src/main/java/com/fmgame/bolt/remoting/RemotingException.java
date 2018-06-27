package com.fmgame.bolt.remoting;

import java.net.InetSocketAddress;

/**
 * 远程调用异常. (API, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月13日 下午3:36:23
 */
public class RemotingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private InetSocketAddress localAddress;
	private InetSocketAddress remoteAddress;

	public RemotingException(Channel channel, String msg) {
		this(channel == null ? null : channel.getLocalAddress(), channel == null ? null : channel.getRemoteAddress(), msg);
	}

	public RemotingException(Channel channel, Throwable cause) {
		this(channel == null ? null : channel.getLocalAddress(), channel == null ? null : channel.getRemoteAddress(), cause);
	}
	
	public RemotingException(Channel channel, String message, Throwable cause) {
		this(channel == null ? null : channel.getLocalAddress(), channel == null ? null : channel.getRemoteAddress(), message, cause);
	}

	public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message) {
		super(message);

		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
	}

	public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Throwable cause) {
		super(cause);

		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
	}

	public RemotingException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message, Throwable cause) {
		super(message, cause);

		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
	}

	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

}
