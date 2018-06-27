package com.fmgame.bolt.remoting.netty;

import com.fmgame.bolt.remoting.Channel;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.RemotingException;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.rpc.URL;

/**
 * netty server测试用例
 * 
 * @author luowei
 * @date 2017年10月20日 上午10:21:12
 */
public class NettyServerExample {
	
    public static void main(String[] args) throws InterruptedException, RemotingException {
        URL url = new URL("netty", "127.0.0.1", 18080, "com.fmgame.bolt.example.IHello");
        
        new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;

                System.out.println("[server] get request: requestId: " + request.getRequestId() + " method: " + request.getMethodName());

                DefaultResponse response = new DefaultResponse(request.getRequestId());
                response.setValue("requestId: " + request.getRequestId() + " time: " + System.currentTimeMillis());

                return response;
            }
        });
        
        System.out.println("~~~~~~~~~~~~~ Server open ~~~~~~~~~~~~~");
    }
    
}
