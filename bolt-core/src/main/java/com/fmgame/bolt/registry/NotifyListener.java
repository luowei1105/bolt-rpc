package com.fmgame.bolt.registry;

import java.util.List;
import com.fmgame.bolt.rpc.URL;

/**
 * 变更事件通知.
 * 
 * @author luowei
 * @date 2017年10月16日 下午2:36:40
 */
public interface NotifyListener {

	/**
	 * 当收到服务变更通知时触发。
	 * 
	 * @param urls
	 */
	void notify(List<URL> urls);
}
