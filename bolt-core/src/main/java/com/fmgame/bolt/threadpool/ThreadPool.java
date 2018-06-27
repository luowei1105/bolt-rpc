package com.fmgame.bolt.threadpool;

import java.util.concurrent.Executor;
import com.fmgame.bolt.extension.SPI;
import com.fmgame.bolt.rpc.URL;

/**
 * 线程池
 * @author luowei
 * @date 2018年4月26日 上午10:16:47
 */
@SPI
public interface ThreadPool {
	
	/**
	 * 获取线程执行器
	 * 
	 * @param url 线程参数
	 * @return
	 */
	Executor getExecutor(URL url);
		
}
