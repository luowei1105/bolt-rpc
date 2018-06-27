package com.fmgame.bolt.remoting.impl;

import com.fmgame.bolt.remoting.Channel;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.rpc.URL;

/**
 * 抽象通道
 * 
 * @author luowei
 * @date 2017年10月18日 下午2:56:43
 */
public abstract class AbstractChannel extends AbstractEndpoint implements Channel {
	
    public AbstractChannel(URL url, MessageHandler handler) {
		super(url, handler);
	}

	@Override
	public Response request(Request request) throws RemotingException {
        if (isClosed()) {
            throw new RemotingException(this, "Failed to send message "
                    + (request == null ? "" : request.getClass().getName()) + ":" + request
                    + ", cause: Channel closed. channel: " + getLocalAddress() + " -> " + getRemoteAddress());
        }
		return null;
	}

	@Override
    public String toString() {
        return getLocalAddress() + " -> " + getRemoteAddress();
    }

}
