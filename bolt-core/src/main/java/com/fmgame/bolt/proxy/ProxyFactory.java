package com.fmgame.bolt.proxy;

import java.lang.reflect.InvocationHandler;
import com.fmgame.bolt.extension.SPI;

/**
 * 代理工厂类
 * @author luowei
 * @date 2017年12月27日 下午7:43:43
 */
@SPI
public interface ProxyFactory {
	
	/**
	 * 获取对象
	 * @param clz
	 * @param invocationHandler
	 * @return
	 */
	<T> T getProxy(Class<T> clz, InvocationHandler invocationHandler);
	
}
