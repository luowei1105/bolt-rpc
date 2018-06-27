package com.fmgame.bolt.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.ConcurrentHashSet;

/**
 * 抽象注册服务类
 * 
 * @author luowei
 * @date 2017年11月3日 下午5:46:22
 */
public abstract class AbstractRegistry implements Registry {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);
	
	/** 注册服务关联url */
	protected URL registryUrl;
	/** 已注册URL列表 */
	protected final Set<URL> registered = new ConcurrentHashSet<>();
	/** 已订阅指定URL集合 */
	protected final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<>();
    /** 已通知指定URL集合 */
    protected final ConcurrentMap<URL, List<URL>> notified = new ConcurrentHashMap<>();
    
    /** 是否销毁 */
    protected AtomicBoolean destroyed = new AtomicBoolean(false);

	public AbstractRegistry(URL url) {
		this.registryUrl = url;
	}

	@Override
	public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Register: " + url);
        }
        registered.add(url);
	}

	@Override
	public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unregister: " + url);
        }
        registered.remove(url);
	}

	@Override
	public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
	}

	@Override
	public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unsubscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
	}

	@Override
	public List<URL> lookup(URL url) {
        List<URL> result = new ArrayList<URL>();
        List<URL> notifiedUrls = getNotified().get(url);
        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            for (URL u : notifiedUrls) {
            	result.add(u);
            }
        } else {
            final AtomicReference<List<URL>> reference = new AtomicReference<>();
            NotifyListener listener = new NotifyListener() {
                public void notify(List<URL> urls) {
                    reference.set(urls);
                }
            };
            subscribe(url, listener); // 订阅逻辑保证第一次notify后再返回
            List<URL> urls = reference.get();
            if (urls != null && urls.size() > 0) {
				for (URL u : urls) {
					result.add(u);
				}
            }
        }
        return result;
	}

	@Override
	public URL getUrl() {
		return registryUrl;
	}

	@Override
	public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("Destroy registry:" + getUrl());
        }
        if (!getRegistered().isEmpty()) {
            for (URL url : new HashSet<>(getRegistered())) {
            	try {
            		unregister(url);
            		if (logger.isInfoEnabled()) {
            			logger.info("Destroy unregister url " + url);
            		}
            	} catch (Throwable t) {
            		logger.warn("Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
            	}
            }
        }
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<>(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener);
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unsubscribe url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
	}

	public Set<URL> getRegistered() {
		return registered;
	}

	public ConcurrentMap<URL, Set<NotifyListener>> getSubscribed() {
		return subscribed;
	}

	public ConcurrentMap<URL, List<URL>> getNotified() {
		return notified;
	}

	protected void notify(URL refUrl, NotifyListener listener, List<URL> urls) {
        if (listener == null || urls == null) {
            return;
        }
        List<URL> curls = notified.get(refUrl);
        if (curls == null) {
        	notified.putIfAbsent(refUrl, new ArrayList<URL>());
            curls = notified.get(refUrl);
        }
        curls.addAll(urls);
        listener.notify(urls);
    }
	
	/**
	 * 恢复
	 * @throws Exception
	 */
    protected void recover() throws Exception {
        // register
        Set<URL> recoverRegistered = new HashSet<>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(url, listener);
                }
            }
        }
    }
	
}
