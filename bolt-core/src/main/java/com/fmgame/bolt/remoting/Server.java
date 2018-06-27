package com.fmgame.bolt.remoting;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * 用于RPC服务的server.(API/SPI, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月16日 下午4:03:37
 */
public interface Server extends Endpoint {

    /**
     * is server bound
     * 
     * @return
     */
    boolean isBound();

    /**
     * 获取所有连接通道
     * 
     * @return channels
     */
    Collection<Channel> getChannels();
    
    /**
     * 获取指定地址的连接通道
     *
     * @param remoteAddress
     * @return channel
     */
    Channel getChannel(InetSocketAddress remoteAddress);
    
}
