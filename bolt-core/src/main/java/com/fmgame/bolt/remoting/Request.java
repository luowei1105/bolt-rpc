package com.fmgame.bolt.remoting;

import java.util.Map;

/**
 * rpc 请求
 * 
 * @author luowei
 * @date 2017年10月13日 下午3:37:53
 */
public interface Request {
	
    /**
     * 请求唯一序列号
     * 
     * @return
     */
    long getRequestId();

    /**
     * 服务对应接口名
     * 
     * @return
     */
    String getInterfaceName();

    /**
     * 服务对应方法名
     * 
     * @return
     */
    String getMethodName();
    
    /**
     * 服务对应方法参数描述(sign)
     * 
     * @return
     */
    String getParamtersDesc();
    
    /**
     * 服务对应方法参数
     * 
     * @return
     */
    Object[] getArguments();
    
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
