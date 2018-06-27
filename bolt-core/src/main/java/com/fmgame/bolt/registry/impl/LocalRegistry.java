package com.fmgame.bolt.registry.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.registry.AbstractRegistry;
import com.fmgame.bolt.registry.NotifyListener;
import com.fmgame.bolt.registry.RegistryService;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.ConcurrentHashSet;
import com.fmgame.bolt.utils.NetUtils;
import com.fmgame.bolt.utils.UrlUtils;

/**
 * 本地注册服务
 * 
 * @author luowei
 * @date 2017年11月27日 上午11:10:05
 */
public class LocalRegistry extends AbstractRegistry {
	
	private static final Logger logger = LoggerFactory.getLogger(LocalRegistry.class);
	
    private final ConcurrentMap<String, Set<URL>> remoteRegistered = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, ConcurrentHashMap<URL, Set<NotifyListener>>> remoteSubscribed = new ConcurrentHashMap<>();
    
    public LocalRegistry() {
		super(new URL(Constants.REGISTRY_PROTOCOL_LOCAL, NetUtils.LOCALHOST,
				Constants.DEFAULT_INT_VALUE, RegistryService.class.getName()));
    }
	
	@Override
	public boolean isAvailable() {
		return true;
	}
	
    @Override
    public void register(URL url) {
        String registryKey = getRegistryKey(url);
		Set<URL> urls = remoteRegistered.get(registryKey);
		if (urls == null) {
			remoteRegistered.putIfAbsent(registryKey, new ConcurrentHashSet<URL>());
			urls = remoteRegistered.get(registryKey);
		}

		urls.add(url);
		super.register(url);
		logger.info("LocalRegistryService register: url={}", url);

		// 在变更后立即进行通知
		notifyListeners(url);
    }

    @Override
    public void unregister(URL url) {
		Set<URL> urls = remoteRegistered.get(getRegistryKey(url));
		if (urls == null) {
			return;
		}
		
		urls.remove(url);
		super.unregister(url);
		logger.info("LocalRegistryService unregister: url={}", url);
		
		// 在变更后立即进行通知
		notifyListeners(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        String subscribeKey = getRegistryKey(url);
        ConcurrentHashMap<URL, Set<NotifyListener>> urlListeners = remoteSubscribed.get(subscribeKey);
        if (urlListeners == null) {
            remoteSubscribed.putIfAbsent(subscribeKey, new ConcurrentHashMap<URL, Set<NotifyListener>>());
            urlListeners = remoteSubscribed.get(subscribeKey);
        }

        Set<NotifyListener> listeners = urlListeners.get(url);
        if (listeners == null) {
            urlListeners.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = urlListeners.get(url);
        }

        listeners.add(listener);
        super.subscribe(url, listener);
        
        List<URL> urls = lookup(url);
        if (urls != null && urls.size() > 0) {
            listener.notify(urls);
        }

        logger.info("LocalRegistryService subscribe: url={}", url);
    }

	@Override
	public void unsubscribe(URL url, NotifyListener listener) {
		String subscribeKey = getRegistryKey(url);
		ConcurrentHashMap<URL, Set<NotifyListener>> urlListeners = remoteSubscribed.get(subscribeKey);
		if (urlListeners != null) {
			urlListeners.remove(url);
		}
		super.unsubscribe(url, listener);

		logger.info("LocalRegistryService unsubscribe: url={}", url);
	}
    
    private String getRegistryKey(URL url) {
    	String group = url.getParameter(URLParamType.GROUP.getName());
        String addr = url.getAddress();
        if (group != null) {
            return group + Constants.PATH_SEPARATOR + addr;
        } else {
            logger.warn("Url need a group as param in localRegistry, url={}", url);
            return addr;
        }
    }
    
    private void notifyListeners(URL changedUrl) {
        List<URL> interestingUrls = lookup(changedUrl);
        if (interestingUrls == null) {
        	return;
        }
        
        ConcurrentHashMap<URL, Set<NotifyListener>> urlListeners = remoteSubscribed.get(getRegistryKey(changedUrl));
        if (urlListeners == null) {
            return;
        }

        for (Set<NotifyListener> listeners : urlListeners.values()) {
            for (NotifyListener ln : listeners) {
                try {
                    ln.notify(interestingUrls);
                } catch (Exception e) {
                    logger.warn(String.format("Exception when notify listerner %s, changedUrl: %s", ln, changedUrl), e);
                }
            }
        }
    }
    
    @Override
    public List<URL> lookup(URL url) {
        List<URL> urls = new ArrayList<URL>();
        for (URL u : getRegistered()) {
            if (UrlUtils.isMatch(url, u)) {
                urls.add(u);
            }
        }
        return urls;
    }


}
