package com.fmgame.bolt.benchmark;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import com.fmgame.bolt.benchmark.entity.IBenchmark;

public class TestEmptyRunnable extends AbstractClientRunnable {

    public TestEmptyRunnable(IBenchmark benchmark, String params, CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        super(benchmark, barrier, latch, startTime, endTime);
    }

    @Override
    protected Object call(IBenchmark benchmark) {
    	benchmark.emptyService();
        return "empty";
    }
}
