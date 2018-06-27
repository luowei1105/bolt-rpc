package com.fmgame.bolt.cluster.support;

import java.util.List;
import com.fmgame.bolt.cluster.AbstractClusterInvoker;
import com.fmgame.bolt.cluster.Directory;
import com.fmgame.bolt.cluster.LoadBalance;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.utils.NetUtils;

/**
 * 快速失败，只发起一次调用，失败立即报错，通常用于非幂等性的写操作。
 * 
 * @author luowei
 * @date 2018年4月4日 下午3:14:45
 */
public class FailfastClusterInvoker<T> extends AbstractClusterInvoker<T> {

    public FailfastClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    public Response doInvoke(Request request, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        checkInvokers(invokers, request);
        Invoker<T> invoker = select(loadbalance, request, invokers, null);
        try {
            return invoker.invoke(request);
        } catch (Throwable e) {
            if (e instanceof RpcException && ((RpcException) e).isBiz()) { // biz exception.
                throw (RpcException) e;
            }
            throw new RpcException(e instanceof RpcException ? ((RpcException) e).getCode() : 0, "Failfast invoke providers " 
            		+ invoker.getUrl() + " " + loadbalance.getClass().getSimpleName() 
            		+ " select from all providers " + invokers + " for service " 
            		+ getInterface().getName() + " method " + request.getMethodName() + " on consumer " 
            		+ NetUtils.getLocalHost() + ", but no luck to perform the invocation. Last error is: " 
            		+ e.getMessage(), e.getCause() != null ? e.getCause() : e);
        }
    }
    
}
