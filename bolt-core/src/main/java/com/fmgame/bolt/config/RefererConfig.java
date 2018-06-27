package com.fmgame.bolt.config;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.cluster.Cluster;
import com.fmgame.bolt.cluster.directory.RegistryDirectory;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.proxy.ProxyFactory;
import com.fmgame.bolt.proxy.RefererInvocationHandler;
import com.fmgame.bolt.registry.RegistryFactory;
import com.fmgame.bolt.registry.RegistryService;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.Protocol;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.rpc.protocol.ProtocolFilterWrapper;
import com.fmgame.bolt.utils.NetUtils;

/**
 * 引用配置
 * 
 * @author luowei
 * @date 2017年12月1日 下午7:10:38
 */
public class RefererConfig<T> extends AbstractRefererConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(RefererConfig.class);

	/** url地址列表 */
	private final List<URL> urls = new ArrayList<>();
	/** 集群类 */
	private final Cluster cluster;
	/** 注册工厂类 */
	private final RegistryFactory registryFactory;
	/** 服务接口名 */
	private String interfaceName;
	/** 服务接口定义 */
	private Class<T> interfaceClass;
	/** 接口实现类引用 */
	private T ref;
	/** 具体到方法的配置 */
	private List<MethodConfig> methods;
	/** 调用者 */
	private volatile Invoker<?> invoker;
	/** service用于注册的url，用于管理service注册的生命周期，url为regitry url，内部嵌套service url。 */
	private URL registryUrl;
	/** 是否已初始化 */
	private volatile boolean initialized;
	/** 是否已销毁 */
	private volatile boolean destroyed;

	public RefererConfig(RegistryConfig registry, ProtocolClusterConfig protocol, String interfaceName) {
		super(registry, protocol);

		this.interfaceName = interfaceName;
		this.registryUrl = loadRegistry();
		
		// 注册工厂类
		this.registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class)
				.getExtension(registryUrl.getProtocol());
		// 集群
		String protocolCluster = protocolConfig.getCluster();
		if (protocolCluster == null || protocolCluster.length() == 0) {
			protocolCluster = URLParamType.CLUSTER.getValue();
		}
		this.cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getExtension(protocolCluster);
	}
	
	protected URL loadRegistry() {
		String group = registry.getGroup();
		if (StringUtils.isBlank(group)) {
			group = URLParamType.GROUP.getValue();
		}
		String address = registry.getAddress();
        if (StringUtils.isBlank(address)) {	
            address = NetUtils.getLocalHost();
        }
        Map<String, String> params = new HashMap<>();
        params.put(URLParamType.GROUP.getName(), group);
        params.put(URLParamType.ADDRESS.getName(), address);
        appendAttributes(params, registry);
        
        return new URL(registry.getName(), address, registry.getPort(), RegistryService.class.getName(), params);
	}

	public T getRef() {
		if (ref == null) {
			initRef();
		}
		return ref;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized void initRef() {
		if (initialized) {
			return;
		}
		initialized = true;

		try {
			interfaceClass = (Class) Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		checkInterfaceAndMethods(interfaceClass, methods);
		
		String protocolName = protocolConfig.getName();
		if (protocolName == null || protocolName.length() == 0) {
			protocolName = URLParamType.PROTOCOL.getValue();
		}
		String group = protocolConfig.getGroup();
		if (StringUtils.isBlank(group)) {
			group = URLParamType.GROUP.getValue();
		}

		Map<String, String> params = new HashMap<>();
		params.put(URLParamType.GROUP.getName(), group);
		appendAttributes(params, protocolConfig);
		appendAttributes(params, this);
		
		if (protocolConfig.getClusterAddress() != null && !"".equals(protocolConfig.getClusterAddress())) {
			String[] us = Constants.SEMICOLON_SPLIT_PATTERN.split(protocolConfig.getClusterAddress());
			for (String u : us) {
				InetSocketAddress address = NetUtils.toAddress(u);
				URL refUrl = new URL(protocolName, address.getHostName(), address.getPort(),
						interfaceClass.getName(), params);
				urls.add(refUrl);
			}
		} else {
			String hostAddress = protocolConfig.getHost();
			if (StringUtils.isBlank(hostAddress)) {
				hostAddress = NetUtils.getLocalHost();
			}
			URL refUrl = new URL(protocolName, hostAddress, protocolConfig.getPort(), interfaceClass.getName(), params);
			urls.add(refUrl);
		}

		Protocol orgProtocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName);
		// 利用protocol decorator来增加filter特性
		Protocol protocol = new ProtocolFilterWrapper(orgProtocol);

		List<Invoker<?>> invokers = new ArrayList<>();
		for (URL url : urls) {
			invokers.add(protocol.refer(interfaceClass, url));
		}
		
		URL refUrl = new URL(protocolName, NetUtils.LOCALHOST, Constants.DEFAULT_INT_VALUE, interfaceClass.getName(), params);
		RegistryDirectory directory = new RegistryDirectory(interfaceClass, refUrl, invokers);
		directory.setRegistry(registryFactory.getRegistry(registryUrl));
		directory.setProtocol(protocol);
		directory.subscribe(refUrl);
		
		invoker = cluster.join(directory);
		ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension("jdk");
		ref = proxyFactory.getProxy(interfaceClass, new RefererInvocationHandler(interfaceClass, invoker));
	}

	/**
	 * 检测接口和方法
	 * 
	 * @param interfaceClass
	 * @param methods
	 */
	private void checkInterfaceAndMethods(Class<?> interfaceClass, List<MethodConfig> methods) {
		// 接口不能为空
		if (interfaceClass == null) {
			throw new IllegalStateException("interface not allow null!");
		}
		// 检查接口类型必需为接口
		if (!interfaceClass.isInterface()) {
			throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
		}
		// 检查方法是否在接口中存在
		if (methods != null && methods.size() > 0) {
			for (MethodConfig methodBean : methods) {
				String methodName = methodBean.getName();
				if (methodName == null || methodName.length() == 0) {
					throw new IllegalStateException("name attribute is required! Please check: interface=" + interfaceClass.getName());
				}
				boolean hasMethod = false;
				for (java.lang.reflect.Method method : interfaceClass.getMethods()) {
					if (method.getName().equals(methodName)) {
						hasMethod = true;
						break;
					}
				}
				if (!hasMethod) {
					throw new IllegalStateException("The interface " + interfaceClass.getName() + " not found method " + methodName);
				}
			}
		}
	}

	public synchronized void destroy() {
		if (ref == null) {
			return;
		}
		if (destroyed) {
			return;
		}
		destroyed = true;
		try {
			invoker.destroy();
		} catch (Throwable t) {
			logger.warn("Unexpected err when destroy invoker of RefererConfig(" + invoker + ").", t);
		}
		invoker = null;
		ref = null;
	}

}
