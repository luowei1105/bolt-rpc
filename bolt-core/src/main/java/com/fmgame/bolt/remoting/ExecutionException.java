
package com.fmgame.bolt.remoting;

import java.net.InetSocketAddress;

/**
 * 执行异常
 * 
 * @author luowei
 * @date 2017年10月31日 上午11:24:34
 */
public class ExecutionException extends RemotingException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6835052603780058799L;
	
	private final Object request;

    public ExecutionException(Object request,Channel channel, String msg) {
		super(channel, msg);
		this.request = request;
	}
    
    public ExecutionException(Object request, Channel channel, Throwable cause) {
    	super(channel, cause);
    	this.request = request;
    }

    public ExecutionException(Object request, Channel channel, String message, Throwable cause) {
        super(channel, message, cause);
        this.request = request;
    }

    public ExecutionException(Object request, InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message) {
        super(localAddress, remoteAddress, message);
        this.request = request;
    }

    public ExecutionException(Object request, InetSocketAddress localAddress, InetSocketAddress remoteAddress, Throwable cause) {
        super(localAddress, remoteAddress, cause);
        this.request = request;
    }
    
    public ExecutionException(Object request, InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message, Throwable cause) {
        super(localAddress, remoteAddress, message, cause);
        this.request = request;
    }

    public Object getRequest() {
        return request;
    }

}