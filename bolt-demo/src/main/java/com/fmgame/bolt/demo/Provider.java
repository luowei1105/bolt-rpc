package com.fmgame.bolt.demo;

import com.fmgame.bolt.config.impl.DefaultProviderHander;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:23:00
 */
public class Provider {

	public static void main(String[] args) {
		DefaultProviderHander handler = new DefaultProviderHander("bolt_demo_provider.xml");
		handler.initialize();
        // 阻止JVM退出
        synchronized (DefaultProviderHander.class) {
            while (true) {
                try {
                	DefaultProviderHander.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
	}
	
}
