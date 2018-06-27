package com.fmgame.bolt.remoting;

/**
 * 用于监听Future的success和fail事件
 * 
 * @author luowei
 * @date 2017年10月19日 下午2:40:06
 */
@FunctionalInterface
public interface FutureListener {

    /**
     * <pre>
	 * 		建议做一些比较简单的低功耗的操作
	 * 		注意一些反模式： 
	 * 		1) 死循环： 
	 * 			operationComplete(Future future) {
	 * 					......
	 * 				future.addListener(this);  
	 * 					......
	 * 			}
	 * 
	 * 		2）耗资源操作或者慢操作：
	 * 			operationComplete(Future future) {
	 * 					......
	 * 				Thread.sleep(500); 
	 * 					......
	 * 			}
	 * 
	 * </pre>
     * 
     * @param future
     * @throws Exception
     */
    void operationComplete(ResponseFuture future) throws Exception;

}
