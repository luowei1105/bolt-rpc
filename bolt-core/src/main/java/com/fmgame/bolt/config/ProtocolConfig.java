package com.fmgame.bolt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.rpc.Protocol;

/**
 * 协议配置
 * 
 * @author luowei
 * @date 2017年11月30日 上午11:06:50
 */
public class ProtocolConfig extends AbstractConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(ProtocolConfig.class);
	
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (logger.isInfoEnabled()) {
                    logger.info("Run shutdown hook now.");
                }
                ProtocolConfig.destroyAll();
            }
        }, "bolt-ShutdownHook"));
    }

    /** 所属组 **/
    private String group;
	/** 协议框架名 */
	private String name;
    /** 服务IP地址(多网卡时使用) */
    private String host;
	/** 对外提供端口 */
	private Integer port;
	/** 传输协议*/
	private String transporter;
    /** 协议编码 */
    private String codec;
    /** 请求超时时间(毫秒) */
    private Integer requestTimeout;
    /** 连接超时时间(毫秒) */
    private Integer connectTimeout;
    /** 是否重连 */
    private boolean reconnect;
    /** IO线程池大小 */
    private Integer ioThreads;
    /** 工作pool线程类型 */
    private String threadPool;
    /** 工作pool核心线程数 */
    private Integer workerCoreThreads;
    /** 工作pool线程最大数 */
    private Integer workerThreads;
    /** 工作pool线程队列长度 */
    private Integer workerThreadQueues;
    /** 工作pool线程存活生命周期 */
    private Integer workerThreadAlive;
    /** 请求响应包的最大长度限制 */
    private Integer maxContentLength;

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getTransporter() {
		return transporter;
	}

	public void setTransporter(String transporter) {
		this.transporter = transporter;
	}

	public String getCodec() {
		return codec;
	}

	public void setCodec(String codec) {
		this.codec = codec;
	}

	public Integer getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(Integer requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public Integer getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public boolean isReconnect() {
		return reconnect;
	}

	public void setReconnect(boolean reconnect) {
		this.reconnect = reconnect;
	}

	public Integer getIoThreads() {
		return ioThreads;
	}

	public void setIoThreads(Integer ioThreads) {
		this.ioThreads = ioThreads;
	}

	public String getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(String threadPool) {
		this.threadPool = threadPool;
	}

	public Integer getWorkerCoreThreads() {
		return workerCoreThreads;
	}

	public void setWorkerCoreThreads(Integer workerCoreThreads) {
		this.workerCoreThreads = workerCoreThreads;
	}

	public Integer getWorkerThreads() {
		return workerThreads;
	}

	public void setWorkerThreads(Integer workerThreads) {
		this.workerThreads = workerThreads;
	}

	public Integer getWorkerThreadQueues() {
		return workerThreadQueues;
	}

	public void setWorkerThreadQueues(Integer workerThreadQueues) {
		this.workerThreadQueues = workerThreadQueues;
	}

	public Integer getWorkerThreadAlive() {
		return workerThreadAlive;
	}

	public void setWorkerThreadAlive(Integer workerThreadAlive) {
		this.workerThreadAlive = workerThreadAlive;
	}

	public Integer getMaxContentLength() {
		return maxContentLength;
	}

	public void setMaxContentLength(Integer maxContentLength) {
		this.maxContentLength = maxContentLength;
	}
	
	protected static void destroyAll() {
		ExtensionLoader<Protocol> loader = ExtensionLoader.getExtensionLoader(Protocol.class);
		for (String protocolName : loader.getLoadedExtensions()) {
			try {
				Protocol protocol = loader.getExtension(protocolName);
				if (protocol != null) {
					protocol.destroy();
				}
			} catch (Throwable t) {
				logger.warn(t.getMessage(), t);
			}
		}
	}

	@Override
	public String toString() {
		return "ProtocolConfig [id=" + id + ", group=" + group + ", name=" + name + ", host=" + host + ", port=" + port + ", transporter=" + transporter
				+ ", codec=" + codec + ", requestTimeout=" + requestTimeout + ", connectTimeout=" + connectTimeout + ", reconnect=" + reconnect + ", ioThreads="
				+ ioThreads + ", workerThreads=" + workerThreads + ", maxContentLength=" + maxContentLength + "]";
	}
    
}
