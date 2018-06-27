package com.fmgame.bolt.remoting.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.remoting.Channel;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.Request;

/**
 * 心跳消息处理封装
 * 
 * @author luowei
 * @date 2017年10月28日 下午2:52:39
 */
public class HeartBeatMessageHandleWrapper implements MessageHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(HeartBeatMessageHandleWrapper.class);
	
    private MessageHandler messageHandler;

    public HeartBeatMessageHandleWrapper(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

	@Override
	public Object handle(Channel channel, Object message) {
        if (isHeartbeatRequest(message)) {
   			if (logger.isDebugEnabled()) {
   				logger.debug("Received heartbeat from remote channel " + channel.getRemoteAddress());
   			}
   			Request request = (Request) message;
        	
            DefaultResponse response = new DefaultResponse(request.getRequestId());
            response.setValue(Constants.HEARTBEAT_METHOD_NAME);
            return response;
        }

        return messageHandler.handle(channel, message);
	}
	
    private boolean isHeartbeatRequest(Object message) {
        if (!(message instanceof Request)) {
            return false;
        }

        Request request = (Request) message;
        return Constants.HEARTBEAT_INTERFACE_NAME.equals(request.getInterfaceName())
                && Constants.HEARTBEAT_METHOD_NAME.equals(request.getMethodName())
                && Constants.HHEARTBEAT_PARAM.endsWith(request.getParamtersDesc());
    }

}
