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
 * 此线程池一直增长，直到上限，增长后不收缩。
 * 
 * @author luowei
 * @date 2018年4月26日 下午12:47:10
 */
public class LimitedThreadPool implements ThreadPool {

	@Override
	public Executor getExecutor(URL url) {
		String name = url.getParameter(URLParamType.WORKER_THREAD_NAME.getName(), URLParamType.WORKER_THREAD_NAME.getValue());
		int cores = url.getIntParameter(URLParamType.WORKER_CORE_THREADS.getName(), URLParamType.WORKER_CORE_THREADS.getIntValue());
		int threads = url.getIntParameter(URLParamType.WORKER_THREADS.getName(), URLParamType.WORKER_THREADS.getIntValue());
		int queues = url.getIntParameter(URLParamType.WORKER_THREAD_QUEUES.getName(), URLParamType.WORKER_THREAD_QUEUES.getIntValue());
        
        return new ThreadPoolExecutor(cores, threads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() :
                        (queues < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedThreadFactory(name, true));
	}

}
