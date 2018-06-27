package com.fmgame.bolt.cluster;

import java.util.List;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.Node;
import com.fmgame.bolt.rpc.RpcException;

/**
 * 包含调用列表的目录对象
 * 
 * @author luowei
 * @date 2018年4月2日 下午3:16:32
 */
public interface Directory<T> extends Node {

	/**
     * 获取服务类型
     *
     * @return service type.
     */
    Class<T> getInterface();
    
    /**
     * 获取调用列表
     *
     * @return request
     */
    List<Invoker<T>> list(Request request) throws RpcException;
    
}
