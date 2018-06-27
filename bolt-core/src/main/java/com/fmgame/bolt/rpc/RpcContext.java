package com.fmgame.bolt.rpc;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.remoting.FutureListener;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ResponseFuture;

/**
 * RPC会话上下文.(API, ThreadLocal, ThreadSafe)
 * <p>
 * 注意：RpcContext是一个临时状态记录器，当接收到RPC请求，或发起RPC请求时，RpcContext的状态都会变化。
 * 比如：A调B，B再调C，则B机器上，在B调C之前，RpcContext记录的是A调B的信息，在B调C之后，RpcContext记录的是B调C的信息。
 * 
 * @author luowei
 * @date 2017年10月20日 下午2:18:51
 */
public class RpcContext {
	
	private static final Logger logger = LoggerFactory.getLogger(RpcContext.class);
	
	/** rpc 请求 */
	private Request request;
	/** rpc 响应 */
	private Response response;
	/** 属性列表 */
	private Map<String, Object> attributes = new ConcurrentHashMap<>();
	/** 异步回调future */
	private ResponseFuture future;

    private static final ThreadLocal<RpcContext> LOCAL = new ThreadLocal<RpcContext>() {
    	
        protected RpcContext initialValue() {
            return new RpcContext();
        }
    };
    
    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public void putAttribute(String key, Object value){
    	attributes.put(key, value);
    }
    
    public Map<String, Object> getAttributes() {
		return attributes;
	}

    public ResponseFuture getFuture() {
        return future;
    }
    
    public void setFuture(ResponseFuture future) {
        this.future = future;
    }

	/**
     * init rpcContext with request
     * 
     * @param request
     * @return
     */
    public static RpcContext init(Request request){
		RpcContext context = getContext();
		if (request != null && context.getRequest() == null) {
			context.request = request;
			context.attributes.putAll(request.getAttachments());
		}
        return context;
    }

    /**
     * 获取上下文.
     *
     * @return context
     */
    public static RpcContext getContext() {
        return LOCAL.get();
    }
    
    /**
     * 移除上下文.
     */
    public static void destroy() {
        LOCAL.remove();
    }
    
    /**
     * 是否异步请求
     * 
     * @return
     */
    public boolean isAsync() {
    	boolean isAsync = false;
		Object async = RpcContext.getContext().getAttribute(Constants.ASYNC_SUFFIX);
		if (async != null && async instanceof Boolean) {
			isAsync = (Boolean) async;
		}
		return isAsync;
    }
    
    /**
     * 请求方法调用是否单向的,无返回值
     * 
     * @return
     */
	public boolean isOneway() {
    	boolean isOneway = false;
		Object oneway = RpcContext.getContext().getAttribute(Constants.ONE_WAY);
		if (oneway != null && oneway instanceof Boolean) {
			isOneway = (Boolean) oneway;
		}
		return isOneway;
    }
    
    /**
     * 异步调用
     * @param callable
     * @param listener
     */
	public <T extends Object> void asyncCall(Callable<T> callable, FutureListener listener) {
		try {
			callable.call();
			ResponseFuture future = RpcContext.getContext().getFuture();
			if (future != null)
				future.addListener(listener);
		} catch (Exception e) {
			logger.error("异步调用", e);
		}
	}
	
}
