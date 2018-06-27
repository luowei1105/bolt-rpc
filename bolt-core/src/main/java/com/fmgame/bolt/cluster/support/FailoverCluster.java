package com.fmgame.bolt.cluster.support;

import com.fmgame.bolt.cluster.Cluster;
import com.fmgame.bolt.cluster.Directory;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;

/**
 * 失败转移，当出现失败，重试其它服务器，通常用于读操作，但重试会带来更长延迟。
 * 
 * @author luowei
 * @date 2018年4月3日 下午5:16:17
 */
public class FailoverCluster implements Cluster {

    public final static String NAME = "failover";

    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailoverClusterInvoker<T>(directory);
    }
    
}
