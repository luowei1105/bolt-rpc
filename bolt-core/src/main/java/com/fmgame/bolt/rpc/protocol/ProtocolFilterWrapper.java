package com.fmgame.bolt.rpc.protocol;

import java.util.List;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.extension.ExtensionLoader;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.rpc.Exporter;
import com.fmgame.bolt.rpc.Filter;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.Protocol;
import com.fmgame.bolt.rpc.Provider;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * 协议过滤
 * 
 * @author luowei
 * @date 2017年11月1日 下午3:08:39
 */
public class ProtocolFilterWrapper implements Protocol {

	private final Protocol protocol;

	public ProtocolFilterWrapper(Protocol protocol) {
		if (protocol == null) {
			throw new IllegalArgumentException("protocol == null");
		}
		this.protocol = protocol;
	}

	@Override
	public <T> Exporter<T> export(Provider<T> provider) throws RpcException {
		if (Constants.REGISTRY_PROTOCOL.equals(provider.getUrl().getProtocol())) {
			return protocol.export(provider);
		}
		return protocol.export(buildProviderChain(provider, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
	}
	
	private static <T> Provider<T> buildProviderChain(final Provider<T> provider, String key, String group) {
		Provider<T> last = provider;
		List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(key, group);
		if (filters.size() > 0) {
			for (int i = filters.size() - 1; i >= 0; i--) {
				final Filter filter = filters.get(i);
				final Provider<T> next = last;
				last = new Provider<T>() {

					public Class<T> getInterface() {
						return provider.getInterface();
					}

					@Override
					public T getImpl() {
						return provider.getImpl();
					}

					public URL getUrl() {
						return provider.getUrl();
					}

					public boolean isAvailable() {
						return provider.isAvailable();
					}

					@Override
					public Response invoke(Request request) throws RpcException {
						return filter.invoke(next, request);
					}

					public void destroy() {
						provider.destroy();
					}

					@Override
					public String toString() {
						return provider.toString();
					}
				};
			}
		}
		return last;
	}


	@Override
	public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
		if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
			return protocol.refer(type, url);
		}
		return buildInvokerChain(protocol.refer(type, url), Constants.REFERENCE_FILTER_KEY, Constants.CONSUMER);
	}

	private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
		Invoker<T> last = invoker;
		List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(key, group);
		if (filters.size() > 0) {
			for (int i = filters.size() - 1; i >= 0; i--) {
				final Filter filter = filters.get(i);
				final Invoker<T> next = last;
				last = new Invoker<T>() {

					public Class<T> getInterface() {
						return invoker.getInterface();
					}

					public URL getUrl() {
						return invoker.getUrl();
					}

					public boolean isAvailable() {
						return invoker.isAvailable();
					}

					@Override
					public Response invoke(Request request) throws RpcException {
						return filter.invoke(next, request);
					}

					public void destroy() {
						invoker.destroy();
					}

					@Override
					public String toString() {
						return invoker.toString();
					}
				};
			}
		}
		return last;
	}

	@Override
	public void destroy() {
		protocol.destroy();
	}

}
