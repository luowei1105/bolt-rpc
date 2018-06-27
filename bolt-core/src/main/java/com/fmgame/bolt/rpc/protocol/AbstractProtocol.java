package com.fmgame.bolt.rpc.protocol;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.Exporter;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.Protocol;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.ConcurrentHashSet;
import com.fmgame.bolt.utils.ProtocolUtils;

/**
 * 抽象协议
 * 
 * @author luowei
 * @date 2017年10月26日 下午2:31:28
 */
public abstract class AbstractProtocol implements Protocol {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProtocol.class);
	
	/**(protocol://host:port/group/interface,服务暴露者)*/
	protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();
	/** 服务引导调用者 */
    protected final Set<Invoker<?>> invokers = new ConcurrentHashSet<>();

	@Override
	public void destroy() {
		destroyInvokers();
		destoryExporters();
	}
	
	/**
	 * 释放调用资源
	 */
	private void destroyInvokers() {
        for (Invoker<?> invoker : invokers) {
            if (invoker != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Destroy reference: " + invoker.getUrl());
                    }
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
        invokers.clear();
	}
	
	/**
	 * 释放引用服务 
	 */
	private void destoryExporters() {
		for (Exporter<?> exporter : exporterMap.values()) {
			if (exporter == null)
				continue;
			
			try {
	            if (logger.isInfoEnabled()) {
	                logger.info("Unexport service: " + exporter.getProvider().getUrl());
	            }
	            exporter.unexport();
			} catch (Throwable t) {
				logger.warn(t.getMessage(), t);
			}
		}
		exporterMap.clear();
	}

	protected String serviceKey(URL url) {
		return ProtocolUtils.getServiceKey(url);
	}
	
    protected static String serviceKey(Request request) {
        return ProtocolUtils.getServiceKey(request);
    }
	
}
