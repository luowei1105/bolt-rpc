package com.fmgame.bolt.config;

/**
 * 抽象引用配置
 * 
 * @author luowei
 * @date 2018年4月3日 上午10:56:25
 */
public abstract class AbstractRefererConfig extends AbstractInterfaceConfig {
	
    /** 暴露使用的协议.*/
    protected ProtocolClusterConfig protocolConfig;

	public AbstractRefererConfig(RegistryConfig registry, ProtocolClusterConfig protocolConfig) {
		super(registry);
		this.protocolConfig = protocolConfig;
	}

	public ProtocolClusterConfig getProtocolConfig() {
		return protocolConfig;
	}

	public void setProtocolConfig(ProtocolClusterConfig protocolConfig) {
		this.protocolConfig = protocolConfig;
	}

}
