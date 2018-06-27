package com.fmgame.bolt.cluster.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.cluster.AbstractClusterInvoker;
import com.fmgame.bolt.cluster.Directory;
import com.fmgame.bolt.cluster.LoadBalance;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.utils.NetUtils;

/**
 * 失败转移，当出现失败，重试其它服务器，通常用于读操作，但重试会带来更长延迟。
 * 
 * @author luowei
 * @date 2018年4月3日 下午5:02:22
 */
public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {
	
    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);
	
    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }

	@Override
	protected Response doInvoke(Request request, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        List<Invoker<T>> copyinvokers = invokers;
        checkInvokers(copyinvokers, request);
        int len = getUrl().getIntParameter(URLParamType.RETRIES.getName(), URLParamType.RETRIES.getIntValue()) + 1;
        if (len <= 0) {
            len = 1;
        }
        // retry loop.
        RpcException le = null; // last exception.
        List<Invoker<T>> invoked = new ArrayList<>(copyinvokers.size()); // invoked invokers.
        Set<String> providers = new HashSet<>(len);
        for (int i = 0; i < len; i++) {
            // 重试时，进行重新选择，避免重试时invoker列表已发生变化.
            // 注意：如果列表发生了变化，那么invoked判断会失效，因为invoker示例已经改变
            if (i > 0) {
                checkWhetherDestroyed();
                copyinvokers = list(request);
                //重新检查一下
                checkInvokers(copyinvokers, request);
            }
            Invoker<T> invoker = select(loadbalance, request, copyinvokers, invoked);
            invoked.add(invoker);
//            RpcContext.getContext().setInvokers((List) invoked);
            try {
                Response result = invoker.invoke(request);
                if (le != null && logger.isWarnEnabled()) {
                    logger.warn("Although retry the method " + request.getMethodName()
                            + " in the service " + getInterface().getName()
                            + " was successful by the provider " + invoker.getUrl().getAddress()
                            + ", but there have been failed providers " + providers
                            + " (" + providers.size() + "/" + copyinvokers.size()
                            + ") from the registry " + directory.getUrl().getAddress()
                            + " on the consumer " + NetUtils.getLocalHost()
                            + ". Last error is: " + le.getMessage(), le);
                }
                return result;
            } catch (RpcException e) {
                if (e.isBiz()) { // biz exception.
                    throw e;
                }
                le = e;
            } catch (Throwable e) {
                le = new RpcException(e.getMessage(), e);
            } finally {
                providers.add(invoker.getUrl().getAddress());
            }
        }
        throw new RpcException(le != null ? le.getCode() : 0, "Failed to invoke the method "
                + request.getMethodName() + " in the service " + getInterface().getName()
                + ". Tried " + len + " times of the providers " + providers
                + " (" + providers.size() + "/" + copyinvokers.size()
                + ") from the registry " + directory.getUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost()
                + ". Last error is: " + (le != null ? le.getMessage() : ""), le != null && le.getCause() != null ? le.getCause() : le);
	}

}
