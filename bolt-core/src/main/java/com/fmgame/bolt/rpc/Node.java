package com.fmgame.bolt.rpc;

/**
 * Node. (API/SPI, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月25日 上午11:06:04
 */
public interface Node {

    /**
     * 获取指定端点唯一标识的：统一资源定位符
     *
     * @return url
     */
    URL getUrl();
    
    /**
     * 是否有效节点
     *
     * @return available.
     */
    boolean isAvailable();
    
    /**
     * 释放
     */
    void destroy();
    
}
