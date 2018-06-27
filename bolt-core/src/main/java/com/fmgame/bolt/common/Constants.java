package com.fmgame.bolt.common;

import java.util.regex.Pattern;

/**
 * 常量
 * 
 * @author luowei
 * @date 2017年10月18日 上午11:12:55
 */
public class Constants {
	
	/** 逗号分割 */
	public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");
	/** 分号分割 */
	public static final Pattern SEMICOLON_SPLIT_PATTERN = Pattern.compile("\\s*[;]+\\s*");
	/** 默认编码 */
	public static final String DEFAULT_CHARACTER = "UTF-8";
	/** 默认值毫秒，避免重新计算. */
    public static final int DEFAULT_SERVER_SHUTDOWN_TIMEOUT = 10000;
    /** 任意地址 */
    public static final String ANYHOST_VALUE = "0.0.0.0";
    
    public static final String REGISTRY_PROTOCOL_LOCAL = "local";
    public static final String REGISTRY_PROTOCOL_DIRECT = "direct";
    public static final int DEFAULT_INT_VALUE = 0;
    
    /**
     * rpc包前缀
     */
    public static final String PACKAGE_PREFIX = "com";
    
    /**
     * heartbeat constants start
     */
    public static final int HEARTBEAT_PERIOD = 500;
    public static final String HEARTBEAT_INTERFACE_NAME = "bolt.rpc.heartbeat";
    public static final String HEARTBEAT_METHOD_NAME = "heartbeat";
    public static final String HHEARTBEAT_PARAM = "void";
    
	
	/** 协议分隔符 */
    public static final String PROTOCOL_SEPARATOR = "://";
    /** url path 分隔符 */
    public static final String PATH_SEPARATOR = "/";
    /** 默认组名称 */
	public static final String DEFAULT_NAME = "bolt";
	/** 默认协议 */
	public static final String DEFAULT_PROTOL = "default";
	
    public static final String PROVIDER = "provider";
    public static final String CONSUMER = "consumer";
	/** 协议注册标识 */
    public static final String REGISTRY_PROTOCOL = "registry";
    public static final String SERVICE_FILTER_KEY = "service.filter";
    public static final String REFERENCE_FILTER_KEY = "reference.filter";
    
	/**
	 * client constants start
	 */

	// 重连key
	public static final String SEND_RECONNECT_KEY = "send.reconnect";
	// 检测key
	public static final String CHECK_KEY = "check";
	
    // 方法是否异步调用stuff
    public static final String ASYNC_SUFFIX = "Async";
    // 方法调用是否单向的,无返回值
    public static final String ONE_WAY = "Oneway";
    
    // message 标识
    public static final byte FLAG_REQUEST = 0x00;
    public static final byte FLAG_RESPONSE = 0x01;

    /**
     * CODEC constants start
     **/
    
    // netty header length
	public static final int CODEC_HEADER = 16;
	// netty codec
	public static final short CODEC_MAGIC_TYPE = (short) 0x22B8;
	
}
