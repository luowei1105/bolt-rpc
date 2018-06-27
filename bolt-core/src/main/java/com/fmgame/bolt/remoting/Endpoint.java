package com.fmgame.bolt.remoting;

import java.net.InetSocketAddress;
import com.fmgame.bolt.rpc.URL;

/**
 * 用于表示网络IO中指定服务(client或者server)的一个端点. (API/SPI, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月13日 下午3:30:24
 */
public interface Endpoint {

    /**
     * 获取指定端点唯一标识的：统一资源定位符
     *
     * @return url
     */
    URL getUrl();
    
    /**
     * 获取本地socket地址
     * 
     * @return local address.
     */
    InetSocketAddress getLocalAddress();
    
    /**
     * 发送请求，同时获得对方响应
     *
     * @param request
     * @return response
     * @throws RemotingException
     */
    Response request(Request request) throws RemotingException;

    /**
     * 关闭此端点
     */
    void close();

    /**
     * 优雅的关闭此端点
     */
    void close(int timeout);
    
    /**
     * 判断是否关闭
     *
     * @return closed
     */
    boolean isClosed();

}
