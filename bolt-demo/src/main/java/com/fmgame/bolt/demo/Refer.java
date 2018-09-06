package com.fmgame.bolt.demo;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import com.fmgame.bolt.config.impl.DefaultRefererHandler;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.remoting.impl.FutureListenerWrapper;
import com.fmgame.bolt.rpc.RpcContext;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:27:58
 */
public class Refer {

	public static void main(String[] args) {
		DefaultRefererHandler handler = new DefaultRefererHandler("bolt_demo_referer.xml");
		handler.initialize();
		
		IHello hello = handler.get(IHello.class);
		
		// 同步执行10次
		int count = 10;
		long now = System.currentTimeMillis();
		final CountDownLatch latch = new CountDownLatch(count);
		IntStream.range(0, count).parallel().forEach(action -> {
			System.out.println(hello.sayHello());
			latch.countDown();
		});
		
		try {
			latch.await();
			System.out.println("总执行时间：" + (System.currentTimeMillis() - now) + "ms");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// 异步执行
		RpcContext.getContext().asyncCall(hello::sayHelloAsync, new FutureListenerWrapper("player", 1) {

			@Override
			public void operationComplete(ResponseFuture future) throws Exception {
				int ctx = this.getParams().getIntValue("player");
				System.out.println(future.get() + "-----" + ctx);
			}
			
		});
		

	}
	
}
