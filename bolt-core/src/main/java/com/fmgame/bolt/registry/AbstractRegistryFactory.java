package com.fmgame.bolt.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.rpc.URL;

/**
 * 抽象注册工厂类.
 * 
 * @author luowei
 * @date 2017年11月3日 下午5:36:42
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    /** 注册中心获取过程锁 */
    private static final ReentrantLock LOCK = new ReentrantLock();
    /** 注册中心集合 Map<RegistryAddress, Registry> */
    private static final Map<String, Registry> REGISTRIES = new ConcurrentHashMap<String, Registry>();

    /**
     * 获取所有注册中心
     *
     * @return 所有注册中心
     */
    public static Collection<Registry> getRegistries() {
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }

    public Registry getRegistry(URL url) {
        String key = url.getUri();
        // 锁定注册中心获取过程，保证注册中心单一实例
        LOCK.lock();
        try {
            Registry registry = REGISTRIES.get(key);
            if (registry != null) {
                return registry;
            }
            registry = createRegistry(url);
            if (registry == null) {
                throw new IllegalStateException("Can not create registry " + url);
            }
            REGISTRIES.put(key, registry);
            return registry;
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    protected abstract Registry createRegistry(URL url);
    
    /**
     * 关闭所有已创建注册中心
     */
    public static void destroyAll() {
        if (logger.isInfoEnabled()) {
            logger.info("Close all registries " + getRegistries());
        }
        // 锁定注册中心关闭过程
        LOCK.lock();
        try {
            for (Registry registry : getRegistries()) {
                try {
                    registry.destroy();
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            }
            REGISTRIES.clear();
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }
    
}
