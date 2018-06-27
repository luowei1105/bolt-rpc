package com.fmgame.bolt.config;

import java.util.List;

/**
 * 抽象服务配置
 * 
 * @author luowei
 * @date 2018年4月3日 上午10:54:21
 */
public abstract class AbstractServiceConfig extends AbstractInterfaceConfig {
	
    /** 暴露使用的协议.*/
    protected List<ProtocolConfig> protocols;

	public AbstractServiceConfig(RegistryConfig registry, List<ProtocolConfig> protocols) {
		super(registry);
		this.protocols = protocols;
	}
	
	public List<ProtocolConfig> getProtocol() {
		return protocols;
	}

	public void setProtocol(List<ProtocolConfig> protocols) {
		this.protocols = protocols;
	}

}
