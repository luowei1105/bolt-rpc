package com.fmgame.bolt.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import com.fmgame.bolt.common.Constants;

/**
 * 默认线程池工厂
 * 
 * @author luowei
 * @date 2017年10月18日 下午4:57:28
 */
public class NamedThreadFactory implements ThreadFactory {
	
	static final AtomicInteger poolNumber = new AtomicInteger(1);
    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;
    int priority = Thread.NORM_PRIORITY;
    boolean isDaemon = false;
    
	public NamedThreadFactory() {
		this(Constants.DEFAULT_NAME);
	}

	public NamedThreadFactory(String prefix) {
		this(prefix, false);
	}

	public NamedThreadFactory(String prefix, boolean isDaemon) {
		this(prefix, isDaemon, Thread.NORM_PRIORITY);
	}

    public NamedThreadFactory(String prefix, boolean isDaemon, int priority) {
        SecurityManager s = System.getSecurityManager();
        this.group = (s != null)? s.getThreadGroup() :
                             Thread.currentThread().getThreadGroup();
        this.namePrefix = prefix + "-" +
                      poolNumber.getAndIncrement() +
                     "-thread-";
        this.isDaemon = isDaemon;
        this.priority = priority;
    }

    @Override
	public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

	public ThreadGroup getGroup() {
		return group;
	}
    
}
