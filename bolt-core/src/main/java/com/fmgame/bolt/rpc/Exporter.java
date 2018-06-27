package com.fmgame.bolt.rpc;

/**
 * 服务暴露者. (API/SPI, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月25日 上午11:38:09
 */
public interface Exporter<T> {

    /**
     * get 提供者.
     *
     * @return invoker
     */
	Provider<T> getProvider();

    /**
     * unexport.
     * <p>
     * <code>
     * getInvoker().destroy();
     * </code>
     */
    void unexport();
}
