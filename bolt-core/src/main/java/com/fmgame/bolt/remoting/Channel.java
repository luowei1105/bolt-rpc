package com.fmgame.bolt.remoting;

import java.net.InetSocketAddress;

/**
 * 用于client对server进行网络IO操作的开放连接. (API/SPI, Prototype, ThreadSafe)
 * <p>
 * 通道可处于连接或关闭状态。创建通道时它处于连接状态，一旦关闭，则保持关闭状态。
 * 通过调用通道的<code>isConnected</code>方法可测试通道是否处于连接状态。
 * 
 * @author luowei
 * @date 2017年10月13日 下午3:34:32
 */
public interface Channel extends Endpoint {

    /**
     * 获取远程socket地址
     * 
     * @return
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 判断此通道是否处于连接状态
     *
     * @return 
     */
    boolean isConnected();
    
}
