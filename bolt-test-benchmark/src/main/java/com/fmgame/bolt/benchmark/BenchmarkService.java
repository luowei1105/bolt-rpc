package com.fmgame.bolt.benchmark;

import com.fmgame.bolt.config.impl.DefaultProviderHander;

/**
 * @author luowei
 * @date 2018年4月23日 下午5:39:44
 */
public class BenchmarkService {

	public static void main(String[] args) {
		DefaultProviderHander handler = new DefaultProviderHander("benchmark-server.xml");
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
