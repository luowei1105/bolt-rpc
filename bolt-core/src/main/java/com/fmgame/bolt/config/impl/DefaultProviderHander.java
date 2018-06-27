package com.fmgame.bolt.config.impl;

import com.fmgame.bolt.config.AbstractProviderHandler;

/**
 * 默认服务提供处理类 
 * 
 * @author luowei
 * @date 2017年12月1日 下午3:22:33
 */
public class DefaultProviderHander extends AbstractProviderHandler {

	public DefaultProviderHander(String fileName) {
		super(fileName);
	}

	@Override
	protected <T> T ref(String refName) {
		try {
			@SuppressWarnings("unchecked")
			Class<T> clazz = (Class<T>) Class.forName(refName, true, Thread.currentThread().getContextClassLoader());
			return clazz.newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
