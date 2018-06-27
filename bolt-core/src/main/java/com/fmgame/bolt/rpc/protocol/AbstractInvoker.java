package com.fmgame.bolt.rpc.protocol;

import java.util.concurrent.atomic.AtomicBoolean;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.URL;

/**
 * 抽象Invoker
 * 
 * @author luowei
 * @date 2017年10月25日 上午11:44:04
 */
public abstract class AbstractInvoker<T> implements Invoker<T> {

	/** 统一资源定位符 */
	private final URL url;
	/** 服务提供接口 */
	private final Class<T> type;
	/** 是否有效节点 */
	private volatile boolean available = true;
	/** 是否销毁 */
	private AtomicBoolean destroyed = new AtomicBoolean(false);

	public AbstractInvoker(URL url, Class<T> type) {
		if (url == null)
			throw new IllegalArgumentException("service url == null");
		if (type == null)
			throw new IllegalArgumentException("service type == null");

		this.type = type;
		this.url = url;
	}

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        setAvailable(false);
	}
	
    protected void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isDestroyed() {
        return destroyed.get();
    }

	@Override
	public Class<T> getInterface() {
		return type;
	}

	@Override
	public String toString() {
		return getInterface() + " -> " + (getUrl() == null ? "" : getUrl().toString());
	}

}
