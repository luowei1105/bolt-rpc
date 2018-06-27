package com.fmgame.bolt.cluster;

import java.util.List;
import com.fmgame.bolt.extension.SPI;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * 负载均衡器.
 * 
 * @author luowei
 * @date 2018年4月2日 下午3:18:49
 */
@SPI
public interface LoadBalance {

    /**
     * 从调用执行列表中选择一个调用执行类
     *
     * @param invokers   invokers.
     * @param url        refer url
     * @param request    request.
     * @return selected invoker.
     */
    <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Request request) throws RpcException;
    
}
