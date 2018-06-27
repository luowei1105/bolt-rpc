package com.fmgame.bolt.config;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.utils.NetUtils;
import com.fmgame.bolt.utils.ProtocolUtils;

/**
 * 抽象服务引用方提供类
 * 
 * @author luowei
 * @date 2017年12月1日 下午6:58:13
 */
public abstract class AbstractRefererHandler {
	
	private static final String DEFAULT_FILE_NAME = "bolt_rpc_referer.xml";
	
	/** 指定xml文件名 */ 
	protected final String fileName;
	/** 引用配置列表 */
	private Map<String, RefererConfig<?>> configMap = new ConcurrentHashMap<>();
	
	public AbstractRefererHandler(String fileName) {
		this.fileName = fileName;
	}
	
	public AbstractRefererHandler() {
		this(DEFAULT_FILE_NAME);
	}
	
	public void initialize() {
 		try {
			Element root = getRootElement();

			// 注册配置
			RegistryConfig registryConfig = registryConfig(root.element("registry"));
			// 协议配置
			Map<String, ProtocolClusterConfig> protocolsConfig = protocolsConfig(root.elementIterator("protocol"));
			// 引用配置
			refersConfig(registryConfig, protocolsConfig, root.elementIterator("refer"));
		} catch (DocumentException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void release() {
		configMap.values().forEach(action -> action.destroy());
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Object> T get(String key, Class<T> clazz) {
		synchronized (key.intern()) {
			RefererConfig<?> config = getConfig(key);
			if (config == null)
				return null;
			return (T) config.getRef();
		}
	}
	
	public <T extends Object> T get(Class<T> clazz) {
		String key = ProtocolUtils.getServiceKey(URLParamType.GROUP.getValue(), clazz.getName());
		return get(key, clazz);
	}
	
	/**
	 * 获取xml文件root
	 * @return
	 * @throws DocumentException
	 */
	protected Element getRootElement() throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(fileName));
		return document.getRootElement(); 
	}
	
	/**
	 * 获取config配置
	 * @param key
	 * @param clazz
	 * @return
	 */
	protected <T extends Object> RefererConfig<?> getConfig(String key) {
		return configMap.get(key);
	}

	/**
	 * 获取注册配置
	 * @param registryEle
	 * @return
	 */
	protected RegistryConfig registryConfig(Element registryEle) {
		RegistryConfig config = new RegistryConfig();
		config.setId(registryEle.attributeValue("id"));
		config.setName(registryEle.attributeValue("name"));
		
		InetSocketAddress address = NetUtils.toAddress(registryEle.attributeValue("address"));
		config.setAddress(address.getHostName());
		config.setPort(address.getPort());
		
		String group = registryEle.attributeValue("group");
		if (group == null) {
			config.setGroup(group);
		}
		
		String requestTimeout = registryEle.attributeValue("requestTimeout");
		if (requestTimeout != null) {
			config.setRequestTimeout(Integer.valueOf(requestTimeout));
		}
		
		String connectTimeout = registryEle.attributeValue("connectTimeout");
		if (connectTimeout != null) {
			config.setConnectTimeout(Integer.valueOf(connectTimeout));
		}

		return config;
	}
	
	/**
	 * 引用配置
	 */
	protected Map<String, ProtocolClusterConfig> protocolsConfig(Iterator<Element> it) {
		Map<String, ProtocolClusterConfig> protocolsConfig = new HashMap<>();
		for ( ; it.hasNext(); ) {
			ProtocolClusterConfig protocolConfig = protocolConfig(it.next());
			protocolsConfig.put(protocolConfig.getId(), protocolConfig);
		}
		return protocolsConfig;
	}
	
	/**
	 * 获取协议配置
	 * @param protocolEle
	 * @return
	 */
	protected ProtocolClusterConfig protocolConfig(Element protocolEle) {
		ProtocolClusterConfig config = new ProtocolClusterConfig();
		config.setId(protocolEle.attributeValue("id"));
		config.setName(protocolEle.attributeValue("name"));
		
		// 组
		String group = protocolEle.attributeValue("group");
		config.setGroup(group != null ? group : URLParamType.GROUP.getValue());
		// host地址
		String host = protocolEle.attributeValue("host");
		config.setHost(host);
		// 端口
		String port = protocolEle.attributeValue("port");
		if (port != null) {
			config.setPort(Integer.valueOf(port));
		}
		// 传输协议
		String transporter = protocolEle.attributeValue("transporter");
		config.setTransporter(transporter);
		// 编解码
		String codec = protocolEle.attributeValue("codec");
		config.setCodec(codec);
		// io线程数
		String ioThreads = protocolEle.attributeValue("ioThreads");
		if (ioThreads != null) {
			config.setIoThreads(Integer.valueOf(ioThreads));
		}
		// 工作线程类型
		String threadPool = protocolEle.attributeValue("threadPool");
		if (threadPool != null) {
			config.setThreadPool(threadPool);
		}
		// 工作核心线程数
		String workerCoreThreads = protocolEle.attributeValue("workerCoreThreads");
		if (workerCoreThreads != null) {
			config.setWorkerCoreThreads(Integer.valueOf(workerCoreThreads));
		}
		// 工作线程数
		String workerThreads = protocolEle.attributeValue("workerThreads");
		if (workerThreads != null) {
			config.setWorkerThreads(Integer.valueOf(workerThreads));
		}
		// 工作线程队列长度
		String workerThreadQueues = protocolEle.attributeValue("workerThreadQueues");
		if (workerThreadQueues != null) {
			config.setWorkerThreadQueues(Integer.valueOf(workerThreadQueues));
		}
		// 工作线程存活生命周期
		String workerThreadAlive = protocolEle.attributeValue("workerThreadAlive");
		if (workerThreadAlive != null) {
			config.setWorkerThreadAlive(Integer.valueOf(workerThreadAlive));
		}
		
		// 集群
		String cluster = protocolEle.attributeValue("cluster");
		if (cluster != null) {
			config.setCluster(cluster);
		}
		String clusterAddress = protocolEle.attributeValue("clusterAddress");
		if (clusterAddress != null) {
			config.setClusterAddress(clusterAddress);
		}
		String loadbalance = protocolEle.attributeValue("loadbalance");
		if (loadbalance != null) {
			config.setLoadbalance(loadbalance);
		}
		String retries = protocolEle.attributeValue("retries");
		if (retries != null) {
			config.setRetries(Integer.valueOf(retries));
		}

		return config;
	}
	
	/**
	 * 引用配置
	 * @param registryConfig
	 * @param protocolConfigs
	 * @param it
	 */
	protected void refersConfig(RegistryConfig registryConfig, Map<String, ProtocolClusterConfig> protocolConfigs, Iterator<Element> it) {
		for ( ; it.hasNext(); ) {
			Element refererEle = it.next();
			String interfaceName = refererEle.attributeValue("interface");
			String protocol = refererEle.attributeValue("protocol");
	
			ProtocolClusterConfig protocolConfig = protocolConfigs.get(protocol);
			String key = ProtocolUtils.getServiceKey(protocolConfig.getGroup(), interfaceName);
			referConfig(key, registryConfig, protocolConfig, interfaceName);
		}
	}
	
	/**
	 * 注册引用
	 * @param registryConfig
	 * @param protocolConfig
	 * @param refererEle
	 */
	protected void referConfig(String key, RegistryConfig registryConfig, 
			ProtocolClusterConfig protocolConfig, String interfaceName) {
		RefererConfig<?> config = new RefererConfig<>(registryConfig, protocolConfig, interfaceName);
		configMap.put(key, config);
	}
	
}
