package com.fmgame.bolt.rpc;

import com.fmgame.bolt.extension.SPI;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;

/**
 * Filter. (SPI, Singleton, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年11月1日 下午2:36:20
 */
@SPI
public interface Filter {

	/**
	 * do invoke filter.
	 * <p>
     * <code>
     * // before filter
     * Response response = invoker.invoke(request);
     * // after filter
     * return response;
     * </code>
     * 
	 * @param invoker
	 * @param request
	 * @return
	 * @throws RpcException
	 */
	Response invoke(Invoker<?> invoker, Request request) throws RpcException;
	
}
