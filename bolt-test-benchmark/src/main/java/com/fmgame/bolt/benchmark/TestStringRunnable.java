package com.fmgame.bolt.benchmark;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import com.fmgame.bolt.benchmark.entity.IBenchmark;

public class TestStringRunnable extends AbstractClientRunnable {
    private String str;

    public TestStringRunnable(IBenchmark benchmark, String params, CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        super(benchmark, barrier, latch, startTime, endTime);
        int size = Integer.parseInt(params);
        int length = 1024 * size;
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append((char) (ThreadLocalRandom.current().nextInt(33, 128)));
        }
        str = builder.toString();
    }

    @Override
    protected Object call(IBenchmark benchmark) {
        Object result = benchmark.echoService(str);
        return result;
    }
}
