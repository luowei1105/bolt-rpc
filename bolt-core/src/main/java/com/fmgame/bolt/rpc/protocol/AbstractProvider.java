package com.fmgame.bolt.rpc.protocol;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.Provider;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.utils.ReflectUtil;

/**
 * 抽象provider
 * 
 * @author luowei
 * @date 2017年10月25日 上午11:44:04
 */
public abstract class AbstractProvider<T> extends AbstractInvoker<T> implements Provider<T> {

	/** 服务实现类 */
	protected final T impl;
	/** 服务关联方法 */
    protected Map<String, Method> methodMap = new HashMap<>();

	public AbstractProvider(URL url, Class<T> type, T impl) {
		super(url, type);
		this.impl = impl;

		initMethodMap(type);
	}

	@Override
	public T getImpl() {
		return impl;
	}
	
	/**
	 * 初始化方法map 
	 * @param clz
	 */
    private void initMethodMap(Class<T> clz) {
        Method[] methods = clz.getMethods();

        for (Method method : methods) {
            String methodDesc = ReflectUtil.getMethodDesc(method);
            methodMap.put(methodDesc, method);
        }
    }
    
    /**
     * 获取请求查询方法
     * @param request
     * @return
     */
    protected Method lookup(Request request) {
        String methodDesc = ReflectUtil.getMethodDesc(request.getMethodName(), request.getParamtersDesc());
        return methodMap.get(methodDesc);
    }

}
