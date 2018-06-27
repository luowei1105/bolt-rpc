package com.fmgame.bolt.remoting;

/**
 * 消息处理. (API, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月16日 下午4:16:27
 */
@FunctionalInterface
public interface MessageHandler {
	
	/**
	 * 处理通道消息
	 * 
	 * @param channel
	 * @param message
	 * @return
	 */
    Object handle(Channel channel, Object message);
    
}
