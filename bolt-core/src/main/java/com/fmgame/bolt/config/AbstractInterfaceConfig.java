package com.fmgame.bolt.config;

/**
 * 抽象接口配置
 * 
 * @author luowei
 * @date 2017年11月30日 下午1:32:51
 */
public abstract class AbstractInterfaceConfig extends AbstractConfig {

	/** 注册中心的配置列表 */
	protected RegistryConfig registry;
    /** 是否共享 channel */
    private Boolean shareChannel;

	public AbstractInterfaceConfig(RegistryConfig registry) {
		this.registry = registry;
	}

	public RegistryConfig getRegistry() {
		return registry;
	}

	public void setRegistry(RegistryConfig registry) {
		this.registry = registry;
	}

	public Boolean isShareChannel() {
		return shareChannel;
	}

	public void setShareChannel(Boolean shareChannel) {
		this.shareChannel = shareChannel;
	}
    
}
