package com.fmgame.bolt.cluster.directory;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.cluster.AbstractDirectory;
import com.fmgame.bolt.registry.NotifyListener;
import com.fmgame.bolt.registry.Registry;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.Protocol;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.NetUtils;
import com.fmgame.bolt.utils.ProtocolUtils;

/**
 * 连接注册服，可根据注册服来更新调用列表的目录对象
 * 
 * @author luowei
 * @date 2018年4月3日 下午3:56:28
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

	private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);

	/** 服务提供类型 */
	private final Class<T> serviceType;
	private final String serviceKey;
	private volatile List<Invoker<T>> invokers;
	/** 注册 */
	private Registry registry;
	/** 协议 */
	private Protocol protocol;
	/** 服务是否被禁用 */
    private volatile boolean forbidden = false;

	public RegistryDirectory(Class<T> serviceType, URL url, List<Invoker<T>> invokers) {
		super(url);
		if (serviceType == null)
			throw new IllegalArgumentException("service type is null.");

		this.serviceType = serviceType;
		this.serviceKey = ProtocolUtils.getServiceKey(url);
		this.invokers = invokers;
	}

	public Registry getRegistry() {
		return registry;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public void subscribe(URL url) {
		registry.subscribe(url, this);
	}

	@Override
	public Class<T> getInterface() {
		return serviceType;
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public void destroy() {
		if (isDestroyed()) {
			return;
		}
		super.destroy(); // 必须在unsubscribe之后执行
		try {
			destroyAllInvokers();
		} catch (Throwable t) {
			logger.warn("Failed to destroy service " + serviceKey, t);
		}
	}

	/**
	 * 关闭所有Invoker
	 */
	private void destroyAllInvokers() {
		this.destroyAllInvokers(invokers);
	}
	
	/**
	 * 关闭指定调用列表
	 * @param invokers
	 */
	private void destroyAllInvokers(List<Invoker<T>> invokers) {
		invokers.forEach(invoker -> {
			try {
				invoker.destroy();
			} catch (Throwable t) {
				logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
			}
		});
		invokers.clear();
	}

	@Override
	protected List<Invoker<T>> doList(Request request) throws RpcException {
        if (forbidden) {
            // 1. 没有服务提供者 2. 服务提供者被禁用
            throw new RpcException(RpcException.FORBIDDEN_EXCEPTION,
                "No provider available from registry " + getUrl().getAddress() + " for service " 
                	+ this.serviceKey + " on consumer " +  NetUtils.getLocalHost()
                    + ", may be providers disabled or not registered");
        }
        
		return invokers;
	}

	/**
	 * 根据invokerUrl列表转换未invoker列表。转换规则如下：
	 * 1. 如果url已经被转换为invoker,则不再重新引用。直接从缓存中获取。
	 * 2.  
	 */
	@Override
	public void notify(List<URL> invokerUrls) {
		if (invokerUrls == null || invokerUrls.isEmpty()) {
			this.forbidden = true; // 禁止访问
			destroyAllInvokers();
		} else {
			this.forbidden = false; // 允许访问
			// 新的调用列表
			List<Invoker<T>> newInvokers = new ArrayList<>(invokers);
			invokerUrls.stream()
				.filter(invokerUrl -> invokers.stream().noneMatch(invoker -> invokerUrl.getAddress().equals(invoker.getUrl().getAddress())))
				.forEach(invokerUrl -> {
					URL tmpURL = this.getUrl().createCopy();
					tmpURL.setHost(invokerUrl.getHost());
					tmpURL.setPort(invokerUrl.getPort());
					newInvokers.add(protocol.refer(serviceType, tmpURL));
				});
			// 获取需要销毁调用列表
			List<Invoker<T>> destoryInvokers = new ArrayList<>();
			invokers.stream()
				.filter(invoker -> invokerUrls.stream().noneMatch(invokerUrl -> invoker.getUrl().getAddress().equals(invokerUrl.getAddress())))
				.forEach(invoker -> {
					destoryInvokers.add(invoker);
					newInvokers.remove(invoker);
				});
			// 设置新的调用列表
			this.invokers = newInvokers;
			// 销毁调用列表
			this.destroyAllInvokers(destoryInvokers);
		}
	}

}
