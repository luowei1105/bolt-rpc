package com.fmgame.bolt.demo;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:24:17
 */
public class HelloImpl implements IHello {

	@Override
	public String sayHello() {
		return "i am a rpc framework for bolt";
	}

	@Override
	public String sayHelloAsync() {
		return "async";
	}

}
