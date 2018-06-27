package com.fmgame.bolt.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.StringUtils;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.registry.Registry;
import com.fmgame.bolt.registry.RegistryFactory;
import com.fmgame.bolt.registry.RegistryService;
import com.fmgame.bolt.rpc.Exporter;
import com.fmgame.bolt.rpc.Protocol;
import com.fmgame.bolt.rpc.Provider;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.rpc.protocol.DefaultProvider;
import com.fmgame.bolt.rpc.protocol.ProtocolFilterWrapper;
import com.fmgame.bolt.utils.NetUtils;

/**
 * 服务配置
 * 
 * @author luowei
 * @date 2017年11月30日 下午1:45:53
 */
public class ServiceConfig<T> extends AbstractServiceConfig {
	
	/** 服务接口名 */
    private String interfaceName;
	/** 服务接口定义 */
	private Class<T> interfaceClass;
	/** 接口实现类引用 */
	private T ref;
	/** 具体到方法的配置 */
	private List<MethodConfig> methods;
	/** service用于注册的url，用于管理service注册的生命周期，url为regitry url，内部嵌套service url。 */
	private URL registryUrl;
	/** 服务暴露 */
	private List<Exporter<T>> exporters = new CopyOnWriteArrayList<>();
	/** 是否已暴露服务 */
	private volatile boolean exported;

	public ServiceConfig(RegistryConfig registry, List<ProtocolConfig> protocols, String interfaceName, T ref) {
		super(registry, protocols);

		this.interfaceName = interfaceName;
		this.ref = ref;
		this.registryUrl = loadRegistry();
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
		return ref;
	}

	public List<MethodConfig> getMethods() {
		return methods;
	}

	public void setMethods(List<MethodConfig> methods) {
		this.methods = methods;
	}
	
    public List<Exporter<T>> getExporters() {
        return Collections.unmodifiableList(exporters);
    }

	public URL getRegistryUrl() {
		return registryUrl;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void export() {
		if (exported) {
			return;
		}
		exported = true;

		try {
			interfaceClass = (Class<T>) Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		checkInterfaceAndMethods(interfaceClass, methods);
		checkRef();

		for (ProtocolConfig protocolConfig : protocols) {
			doExportUrl(protocolConfig);
		}
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
	
	/**
	 * 检测实现类
	 */
    private void checkRef() {
        // 检查引用不为空，并且引用必需实现接口
        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
        if (!interfaceClass.isInstance(ref)) {
            throw new IllegalStateException("The class "
                    + ref.getClass().getName() + " unimplemented interface "
                    + interfaceClass + "!");
        }
    }
    
    /**
     * @param protocolConfig 
     */
	private void doExportUrl(ProtocolConfig protocolConfig) {
        String protocolName = protocolConfig.getName();
        if (protocolName == null || protocolName.length() == 0) {
            protocolName = URLParamType.PROTOCOL.getValue();
        }
        String hostAddress = protocolConfig.getHost();
        if (StringUtils.isBlank(hostAddress)) {
            hostAddress = NetUtils.getLocalHost();
        }
		String group = protocolConfig.getGroup();
		if (StringUtils.isBlank(group)) {
			group = URLParamType.GROUP.getValue();
		}
        
        Map<String, String> params = new HashMap<>();
        params.put(URLParamType.GROUP.getName(), group);
        params.put(URLParamType.ADDRESS.getName(), hostAddress);
        appendAttributes(params, protocolConfig);

        URL serviceUrl = new URL(protocolName, hostAddress, protocolConfig.getPort(), interfaceClass.getName(), params);
        exporters.add(export(interfaceClass, ref, registryUrl, serviceUrl));
	}
	
	private Exporter<T> export(Class<T> interfaceClass, T ref, URL registryUrl, URL serviceUrl) {
		// export service
		String protocolName = serviceUrl.getParameter(URLParamType.PROTOCOL.getName(), URLParamType.PROTOCOL.getValue());
		Protocol orgProtocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName);
		
		// provider
		Provider<T> provider = new DefaultProvider<T>(serviceUrl, interfaceClass, ref);

		// 利用protocol decorator来增加filter特性
		Protocol protocol = new ProtocolFilterWrapper(orgProtocol);
		Exporter<T> exporter = protocol.export(provider);

		// register service
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class)
        		.getExtension(registryUrl.getProtocol());
        Registry registry = registryFactory.getRegistry(registryUrl);
        registry.register(serviceUrl);

		return exporter;
	}
	
	public synchronized void unexport() {
		if (!exported)
			return;
		
		// 取消注册
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class)
        		.getExtension(registryUrl.getProtocol());
        Registry registry = registryFactory.getRegistry(registryUrl);
        registry.destroy();
		// 取消服务
        for (Exporter<T> exporter : exporters) {
            exporter.unexport();
        }
	}

}
