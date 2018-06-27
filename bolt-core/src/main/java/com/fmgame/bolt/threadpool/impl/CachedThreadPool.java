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
 * 此线程池可伸缩，线程空闲一分钟后回收，新请求重新创建线程，来源于：<code>Executors.newCachedThreadPool()</code>
 * 
 * @author luowei
 * @date 2018年4月26日 下午12:42:13
 */
public class CachedThreadPool implements ThreadPool {

	@Override
	public Executor getExecutor(URL url) {
		String name = url.getParameter(URLParamType.WORKER_THREAD_NAME.getName(), URLParamType.WORKER_THREAD_NAME.getValue());
		int cores = url.getIntParameter(URLParamType.WORKER_CORE_THREADS.getName(), URLParamType.WORKER_CORE_THREADS.getIntValue());
		int threads = url.getIntParameter(URLParamType.WORKER_THREADS.getName(), URLParamType.WORKER_THREADS.getIntValue());
		int queues = url.getIntParameter(URLParamType.WORKER_THREAD_QUEUES.getName(), URLParamType.WORKER_THREAD_QUEUES.getIntValue());
        int alive = url.getIntParameter(URLParamType.WORKER_THREAD_ALIVE.getName(), URLParamType.WORKER_THREAD_ALIVE.getIntValue());
        
        return new ThreadPoolExecutor(cores, threads, alive, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedThreadFactory(name, true));
	}

}
