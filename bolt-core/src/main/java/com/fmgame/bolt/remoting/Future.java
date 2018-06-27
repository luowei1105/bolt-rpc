package com.fmgame.bolt.remoting;

/**
 * Future 表示异步计算的结果. (API/SPI, Prototype, ThreadSafe)
 * <p>
 * 它提供了检查计算是否完成的方法，以等待计算的完成，并获取计算的结果。
 * 计算完成后只能使用 get 方法来获取结果，如有必要，计算完成前可以阻塞此方法。
 * 
 * @author luowei
 * @date 2017年10月19日 下午2:40:44
 */
public interface Future {

    /**
     * get result.
     *
     * @return result.
     */
    Object get() throws RemotingException;

    /**
     * get result with the specified timeout.
     *
     * @param timeoutInMillis timeout.
     * @return result.
     */
    Object get(int timeoutInMillis) throws RemotingException;

    /**
     * check is done.
     *
     * @return done or not.
     */
    boolean isDone();
    
    /**
     * add future listener , when task is success，failure, timeout, cancel, it will be called
     * 
     * @param listener
     */
    void addListener(FutureListener listener);
	
}
