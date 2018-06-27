package com.fmgame.bolt.remoting.impl;

import com.alibaba.fastjson.JSONObject;
import com.fmgame.bolt.remoting.FutureListener;

/**
 * 用于监听Future的success和fail事件包装类
 * 
 * @author luowei
 * @date 2018年1月2日 下午4:01:12
 */
public abstract class FutureListenerWrapper implements FutureListener {
	
	// 当玩家只传入一个数据项时，自动用此KEY值，简化操作
	private static final String KEY_SINGLE = "KEY_SINGLE";
	
	private JSONObject params;
	
	public FutureListenerWrapper(Object... ctx) {
		//无参 返回空即可
		if(ctx == null || ctx.length == 0) 
			return;
		
		//当数据仅有一项是 使用默认KEY作为值 简化操作
		if(ctx.length == 1) {
			params = new JSONObject(1);
			params.put(KEY_SINGLE, ctx[0]);
		} else {	//处理成对参数
			int len = ctx.length;
			params = new JSONObject(len / 2);
			for (int i = 0; i < len; i += 2) {
				String key = (String) ctx[i];
				Object val = (Object) ctx[i + 1];
				
				params.put(key, val);
			}
		}
	}

	public JSONObject getParams() {
		return params;
	}
	
	/**
	 * 单个参数时获取数据
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> T get() {
		return (T) params.get(KEY_SINGLE);
	}

}
