package com.fmgame.bolt.utils;

import com.fmgame.bolt.rpc.URL;

/**
 * url 工具类
 * 
 * @author luowei
 * @date 2018年4月8日 上午10:49:13
 */
public final class UrlUtils {

	/**
	 * 判断两个url地址是否匹配
	 * @param consumerUrl
	 * @param providerUrl
	 * @return
	 */
	public static boolean isMatch(URL consumerUrl, URL providerUrl) {
		if (!consumerUrl.getUri().equals(providerUrl.getUri()))
			return false;
		if (!consumerUrl.getGroup().equals(providerUrl.getGroup()))
			return false;
		if (!consumerUrl.getPath().equals(providerUrl.getPath()))
			return false;
		return true;
	}

}
