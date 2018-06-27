package com.fmgame.bolt.cluster.support;

import com.fmgame.bolt.cluster.Cluster;
import com.fmgame.bolt.cluster.Directory;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;

/**
 * 快速失败，只发起一次调用，失败立即报错，通常用于非幂等性的写操作。
 * 
 * @author luowei
 * @date 2018年4月4日 下午3:14:25
 */
public class FailfastCluster implements Cluster {

	@Override
	public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
		return new FailfastClusterInvoker<T>(directory);
	}

}
