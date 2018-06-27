package com.fmgame.bolt.rpc;

import com.fmgame.bolt.extension.SPI;

/**
 * 协议
 * <p>
 * 负责点到点的通讯
 * 
 * @author luowei
 * @date 2017年10月25日 上午11:36:42
 */
@SPI
public interface Protocol {
	
	/**
	 * 暴露远程服务：<br>
	 * 1. 协议在接收请求时，应记录请求来源方地址信息：RpcContext.getContext().setRemoteAddress();<br>
	 * 
	 * @param provider
	 * @return
	 * @throws RpcException
	 */
	<T> Exporter<T> export(Provider<T> provider) throws RpcException;

	/**
	 * 引用远程服务：<br>
	 * 
	 * @param type
	 * @param url
	 * @return
	 * @throws RpcException
	 */
	<T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;

	/**
	 * 释放协议：<br>
	 * 1. 取消该协议所有已经暴露和引用的服务。<br>
     * 2. 释放协议所占用的所有资源，比如连接和端口。<br>
     * 3. 协议在释放后，依然能暴露和引用新的服务。<br>
	 */
	void destroy();
	
}
