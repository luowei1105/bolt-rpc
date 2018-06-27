package com.fmgame.bolt.config;

/**
 * 协议集群配置
 * 
 * @author luowei
 * @date 2018年4月3日 上午11:01:24
 */
public class ProtocolClusterConfig extends ProtocolConfig {
	
	/** 集群地址 */
	private String clusterAddress;
	/** 采用哪种cluster 的实现 */
    private String cluster;
    /** 负载均衡器 */
    private String loadbalance;
    /** 失败重试策略时,重试次数 */
    private int retries;
    
	public String getClusterAddress() {
		return clusterAddress;
	}

	public void setClusterAddress(String clusterAddress) {
		this.clusterAddress = clusterAddress;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	public String getLoadbalance() {
		return loadbalance;
	}

	public void setLoadbalance(String loadbalance) {
		this.loadbalance = loadbalance;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}
    
}
