package com.fmgame.bolt.rpc.protocol;

import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * 调用包装类
 * 
 * @author luowei
 * @date 2017年11月1日 下午2:56:10
 */
public class InvokerWrapper<T> implements Invoker<T> {
	
    private final Invoker<T> invoker;
    private final URL url;
    
    public InvokerWrapper(Invoker<T> invoker, URL url) {
        this.invoker = invoker;
        this.url = url;
    }

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public boolean isAvailable() {
		return invoker.isAvailable();
	}

	@Override
	public void destroy() {
		invoker.destroy();
	}

	@Override
	public Class<T> getInterface() {
		 return invoker.getInterface();
	}

	@Override
	public Response invoke(Request request) throws RpcException {
		 return invoker.invoke(request);
	}

}
