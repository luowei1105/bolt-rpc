package com.fmgame.bolt.remoting.netty;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.remoting.impl.DefaultFuture;
import com.fmgame.bolt.remoting.impl.DefaultRequest;
import com.fmgame.bolt.remoting.netty.NettyClient;
import com.fmgame.bolt.rpc.RpcContext;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.RequestIdGenerator;

/**
 * netty client 测试用例
 * 
 * @author luowei
 * @date 2017年10月23日 下午2:07:28
 */
public class NettyClientExample {

	public static void main(String[] args) {
		URL url = new URL("netty", "127.0.0.1", 18080, "com.fmgame.bolt.example.IHello");

		NettyClient client;
		try {
			client = new NettyClient(url, (channel, message) -> {
				Response response = (Response) message;
				DefaultFuture.received(channel, response);
				
				return null;
			});
			
			int count = 20000;
			long now = System.currentTimeMillis();
			final CountDownLatch latch = new CountDownLatch(count);
			IntStream.range(0, count).parallel().forEach(action -> {
				try {
					DefaultRequest request = new DefaultRequest();
					request.setRequestId(RequestIdGenerator.getRequestId());
					request.setInterfaceName("com.fmgame.bolt.example.IHello");
					request.setMethodName("hello");
					request.setParamtersDesc("void");
					
					RpcContext curContext = RpcContext.getContext();
					// curContext.putAttribute(Constants.ASYNC_SUFFIX, true);
					// curContext.putAttribute(Constants.ONE_WAY, true);
					Map<String, Object> attachments = curContext.getAttributes();
					if (!attachments.isEmpty()) { 
						for (Map.Entry<String, Object> entry : attachments.entrySet()) {
							request.setAttachment(entry.getKey(), entry.getValue());
						}
					}
					
					if (curContext.getAttribute(Constants.ONE_WAY) != null) {
						client.request(request);
					} else {
						ResponseFuture response = (ResponseFuture) client.request(request);
						System.out.println(response.getResult());
					}
					latch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			
			try {
				latch.await();
				System.out.println("总执行时间：" + (System.currentTimeMillis() - now) + "ms");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
