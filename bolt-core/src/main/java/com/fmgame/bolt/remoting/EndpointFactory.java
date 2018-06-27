package com.fmgame.bolt.remoting;

import com.fmgame.bolt.extension.SPI;
import com.fmgame.bolt.rpc.URL;

/**
 * 网络IO端点创建工厂类
 * 
 * @author luowei
 * @date 2017年10月28日 下午1:50:00
 */
@SPI
public interface EndpointFactory {
	
	/**
	 * 创建服务
	 * 
	 * @param url
	 * @param messageHandler
	 * @return
	 * @throws RemotingException
	 */
	Server createServer(URL url, MessageHandler messageHandler) throws RemotingException;
	
	/**
	 * 创建客户端
	 * 
	 * @param url
	 * @param messageHandler
	 * @return
	 * @throws RemotingException
	 */
	Client createClient(URL url, MessageHandler messageHandler) throws RemotingException;
	
}
