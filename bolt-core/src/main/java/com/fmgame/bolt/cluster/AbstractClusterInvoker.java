package com.fmgame.bolt.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.NetUtils;

/**
 * 抽象的请求调用代理类
 * 
 * @author luowei
 * @date 2018年4月3日 下午12:49:22
 */
public abstract class AbstractClusterInvoker<T> implements Invoker<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractClusterInvoker.class);
	
	protected final Directory<T> directory;
    private AtomicBoolean destroyed = new AtomicBoolean(false);

	public AbstractClusterInvoker(Directory<T> directory) {
        if (directory == null)
            throw new IllegalArgumentException("service directory == null");

        this.directory = directory;
	}

	@Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }
	
    public URL getUrl() {
        return directory.getUrl();
    }
    
    @Override
    public boolean isAvailable() {
        return directory.isAvailable();
    }
    
    @Override
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            directory.destroy();
        }
    }
    
    protected void checkInvokers(List<Invoker<T>> invokers, Request request) {
        if (invokers == null || invokers.size() == 0) {
            throw new RpcException("Failed to invoke the method "
                    + request.getMethodName() + " in the service " + getInterface().getName()
                    + ". No provider available for the service " + directory.getUrl().getUri()
                    + " from registry " + directory.getUrl().getAddress()
                    + " on the consumer " + NetUtils.getLocalHost()
                    + ". Please check if the providers have been started and registered.");
        }
    }
    
	/**
	 * 使用loadbalance选择invoker.</br>
	 * a)先lb选择，如果在selected列表中 或者 不可用且做检验时，进入下一步(重选),否则直接返回</br>
	 * b)重选验证规则：selected > available .保证重选出的结果尽量不在select中，并且是可用的
	 *
	 * @param availablecheck 如果设置true，在选择的时候先选invoker.available == true
	 * @param selected 已选过的invoker.注意：输入保证不重复
	 */
	protected Invoker<T> select(LoadBalance loadbalance, Request request, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {
		if (invokers == null || invokers.size() == 0)
			return null;
		
		return doselect(loadbalance, request, invokers, selected);
	}

	private Invoker<T> doselect(LoadBalance loadbalance, Request request, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {
		if (invokers == null || invokers.size() == 0)
			return null;
		if (invokers.size() == 1)
			return invokers.get(0);
		// 如果只有两个invoker，退化成轮循
		if (invokers.size() == 2 && selected != null && selected.size() > 0) 
			return selected.get(0) == invokers.get(0) ? invokers.get(1) : invokers.get(0);
		
		Invoker<T> invoker = loadbalance.select(invokers, getUrl(), request);
		// 如果 selected中包含（优先判断）
		if ((selected != null && selected.contains(invoker)) || (!invoker.isAvailable() && getUrl() != null)) {
			try {
				Invoker<T> rinvoker = reselect(loadbalance, request, invokers, selected);
				if (rinvoker != null) {
					invoker = rinvoker;
				} else {
					// 看下第一次选的位置，如果不是最后，选+1位置.
					int index = invokers.indexOf(invoker);
					try {
						// 最后在避免碰撞
						invoker = index < invokers.size() - 1 ? invokers.get(index + 1) : invoker;
					} catch (Exception e) {
						logger.warn(e.getMessage() + " may because invokers list dynamic change, ignore.", e);
					}
				}
			} catch (Throwable t) {
				logger.error("clustor relselect fail reason is :" + t.getMessage() + " if can not slove ,you can set cluster.availablecheck=false in url", t);
			}
		}
		return invoker;
	}

	/**
	 * 重选，先从非selected的列表中选择，没有在从selected列表中选择.
	 *
	 * @param loadbalance
	 * @param invocation
	 * @param invokers
	 * @param selected
	 * @return
	 * @throws RpcException
	 */
	private Invoker<T> reselect(LoadBalance loadbalance, Request request, List<Invoker<T>> invokers, List<Invoker<T>> selected)
			throws RpcException {
		// 预先分配一个，这个列表是一定会用到的.
		List<Invoker<T>> reselectInvokers = new ArrayList<>(invokers.size() > 1 ? (invokers.size() - 1) : invokers.size());

		// 选全部非select
		for (Invoker<T> invoker : invokers) {
			if (selected == null || !selected.contains(invoker)) {
				reselectInvokers.add(invoker);
			}
		}
		if (reselectInvokers.size() > 0) {
			return loadbalance.select(reselectInvokers, getUrl(), request);
		}
		// 最后从select中选可用的.
		{
			if (selected != null) {
				for (Invoker<T> invoker : selected) {
					if ((invoker.isAvailable()) // 优先选available
							&& !reselectInvokers.contains(invoker)) {
						reselectInvokers.add(invoker);
					}
				}
			}
			if (reselectInvokers.size() > 0) {
				return loadbalance.select(reselectInvokers, getUrl(), request);
			}
		}
		return null;
	}
    
	@Override
	public Response invoke(Request request) throws RpcException {
        checkWhetherDestroyed();

        LoadBalance loadbalance = null;
        List<Invoker<T>> invokers = list(request);
        if (invokers != null && invokers.size() > 0) {
            loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(invokers.get(0).getUrl()
                    .getParameter(URLParamType.LOADBALANCE.getName(), URLParamType.LOADBALANCE.getValue()));
        } else {
            loadbalance = ExtensionLoader.getExtensionLoader(LoadBalance.class)
            		.getExtension(URLParamType.LOADBALANCE.getValue());
        }
        return doInvoke(request, invokers, loadbalance);
	}
	
    protected void checkWhetherDestroyed() {
        if (destroyed.get()) {
            throw new RpcException("Rpc cluster invoker for " + getInterface() + " on consumer " + NetUtils.getLocalHost()
                    + " is now destroyed! Can not invoke any more.");
        }
    }
    
    protected List<Invoker<T>> list(Request request) throws RpcException {
        List<Invoker<T>> invokers = directory.list(request);
        return invokers;
    }

    /**
     * 调用
     * @param request
     * @param invokers
     * @param loadbalance
     * @return
     * @throws RpcException
     */
    protected abstract Response doInvoke(Request request, List<Invoker<T>> invokers, 
    			LoadBalance loadbalance) throws RpcException;
    
    @Override
    public String toString() {
        return getInterface() + " -> " + getUrl().toString();
    }
    
}
