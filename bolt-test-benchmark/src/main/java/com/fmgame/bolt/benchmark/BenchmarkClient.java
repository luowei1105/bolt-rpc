package com.fmgame.bolt.benchmark;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import com.fmgame.bolt.benchmark.entity.IBenchmark;
import com.fmgame.bolt.config.impl.DefaultRefererHandler;

/**
 * @author luowei
 * @date 2018年4月23日 下午5:48:16
 */
public class BenchmarkClient extends AbstractBenchmarkClient {
	
    static Properties properties = new Properties();
    /**
     * 并发的Runable线程，是否使用相同的client进行调用。
     * true：并发线程只使用一个client（bean实例）调用服务。
     * false: 每个并发线程使用不同的Client调用服务
     */
    private static IBenchmark benchmark;
    private static boolean isMultiClient;

    private static void loadProperties() {
        try {
            properties.load(ClassLoader.getSystemResourceAsStream("benchmark.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("rawtypes")
	@Override
    public ClientRunnable getClientRunnable(String classname, String params, CyclicBarrier barrier,
            CountDownLatch latch, long startTime, long endTime) {
        IBenchmark service;
        if (isMultiClient) {
          	DefaultRefererHandler handler = new DefaultRefererHandler("benchmark-client.xml");
    		handler.initialize();
    		service = handler.get(IBenchmark.class);
        } else {
            service = benchmark;
        }

        Class[] parameterTypes = new Class[]{IBenchmark.class, String.class, CyclicBarrier.class,
                CountDownLatch.class, long.class, long.class};
        Object[] parameters = new Object[]{service, params, barrier, latch, startTime, endTime};

        ClientRunnable clientRunnable = null;
        try {
            clientRunnable = (ClientRunnable) Class.forName(classname).getConstructor(parameterTypes).newInstance(parameters);
        } catch (InstantiationException | NoSuchMethodException | ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.getTargetException();
        }

        return clientRunnable;
    }
    
    
    public static void main(String[] args) {
        loadProperties();
        int concurrents = Integer.parseInt(properties.getProperty("concurrents"));
        int runtime = Integer.parseInt(properties.getProperty("runtime"));
        String classname = properties.getProperty("classname");
        String params = properties.getProperty("params");
        isMultiClient = Boolean.parseBoolean(properties.getProperty("isMultiClient"));
        if (args.length == 5) {
            concurrents = Integer.parseInt(args[0]);
            runtime = Integer.parseInt(args[1]);
            classname = args[2];
            params = args[3];
            isMultiClient = Boolean.parseBoolean(args[4]);
        }

      	DefaultRefererHandler handler = new DefaultRefererHandler("benchmark-client.xml");
		handler.initialize();
		benchmark = handler.get(IBenchmark.class);

        new BenchmarkClient().start(concurrents, runtime, classname, params);
    }

}
