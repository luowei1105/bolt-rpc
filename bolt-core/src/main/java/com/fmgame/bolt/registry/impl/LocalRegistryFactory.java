package com.fmgame.bolt.registry.impl;

import com.fmgame.bolt.registry.AbstractRegistryFactory;
import com.fmgame.bolt.registry.Registry;
import com.fmgame.bolt.rpc.URL;

/**
 * 本地注册工厂类
 * 
 * @author luowei
 * @date 2017年11月27日 上午11:06:07
 */
public class LocalRegistryFactory extends AbstractRegistryFactory {

	@Override
	protected Registry createRegistry(URL url) {
		return new LocalRegistry();
	}

}
