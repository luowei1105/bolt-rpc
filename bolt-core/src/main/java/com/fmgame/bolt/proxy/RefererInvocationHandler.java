package com.fmgame.bolt.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.remoting.impl.DefaultRequest;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcContext;
import com.fmgame.bolt.utils.ReflectUtil;
import com.fmgame.bolt.utils.RequestIdGenerator;

/**
 * 引用调用处理
 * @author luowei
 * @date 2017年12月27日 下午7:44:38
 */
public class RefererInvocationHandler<T> implements InvocationHandler {

	private Class<T> clz;
	private Invoker<?> invoker;

	public RefererInvocationHandler(Class<T> clz, Invoker<?> invoker) {
		this.clz = clz;
		this.invoker = invoker;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		DefaultRequest request = new DefaultRequest();
		request.setRequestId(RequestIdGenerator.getRequestId());
		request.setInterfaceName(clz.getName());
		request.setMethodName(method.getName());
		request.setArguments(args);
		request.setParamtersDesc(ReflectUtil.getMethodParamDesc(method));
		
		request.setAttachment(URLParamType.GROUP.getName(), invoker.getUrl()
				.getParameter(URLParamType.GROUP.getName(), URLParamType.GROUP.getValue()));
		
		RpcContext curContext = RpcContext.getContext();
		boolean async = method.getName().endsWith(Constants.ASYNC_SUFFIX) ? true : false;
		curContext.putAttribute(Constants.ASYNC_SUFFIX, async);
		boolean isOneway = method.getReturnType() == void.class ? true : false;
		curContext.putAttribute(Constants.ONE_WAY, isOneway);
		Map<String, Object> attachments = curContext.getAttributes();
		if (!attachments.isEmpty()) { 
			for (Map.Entry<String, Object> entry : attachments.entrySet()) {
				request.setAttachment(entry.getKey(), entry.getValue());
			}
		}

		Response response = null;
		try {
			response = invoker.invoke(request);
			if (async && response instanceof ResponseFuture) {
				RpcContext.getContext().setFuture((ResponseFuture) response);
				return null;
			} else {
			    RpcContext.getContext().setFuture(null);
				return response.getResult();
			}
		} catch (Exception e) {
		    RpcContext.getContext().setFuture(null);
			return response.getResult();
		}
	}

}
