package com.fmgame.bolt.remoting;

/**
 * 用于RPC服务的client.(API/SPI, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月16日 下午4:00:39
 */
public interface Client extends Endpoint, Channel {

    /**
     * 重连
     */
    void reconnect() throws RemotingException;
}
