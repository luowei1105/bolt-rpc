package com.fmgame.bolt.rpc.protocol;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * 默认实现暴露服务的提供者
 * 
 * @author luowei
 * @param <T>
 * @date 2017年10月30日 上午11:10:46
 */
public class DefaultProvider<T> extends AbstractProvider<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultProvider.class);

	public DefaultProvider(URL url, Class<T> type, T impl) {
		super(url, type, impl);
	}
	
    @Override
    public Response invoke(Request request) throws RpcException {
        DefaultResponse response = new DefaultResponse(request.getRequestId());

        Method method = lookup(request);
        if (method == null) {	
        	RpcException exception = new RpcException("Service method not exist: "
        			+ request.getInterfaceName() + "." + request.getMethodName() + "(" + request.getParamtersDesc() + ")");

            response.setException(exception);
            return response;
        }

        try {
            Object value = method.invoke(impl, request.getArguments());
            response.setValue(value);
        } catch (Exception e) {
            if (e.getCause() != null) {
                response.setException(new RpcException("provider call process error", e.getCause()));
            } else {
                response.setException(new RpcException("provider call process error", e));
            }
            //服务发生错误时，显示详细日志
            logger.error("Exception caught when during method invocation. request:" + request.toString(), e);
        } catch (Throwable t) {
            // 如果服务发生Error，将Error转化为Exception，防止拖垮调用方
            if (t.getCause() != null) {
                response.setException(new RpcException("provider has encountered a fatal error!", t.getCause()));
            } else {
                response.setException(new RpcException("provider has encountered a fatal error!", t));
            }
            //对于Throwable,也记录日志
            logger.error("Exception caught when during method invocation. request:" + request.toString(), t);
        }
        response.setAttachments(request.getAttachments());
        return response;
    }

}
