package com.fmgame.bolt.rpc.protocol;

import com.fmgame.bolt.rpc.Exporter;
import com.fmgame.bolt.rpc.Provider;

/**
 * 抽象服务暴露者. 
 * 
 * @author luowei
 * @date 2017年10月27日 下午8:00:32
 */
public abstract class AbstractExporter<T> implements Exporter<T> {

	private final Provider<T> provider;
	private volatile boolean unexported = false;
	
    public AbstractExporter(Provider<T> provider) {
		if (provider == null)
			throw new IllegalStateException("service invoker == null");
		if (provider.getUrl() == null)
			throw new IllegalStateException("service url == null");
		if (provider.getInterface() == null)
			throw new IllegalStateException("service type == null");
		if (provider.getImpl() == null)
			throw new IllegalStateException("service impl == null");
        
        this.provider = provider;
    }

	@Override
	public Provider<T> getProvider() {
		return provider;
	}

	@Override
	public void unexport() {
        if (unexported) {
            return;
        }
        unexported = true;
		provider.destroy();
	}
	
	@Override
	public String toString() {
		return provider.toString();
	}
    
}
