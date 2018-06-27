package com.fmgame.bolt.proxy.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import com.fmgame.bolt.proxy.ProxyFactory;

/**
 * jdk proxy
 * @author luowei
 * @date 2017年12月27日 下午8:08:55
 */
public class JDKProxyFactory implements ProxyFactory {

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clz, InvocationHandler invocationHandler) {
        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {clz}, invocationHandler);
    }
    
}
