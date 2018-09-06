package com.fmgame.bolt.config;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.utils.NetUtils;

/**
 * 抽象服务提供初始类
 * 
 * @author luowei
 * @date 2017年12月1日 上午11:40:14
 */
public abstract class AbstractProviderHandler {
	
	public static final String DEFAULT_FILE_NAME = "bolt_rpc_provider.xml";
	
	/** 指定xml文件名 */ 
	protected final String fileName;
	/** 服务配置列表 */
	protected List<ServiceConfig<?>> configs = new ArrayList<>();
	
	public AbstractProviderHandler(String fileName) {
		this.fileName = fileName;
	}
	
	public AbstractProviderHandler() {
		this(DEFAULT_FILE_NAME);
	}
	
	public void initialize() {
 		try {
			Element root = getRootElement();
			
			// 注册配置
			RegistryConfig registryConfig = registryConfig(root.element("registry"));
			// 协议配置
			Map<String, ProtocolConfig> protocolsConfig = protocolsConfig(root.elementIterator("protocol"));
			exportService(registryConfig, protocolsConfig, root.elementIterator("service"));
		} catch (DocumentException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public void release() {
		configs.forEach(action -> action.unexport());
	}
	
	protected abstract <T extends Object> T ref(String refName);
	
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
	 * 生成注册配置
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
	protected Map<String, ProtocolConfig> protocolsConfig(Iterator<Element> it) {
		Map<String, ProtocolConfig> protocolsConfig = new HashMap<>();
		for ( ; it.hasNext(); ) {
			ProtocolConfig protocolConfig = protocolConfig(it.next());
			protocolsConfig.put(protocolConfig.getId(), protocolConfig);
		}
		return protocolsConfig;
	}
	
	/**
	 * 生成协议配置
	 * @param protocolEle
	 * @return
	 */
	protected ProtocolConfig protocolConfig(Element protocolEle) {
		ProtocolConfig config = new ProtocolConfig();
		config.setId(protocolEle.attributeValue("id"));
		config.setName(protocolEle.attributeValue("name"));
		
		// 组
		String group = protocolEle.attributeValue("group");
		config.setGroup(group != null ? group : URLParamType.GROUP.getValue());
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
	    // 请求超时时间(毫秒)
		String requestTimeout = protocolEle.attributeValue("requestTimeout");
		if (requestTimeout != null) {
			config.setRequestTimeout(Integer.valueOf(requestTimeout));
		}
	    // 连接超时时间(毫秒)
		String connectTimeout = protocolEle.attributeValue("connectTimeout");
		if (connectTimeout != null) {
			config.setConnectTimeout(Integer.valueOf(connectTimeout));
		}
	    // 是否重连
		String reconnect = protocolEle.attributeValue("reconnect");
		if (reconnect != null) {
			config.setReconnect(Boolean.valueOf(reconnect));
		}
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

		return config;
	}
	
	protected void exportService(RegistryConfig registryConfig, 
			Map<String, ProtocolConfig> protocolsConfig, Iterator<Element> it) {
		for (; it.hasNext();) {
			Element serviceEle = it.next();
			String id = serviceEle.attributeValue("id");
			String interfaceName = serviceEle.attributeValue("interface");
			String refName = serviceEle.attributeValue("impl");
			
			ServiceConfig<?> config = new ServiceConfig<>(registryConfig, 
					new ArrayList<>(protocolsConfig.values()), interfaceName, ref(refName));
			config.setId(id);
			config.export();
			configs.add(config);
		}
	}
	
}
