package com.fmgame.bolt.utils;

import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.common.URLParamType;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.URL;

/**
 * 协议工具类
 * 
 * @author luowei
 * @date 2017年10月30日 下午1:54:38
 */
public class ProtocolUtils {

    /**
     * 目前根据 group/interface 来唯一标示一个服务
     * 
     * @param request
     * @return
     */
    public static String getServiceKey(Request request) {
        String group = getGroupFromRequest(request);

        return getServiceKey(group, request.getInterfaceName());
    }
    
    private static String getGroupFromRequest(Request request) {
        return getValueFromRequest(request, URLParamType.GROUP.getName(), URLParamType.GROUP.getValue());
    }

    private static String getValueFromRequest(Request request, String key, String defaultValue) {
        String value = defaultValue;
        if (request.getAttachments() != null && request.getAttachments().containsKey(key)) {
            value = (String) request.getAttachments().get(key);
        }
        return value;
    }
	
    /**
     * 目前根据 group/interface/ 来唯一标示一个服务
     *
     * @param url
     * @return
     */
    public static String getServiceKey(URL url) {
        return getServiceKey(url.getGroup(), url.getPath());
    }
    
    /**
     * serviceKey: host:port/group/interface/
     * 
     * @param group
     * @param interfaceName
     * @param version
     * @return
     */
    public static String getServiceKey(String address, String group, String interfaceName) {
        return address + Constants.PATH_SEPARATOR + group + Constants.PATH_SEPARATOR + interfaceName;
    }
    
    /**
     * serviceKey: group/interface
     * 
     * @param group
     * @param interfaceName
     * @param version
     * @return
     */
    public static String getServiceKey(String group, String interfaceName) {
        return group + Constants.PATH_SEPARATOR + interfaceName;
    }

    /**
     * protocol key: protocol://host:port/group/interface/
     * 
     * @param url
     * @return
     */
    public static String getProtocolKey(URL url) {
        return url.getProtocol() + Constants.PROTOCOL_SEPARATOR + url.getAddress() + Constants.PATH_SEPARATOR
                + url.getGroup() + Constants.PATH_SEPARATOR + url.getPath();
    }
    
}
