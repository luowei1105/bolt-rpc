package com.fmgame.bolt.demo;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:24:07
 */
public interface IHello {

	/**
	 * 同步调用
	 * @return
	 */
	String sayHello();
	
	/**
	 * 异步调用，所有异步调用方法名后面必须带上<p>Async</p>
	 * @return
	 */
	String sayHelloAsync();
	
}
