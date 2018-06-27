package com.fmgame.bolt.registry.zookeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.registry.FailbackRegistry;
import com.fmgame.bolt.registry.NotifyListener;
import com.fmgame.bolt.rpc.RpcException;
import com.fmgame.bolt.rpc.URL;

/**
 * zookeeper注册
 * 
 * @author luowei
 * @date 2018年1月26日 上午10:45:19
 */
public class ZookeeperRegistry extends FailbackRegistry {

	private final static Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

	/** zookeeper client */
	private ZkClient zkClient;
	/** zookeeper client */
	private volatile KeeperState state = KeeperState.SyncConnected;
	/** zookeeper监听 */
	private ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> serviceListeners = new ConcurrentHashMap<>();
	/** 行为锁 */
	private final ReentrantLock lock = new ReentrantLock();

	public ZookeeperRegistry(URL url, ZkClient zkClient) {
		super(url);
		this.zkClient = zkClient;
		IZkStateListener zkStateListener = new IZkStateListener() {
			
			@Override
			public void handleStateChanged(KeeperState state) throws Exception {
				ZookeeperRegistry.this.state = state;
			}

			@Override
			public void handleNewSession() throws Exception {
	            try {
	            	// 恢复
                    recover();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
			}

			@Override
			public void handleSessionEstablishmentError(Throwable error) throws Exception {
			}
		};
		zkClient.subscribeStateChanges(zkStateListener);
	}

	@Override
	public void destroy() {
		super.destroy();
		
		try {
			zkClient.close();
		} catch (Exception e) {
			logger.warn("Failed to close zookeeper client " + getUrl() + ", cause: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isAvailable() {
		return state == KeeperState.SyncConnected;
	}

	@Override
	protected void doRegister(URL url) {
		if (destroyed.get()) {
			return;
		}
		
		try {
			lock.lock();
			
			String parentNodePath = toParentNodePath(url);
		    if (!zkClient.exists(parentNodePath)) {
		    	zkClient.createPersistent(parentNodePath, true);
	        }
			zkClient.createEphemeral(toNodePath(url), url.getParameterJson());
		} catch (Throwable e) {
			throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected void doUnregister(URL url) {
		try {
			lock.lock();
			
			String nodePath = toNodePath(url);
			if (zkClient.exists(nodePath)) {
				zkClient.delete(nodePath);
			}
		} catch (Throwable e) {
			throw new RpcException(String.format("Failed to unregister %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected void doSubscribe(URL url, NotifyListener listener) {
		try {
			lock.lock();
			ConcurrentHashMap<NotifyListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
			if (childChangeListeners == null) {
				serviceListeners.putIfAbsent(url, new ConcurrentHashMap<>());
				childChangeListeners = serviceListeners.get(url);
			}
			IZkChildListener zkChildListener = childChangeListeners.get(listener);
			if (zkChildListener == null) {
				childChangeListeners.putIfAbsent(listener, new IZkChildListener() {
					
					@Override
					public void handleChildChange(String parentPath, List<String> currentChilds) {
						List<URL> urls = nodeChildsToUrls(url, parentPath, currentChilds);
						if (urls == null)
							return;
						
						ZookeeperRegistry.this.notify(url, listener, urls);
					}
				});
				zkChildListener = childChangeListeners.get(listener);
			}

			String nodeParentPath = toParentNodePath(url);
			List<String> children = zkClient.subscribeChildChanges(nodeParentPath, zkChildListener);
			List<URL> urls = null;
			if (children != null) {
				urls = nodeChildsToUrls(url, nodeParentPath, children);
			}
			notify(url, listener, urls);
		} catch (Throwable e) {
			throw new RpcException(String.format("Failed to subscribe %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected void doUnsubscribe(URL url, NotifyListener listener) {
		try {
			lock.lock();
			Map<NotifyListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
			if (childChangeListeners != null) {
				IZkChildListener zkChildListener = childChangeListeners.get(listener);
				if (zkChildListener != null) {
					zkClient.unsubscribeChildChanges(toNodePath(url), zkChildListener);
					childChangeListeners.remove(listener);
				}
			}
		} catch (Throwable e) {
			throw new RpcException(String.format("Failed to unsubscribe service %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
		} finally {
			lock.unlock();
		}
	}
	
    private List<URL> nodeChildsToUrls(URL url, String parentPath, List<String> currentChilds) {
        List<URL> urls = new ArrayList<URL>();
        if (currentChilds != null) {
            for (String node : currentChilds) {
                String nodePath = parentPath + Constants.PATH_SEPARATOR + node;
                String data = null;
                try{
                    data = zkClient.readData(nodePath, true);
                } catch (Exception e){
                	logger.warn("get zkdata fail!" + e.getMessage());
                }
                JSONObject jsonObj = JSONObject.parseObject(data);
                
                URL newurl = url.createCopy();
                newurl.setHost(jsonObj.getString("address"));
                newurl.setPort(jsonObj.getIntValue("port"));
                urls.add(newurl);
            }
        }
        return urls;
    }

	private static String toParentNodePath(URL url) {
		return Constants.PATH_SEPARATOR + Constants.DEFAULT_NAME 
				+ Constants.PATH_SEPARATOR + url.getGroup() 
				+ Constants.PATH_SEPARATOR + url.getPath();
	}

	private static String toNodePath(URL url) {
        return toParentNodePath(url) + Constants.PATH_SEPARATOR + url.getAddress();
    }
	
}
