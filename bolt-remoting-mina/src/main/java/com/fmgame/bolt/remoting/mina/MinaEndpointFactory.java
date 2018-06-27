package com.fmgame.bolt.remoting.mina;

import com.fmgame.bolt.remoting.Client;
import com.fmgame.bolt.remoting.EndpointFactory;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Server;
import com.fmgame.bolt.rpc.URL;

/**
 * 创建mina server和client工厂类
 * 
 * @author luowei
 * @date 2017年10月28日 下午1:54:06
 */
public class MinaEndpointFactory implements EndpointFactory {

	@Override
	public Server createServer(URL url, MessageHandler messageHandler) throws RemotingException {
		return new MinaServer(url, messageHandler);
	}

	@Override
	public Client createClient(URL url, MessageHandler messageHandler) throws RemotingException {
		return new MinaClient(url, messageHandler);
	}

}
