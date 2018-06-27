package com.fmgame.bolt.remoting;

import java.util.Map;

/**
 * rpc请求给予响应
 * 
 * @author luowei
 * @date 2017年10月13日 下午3:38:03
 */
public interface Response {

    /**
     * 与 Request 的 requestId 相对应
     * 
     * @return
     */
    long getRequestId();
    
    /**
     * 业务处理时间
     * 
     * @return
     */
    long getProcessTime();
    
    /**
     * 业务处理时间
     * 
     * @param time
     */
    void setProcessTime(long time);

    /**
     * <pre>
	 * 		如果 request 正常处理，那么会返回 Object value，而如果 request 处理有异常，那么 getResult 会抛出异常
	 * </pre>
     * 
     * @throws RuntimeException
     * @return
     */
    Object getResult();
    
    /**
     * 如果request处理有异常，那么调用该方法return exception 如果request还没处理完或者request处理正常，那么return null
     * 
     * <pre>
	 * 		该方法不会阻塞，无论该request是处理中还是处理完成
	 * </pre>
     * 
     * @return
     */
    Exception getException();
    
    /**
     * get framework param
     * 
     * @return
     */
    Map<String, Object> getAttachments();

    /**
     * set framework param
     * 
     * @param key
     * @param value
     */
    void setAttachment(String key, Object value);
    
}
