package com.fmgame.bolt.cluster;

import java.util.List;
import com.fmgame.bolt.cluster.Directory;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * 抽象包含调用列表的目录对象
 * 
 * @author luowei
 * @date 2018年4月2日 下午3:33:25
 */
public abstract class AbstractDirectory<T> implements Directory<T> {

	/** 指定url */
	private final URL url;
	/** 是否消耗 */
	private volatile boolean destroyed = false;
	
    public AbstractDirectory(URL url) {
        this.url = url;
    }

	public URL getUrl() {
		return url;
	}
    
    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        destroyed = true;
    }

	@Override
	public List<Invoker<T>> list(Request request) throws RpcException {
        if (destroyed) {
            throw new RpcException("Directory already destroyed .url: " + getUrl());
        }
        return doList(request);
	}
    
	protected abstract List<Invoker<T>> doList(Request request) throws RpcException;
    
}
