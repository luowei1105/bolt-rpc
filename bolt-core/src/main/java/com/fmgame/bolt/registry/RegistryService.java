package com.fmgame.bolt.registry;

import java.util.List;
import com.fmgame.bolt.rpc.URL;

/**
 * 注册服务. (SPI, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月16日 下午2:28:24
 */
public interface RegistryService {

    /**
     * 向指定注册中心(如：zookeeper)注册服务.
     *
     * @param url 注册信息，不允许为空，如：bolt://127.0.0.1:0/default/com.fmgame.foo.BarService
     */
    void register(URL url);
    
    /**
     * 取消注册.
     * 
     * @param url 注册信息，不允许为空，如：bolt://127.0.0.1:0/default/com.fmgame.foo.BarService
     */
    void unregister(URL url);

    /**
     * 订阅符合条件的已注册数据，当有注册数据变更时自动推送.
     * 
     * @param url 订阅条件，不允许为空
     * @param listener 变更事件监听器，不允许为空
     */
    void subscribe(URL url, NotifyListener listener);
    
    /**
     * 取消订阅.
     * 
     * @param url 订阅条件，不允许为空
     * @param listener 变更事件监听器，不允许为空
     */
    void unsubscribe(URL url, NotifyListener listener);
    
    /**
     * 查询符合条件的已注册数据，与订阅的推模式相对应，这里为拉模式。
     * 
     * @param url 查询条件，不允许为空
     * @return 已注册信息列表，可能为空
     */
    List<URL> lookup(URL url);
    
}
