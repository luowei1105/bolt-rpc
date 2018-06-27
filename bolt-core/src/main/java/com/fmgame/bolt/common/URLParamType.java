package com.fmgame.bolt.common;

/**
 * url参数类型
 * 
 * @author luowei
 * @date 2017年10月17日 下午5:37:27
 */
public enum URLParamType {

    /** 请求超时 */
    REQUEST_TIMEOUT("requestTimeout", 3000),
	/** 连接超时 */
	CONNECT_TIMEOUT("connectTimeout", 1000), 
	/** 会话超时 */
	SESSION_TIMEOUT("sessionTimeout", 1000), 
	/** 是否重连 */
	RECONNECT("reconnect", true),
	/** 请求重连 */
	REQUEST_RECONNECT("requestReconnect", false),
	/** 重连周期 */
	RECONNECT_PERIOD("reconnectWaringPeriod", 2000),
	/** 重连警告间隔 */
	RECONNECT_WARING_PERIOD("reconnectWaringPeriod", 30),
	/** 连接数 */
	CONNECTIONS("connections", 1),
	/** 心跳处理 */
	HEARTBEAT("heartbeat", 60 * 1000),
	/** 心跳超时 */
	HEARTBEAT_TIMEOUT("heartbeatTimeout", 3 * 60 * 1000),
	/** 所属组 */
	GROUP("group", "bolt-rpc"),
    /** 协议 */
	PROTOCOL("protocol", "default"),
    /** 传输 */
	TRANSPORTER("transporter", "netty"),
	/** 加解码 */
	CODEC("codec", "default"),
	/** 本地注册工厂 */
	LOCAL_REGISTRY_FACTORY("localRegistryFactory", "local"),
	/** 注册失败后重试周期,默认5秒进行一次重试 */
	REGISTRY_RETRY_PERIOD("retryPeriod", 5 * 1000),
	/** io线程数 */
	IO_THREADS("ioThreads", Math.min(Runtime.getRuntime().availableProcessors() + 1, 32)),
	/** 线程池类型 */
	THREAD_POOL("threadPool", "cached"),
	/** worker线程名 */
	WORKER_THREAD_NAME("workerThreadName", "bolt-worker-excutor"),
	/** worker核心线程数 */
	WORKER_CORE_THREADS("workerCoreThreads", 0),
	/** worker线程数 */
	WORKER_THREADS("workerThreads", 200),
	/** 线程队列 */
	WORKER_THREAD_QUEUES("workerThreadQueues", 0),
	/** 线程生命周期,默认1分钟 */
	WORKER_THREAD_ALIVE("workerThreadAlive", 60 * 1000),
	/** 连接地址 */
	ADDRESS("address", "127.0.0.1"),
	/** 集群配置 */
	CLUSTER("cluster", "failfast"),
	/** 方法调用重试次数 */
	RETRIES("retries", 2),
	/** 负载均衡 */
	LOADBALANCE("loadbalance", "random"),

	;

	/** 参数名 */
	private String name;
	
	/** 
	 * 多类型参数值 
	 * 
	*/
	private String value;
	private long longValue;
	private int intValue;
	private boolean boolValue;

	private URLParamType(String name, String value) {
		this.name = name;
		this.value = value;
	}

	private URLParamType(String name, long longValue) {
		this.name = name;
		this.value = String.valueOf(longValue);
		this.longValue = longValue;
	}

	private URLParamType(String name, int intValue) {
		this.name = name;
		this.value = String.valueOf(intValue);
		this.intValue = intValue;
	}

	private URLParamType(String name, boolean boolValue) {
		this.name = name;
		this.value = String.valueOf(boolValue);
		this.boolValue = boolValue;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public int getIntValue() {
		return intValue;
	}

	public long getLongValue() {
		return longValue;
	}

	public boolean getBooleanValue() {
		return boolValue;
	}
}
