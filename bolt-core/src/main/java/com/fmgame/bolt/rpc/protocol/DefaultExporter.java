package com.fmgame.bolt.rpc.protocol;

import java.util.Map;
import com.fmgame.bolt.rpc.Exporter;
import com.fmgame.bolt.rpc.Provider;

/**
 * 服务暴露者
 * 
 * @author luowei
 * @date 2017年10月27日 下午8:04:44
 * @param <T>
 */
public class DefaultExporter<T> extends AbstractExporter<T> {

    private final String key;
    private final Map<String, Exporter<?>> exporterMap;
    
	public DefaultExporter(Provider<T> invoker, String key, Map<String, Exporter<?>> exporterMap) {
		super(invoker);
		
        this.key = key;
        this.exporterMap = exporterMap;
	}

    @Override
    public void unexport() {
        super.unexport();
        exporterMap.remove(key);
    }
    
}
