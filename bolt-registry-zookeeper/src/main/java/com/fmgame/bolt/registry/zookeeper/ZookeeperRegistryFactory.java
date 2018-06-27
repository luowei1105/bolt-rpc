package com.fmgame.bolt.registry.zookeeper;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.registry.AbstractRegistryFactory;
import com.fmgame.bolt.registry.Registry;
import com.fmgame.bolt.rpc.URL;

/**
 * zookeeper注册
 * 
 * @author luowei
 * @date 2018年1月26日 上午10:43:31
 */
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {

	@Override
	protected Registry createRegistry(URL url) {
        try {
            int connectTimeout = url.getIntParameter(URLParamType.CONNECT_TIMEOUT.getName(),
            		URLParamType.CONNECT_TIMEOUT.getIntValue());
            int sessionTimeout = url.getIntParameter(URLParamType.SESSION_TIMEOUT.getName(),
            		URLParamType.SESSION_TIMEOUT.getIntValue());
            ZkClient zkClient = new ZkClient(url.getParameter("address"), sessionTimeout, connectTimeout);
            return new ZookeeperRegistry(url, zkClient);
        } catch (ZkException e) {
            throw e;
        }
	}

}
