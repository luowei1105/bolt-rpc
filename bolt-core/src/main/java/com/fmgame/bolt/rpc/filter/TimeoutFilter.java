package com.fmgame.bolt.rpc.filter;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.extension.Activate;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.rpc.Filter;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;

/**
 * 如果执行timeout，则log记录下，不干涉服务的运行
 * 
 * @author luowei
 * @date 2017年11月1日 下午3:36:08
 */
@Activate(group = Constants.PROVIDER)
public class TimeoutFilter implements Filter {
	
    private static final Logger logger = LoggerFactory.getLogger(TimeoutFilter.class);

	@Override
	public Response invoke(Invoker<?> invoker, Request request) throws RpcException {
		long start = System.currentTimeMillis();
		Response result = invoker.invoke(request);
		long elapsed = System.currentTimeMillis() - start;
		
        if (invoker.getUrl() != null && elapsed > invoker.getUrl().getIntParameter(
        		URLParamType.REQUEST_TIMEOUT.getName(), URLParamType.REQUEST_TIMEOUT.getIntValue())) {
            if (logger.isWarnEnabled()) {
                logger.warn("invoke time out. method: " + request.getMethodName()
                        + " arguments: " + Arrays.toString(request.getArguments()) + " , url is "
                        + invoker.getUrl() + ", invoke elapsed " + elapsed + " ms.");
            }
        }
		return result;
	}

}
