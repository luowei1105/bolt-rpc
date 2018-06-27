package com.fmgame.bolt.rpc.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.remoting.Client;
import com.fmgame.bolt.remoting.EndpointFactory;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.Server;
import com.fmgame.bolt.remoting.impl.DefaultFuture;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.remoting.impl.HeartBeatClient;
import com.fmgame.bolt.remoting.impl.HeartBeatMessageHandleWrapper;
import com.fmgame.bolt.rpc.Exporter;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.Provider;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * 默认协议
 * 
 * @author luowei
 * @date 2017年10月27日 下午7:58:30
 */
public class DefaultProtocol extends AbstractProtocol {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultProtocol.class);

	/** <host:port,Server> */
	private final Map<String, Server> serverMap = new ConcurrentHashMap<>();
	/** 对请求的消息进行处理 */
	private MessageHandler requestHandler = new HeartBeatMessageHandleWrapper((channel, message) -> {
		Request request = (Request) message;
		
		String serviceKey = serviceKey(request);
		DefaultExporter<?> exporter = (DefaultExporter<?>) exporterMap.get(serviceKey);
		if (exporter == null) {
			logger.error(this.getClass().getSimpleName() + " handler Error: provider not exist serviceKey=" + serviceKey + " " + request);
			RpcException exception = new RpcException(this.getClass().getSimpleName()
					+ " handler Error: provider not exist serviceKey=" + serviceKey + " " + request);

			DefaultResponse response = new DefaultResponse(request.getRequestId());
			response.setException(exception);
			return response;
		}
		
		try {
			return exporter.getProvider().invoke(request);
		} catch (RpcException e) {
			DefaultResponse response = new DefaultResponse(request.getRequestId());
			response.setException(e);
			return response;
		}
	});
	
	/** <host:port,Client> */
	private final Map<String, Client> referenceClientMap = new ConcurrentHashMap<>();
	/** 收到回调消息处理 */
	private MessageHandler receivedHandler =  (channel, message) -> {
		Response response = (Response) message;
		DefaultFuture.received(channel, response);
		
		return null;
	};
			
	@Override
	public <T> Exporter<T> export(Provider<T> provider) throws RpcException {
		URL url = provider.getUrl();

		// 暴露 service.
		String key = serviceKey(url);
		DefaultExporter<T> exporter = new DefaultExporter<T>(provider, key, exporterMap);
		exporterMap.put(key, exporter);
		// 开启服务
		openServer(url);

		return exporter;
	}

	/**
	 * 开启服务
	 * 
	 * @param url
	 */
	private void openServer(URL url) {
		// find server.
		String key = url.getAddress();
		Server server = serverMap.get(key);
		if (server == null) {
			serverMap.put(key, createServer(url));
		}
	}

	/**
	 * 创建服务
	 * 
	 * @param url
	 * @return
	 */
	private Server createServer(URL url) {
		Server server = null;
		try {
			EndpointFactory factory = ExtensionLoader.getExtensionLoader(EndpointFactory.class)
					.getExtension(url.getParameter(URLParamType.TRANSPORTER.name(), URLParamType.TRANSPORTER.getValue()));
			server = factory.createServer(url, requestHandler);
		} catch (Exception e) {
			throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
		}
		
		return server;
	}

	@Override
	public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
		// create rpc invoker.
		DefaultInvoker<T> invoker = new DefaultInvoker<T>(type, url, getClients(url), invokers);
		invokers.add(invoker);
		return invoker;
	}

	private Client[] getClients(URL url) {
		// 是否共享连接
		boolean service_share_connect = false;
		int connections = url.getIntParameter(URLParamType.CONNECTIONS.name(), URLParamType.CONNECTIONS.getIntValue());
		// 如果connections不配置，则共享连接，否则每服务每连接
		if (connections == 1) {
			service_share_connect = true;
		}

		Client[] clients = new Client[connections];
		for (int i = 0; i < clients.length; i++) {
			if (service_share_connect) {
				clients[i] = getSharedClient(url);
			} else {
				clients[i] = createClient(url);
			}
		}
		return clients;
	}

	/**
	 * 获取共享连接
	 */
	private Client getSharedClient(URL url) {
		String key = url.getAddress();
		Client client = getClientFromCache(key);
		if (client != null) {
			return client;
		}
		synchronized (key.intern()) {
			client = getClientFromCache(key);
			if (client != null) {
				return client;
			}
			
			client = createClient(url);
			referenceClientMap.put(key, client);
			return client;
		}
	}
	
	private Client getClientFromCache(String key) {
		Client client = referenceClientMap.get(key);
		if (client != null && client.isClosed()) {
			referenceClientMap.remove(key);
			client = null;
		}
		return client;
	}

	/**
	 * 创建新连接.
	 */
	private Client createClient(URL url) {
		Client client = null;
		try {
			EndpointFactory factory = ExtensionLoader.getExtensionLoader(EndpointFactory.class)
					.getExtension(url.getParameter(URLParamType.TRANSPORTER.name(), URLParamType.TRANSPORTER.getValue()));
			client = factory.createClient(url, receivedHandler);
		} catch (Exception e) {
			throw new RpcException("Fail to create remoting client for service(url: " + url + ") ", e);
		}
		
		return new HeartBeatClient(client, true);
	}
	
	@Override
    public void destroy() {
		destoryServer();
		destroyClient();
		
        super.destroy();
    }
	
	/**
	 * 是否所有服务
	 */
	private void destoryServer() {
        for (Server server : serverMap.values()) {
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close dubbo server: " + server.getLocalAddress());
                    }
                    server.close(Constants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT);
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        serverMap.clear();
	}
	
	/**
	 * 是否所有客户端连接
	 */
	private void destroyClient() {
        for (Client client : referenceClientMap.values()) {
            if (client != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close bolt connect: " + client.getLocalAddress() + "-->" + client.getRemoteAddress());
                    }
                    client.close(Constants.DEFAULT_SERVER_SHUTDOWN_TIMEOUT);
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        referenceClientMap.clear();
	}

}
