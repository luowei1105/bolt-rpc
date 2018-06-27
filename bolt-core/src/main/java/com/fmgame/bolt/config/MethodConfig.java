package com.fmgame.bolt.config;

/**
 * 方法配置
 * 
 * @author luowei
 * @date 2017年11月30日 上午11:08:50
 */
public class MethodConfig extends AbstractConfig {

    /** 方法名 */
    private String name;
    /** 参数类型（逗号分隔）*/
    private String argumentTypes;
    /** 超时时间 */
    private Integer requestTimeout;
    /** 失败重试次数（默认为0，不重试）*/
    private Integer retries;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getArgumentTypes() {
		return argumentTypes;
	}

	public void setArgumentTypes(String argumentTypes) {
		this.argumentTypes = argumentTypes;
	}

	public Integer getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(Integer requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public Integer getRetries() {
		return retries;
	}

	public void setRetries(Integer retries) {
		this.retries = retries;
	}
    
}
