package com.fmgame.bolt.registry;

import com.fmgame.bolt.extension.SPI;
import com.fmgame.bolt.rpc.URL;

/**
 * 注册工厂类. (SPI, Singleton, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年11月3日 下午5:33:36
 */
@SPI
public interface RegistryFactory {

	/**
	 * 连接注册中心.
	 * 
	 * @param url 注册中心地址，不允许为空
	 * @return 注册中心引用，总不返回空
	 */
	Registry getRegistry(URL url);

}
