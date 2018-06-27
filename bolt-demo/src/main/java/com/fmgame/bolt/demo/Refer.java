package com.fmgame.bolt.demo;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import com.fmgame.bolt.config.impl.DefaultRefererHandler;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:27:58
 */
public class Refer {

	public static void main(String[] args) {
//		Protocol protocol = new DefaultProtocol();
//		URL url = new URL("netty", "127.0.0.1", 18080, IHello.class.getName());
//		
//		DefaultRequest request = new DefaultRequest();
//		request.setRequestId(RequestIdGenerator.getRequestId());
//		request.setInterfaceName(IHello.class.getName());
//		request.setMethodName("sayHello");
//		request.setParamtersDesc("void");
//		
//		Invoker<IHello> invoker = protocol.refer(IHello.class, url);
//		Response response = invoker.invoke(request);
//		System.out.println(response.getResult());
		
		DefaultRefererHandler handler = new DefaultRefererHandler("bolt_demo_referer.xml");
		handler.initialize();
		
		int count = 20000;
		long now = System.currentTimeMillis();
		final CountDownLatch latch = new CountDownLatch(count);
		IntStream.range(0, count).parallel().forEach(action -> {
			IHello hello = handler.get(IHello.class);
			System.out.println(hello.sayHello());
			latch.countDown();
		});
		
		try {
			latch.await();
			System.out.println("总执行时间：" + (System.currentTimeMillis() - now) + "ms");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
//		RpcContext.getContext().asyncCall(hello::sayHelloAsync, new FutureListenerWrapper("player", 1) {
//
//			@Override
//			public void operationComplete(ResponseFuture future) throws Exception {
//				int ctx = this.getParams().getIntValue("player");
//				System.out.println(future.get() + "-----" + ctx);
//			}
//			
//		});
		
//		while (true) {
//			try {
//				// 休眠10秒
//				Thread.sleep(10000);
//				hello = handler.get(IHello.class);
//				System.out.println(hello.sayHello());
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}

	}
	
}
