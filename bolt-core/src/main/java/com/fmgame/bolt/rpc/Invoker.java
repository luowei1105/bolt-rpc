package com.fmgame.bolt.rpc;

import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;

/**
 * 请求调用执行类. (API/SPI, Prototype, ThreadSafe)
 * <p>
 * 调用指定服务请求,并响应.<br>
 * 
 * @author luowei
 * @date 2017年10月25日 上午11:10:10
 */
public interface Invoker<T> extends Node {

	/**
     * 获取服务接口
     *
     * @return service interface.
     */
    Class<T> getInterface();

    /**
     * 发送请求，同时获得对方响应
     *
     * @param invocation
     * @return result
     * @throws RpcException
     */
    Response invoke(Request request) throws RpcException;
    
}
