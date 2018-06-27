package com.fmgame.bolt.cluster;

import java.util.List;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * 抽象负载均衡
 * 
 * @author luowei
 * @date 2018年4月4日 上午10:41:17
 */
public abstract class AbstractLoadBalance implements LoadBalance {

	@Override
	public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Request request) throws RpcException {
		if (invokers == null || invokers.size() == 0)
			return null;
		if (invokers.size() == 1)
			return invokers.get(0);
		return doSelect(invokers, url, request);
	}

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Request request);
    
    protected int getWeight(Invoker<?> invoker, Request request) {
        int weight = 100;
        if (weight > 0) {

        }
        return weight;
    }
    
    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        int ww = (int) ((float) uptime / ((float) warmup / (float) weight));
        return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }
    
}
