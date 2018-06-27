package com.fmgame.bolt.threadpool.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.rpc.URL;
import com.fmgame.bolt.threadpool.ThreadPool;
import com.fmgame.bolt.utils.NamedThreadFactory;

/**
 * 此线程池启动时即创建固定大小的线程数，不做任何伸缩，来源于：<code>Executors.newFixedThreadPool()</code>
 * 
 * @author luowei
 * @date 2018年4月26日 上午10:18:03
 */
public class FixedThreadPool implements ThreadPool {

	@Override
	public Executor getExecutor(URL url) {
		String name = url.getParameter(URLParamType.WORKER_THREAD_NAME.getName(), URLParamType.WORKER_THREAD_NAME.getValue());
		int threads = url.getIntParameter(URLParamType.WORKER_THREADS.getName(), URLParamType.WORKER_THREADS.getIntValue());
		int queues = url.getIntParameter(URLParamType.WORKER_THREAD_QUEUES.getName(), URLParamType.WORKER_THREAD_QUEUES.getIntValue());
		
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedThreadFactory(name, true));
	}

}
