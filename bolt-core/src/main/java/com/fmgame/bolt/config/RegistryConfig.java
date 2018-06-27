package com.fmgame.bolt.config;

/**
 * 注册配置
 * 
 * @author luowei
 * @date 2017年11月30日 上午10:46:42
 */
public class RegistryConfig extends AbstractConfig {

    /** 所属组 **/
    private String group;
	/** 注册名 */
    private String name;
    /** 注册中心地址 */
    private String address;
    /** 注册中心端口 */
    private Integer port;
    /** 注册中心请求超时时间(毫秒) */
    private Integer requestTimeout;
    /** 注册中心连接超时时间(毫秒) */
    private Integer connectTimeout;
    /** 注册中心会话超时时间(毫秒) */
    private Integer sessionTimeout;
    /** 失败后重试的时间间隔 */
    private Integer retryPeriod;

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

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
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

	public Integer getSessionTimeout() {
		return sessionTimeout;
	}

	public void setSessionTimeout(Integer sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public Integer getRetryPeriod() {
		return retryPeriod;
	}

	public void setRetryPeriod(Integer retryPeriod) {
		this.retryPeriod = retryPeriod;
	}
    
}
