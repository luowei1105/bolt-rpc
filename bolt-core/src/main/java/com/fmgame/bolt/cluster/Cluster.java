package com.fmgame.bolt.cluster;

import com.fmgame.bolt.extension.SPI;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;

/**
 * 集群处理类. (SPI, Singleton, ThreadSafe)
 * 
 * @author luowei
 * @date 2018年4月2日 下午3:15:23
 */
@SPI
public interface Cluster {

	/**
	 * 合并调用列表生产一个调用代理.
	 * 
	 * @param directory
	 * @return
	 * @throws RpcException
	 */
	<T> Invoker<T> join(Directory<T> directory) throws RpcException;
	
}
