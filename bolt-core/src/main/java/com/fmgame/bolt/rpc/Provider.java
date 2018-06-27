package com.fmgame.bolt.rpc;

/**
 * 服务暴露提供者. (API/SPI, Prototype, ThreadSafe)
 * <p>
 * 提供服务接口和服务具体实现类
 * 
 * @author luowei
 * @date 2017年10月30日 上午10:54:02
 */
public interface Provider<T> extends Invoker<T> {

    /**
     * 获取服务实现类
     * 
     * @return
     */
    T getImpl();
    
}
