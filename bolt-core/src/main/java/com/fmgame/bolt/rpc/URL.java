package com.fmgame.bolt.rpc;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.common.URLParamType;

/**
 * 类 URL 代表一个统一资源定位符.(Immutable, ThreadSafe)
 * <p>
 * 它是指向互联网“资源”的指针.如：
 * <ul>
 * <li>bolt://127.0.0.1/group/com.fmgame.test.IHello
 * <li>registry://192.168.1.7:9090/group/com.fmgame.test.IHello?param1=value1&amp;param2=value2
 * </ul>
 * 
 * @author luowei
 * @date 2017年10月13日 下午3:32:22
 */
public class URL {

	/** 协议 */
	private String protocol;
	/** 服务器地址 */
	private String host;
	/** 指定端口 */
	private int port;
	/** 服务接口名 */
    private String path;
    /** 指定参数 */
    private Map<String, String> parameters;
    /** 参数解析 */
	private volatile transient Map<String, Number> numbers;
	
    public URL(String protocol, String host, int port, String path) {
        this(protocol, host, port, path, new HashMap<String, String>());
    }
    
    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        // trim the beginning "/"
        while (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        this.path = path;
        // 设置参数列表
        if (parameters == null) {
            parameters = new HashMap<String, String>();
        } else {
            parameters = new HashMap<String, String>(parameters);
        }
        this.parameters = Collections.unmodifiableMap(parameters);
    }
    
    public String getProtocol() {
		return protocol;
	}

	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 获取地址.格式(host:post)
	 * @return
	 */
    public String getAddress() {
        return port <= 0 ? host : host + ":" + port;
    }
	
	/**
	 * 获取所属组
	 * 
	 * @return
	 */
    public String getGroup() {
        return getParameter(URLParamType.GROUP.getName(), URLParamType.GROUP.getValue());
    }

	public String getPath() {
		return path;
	}
	
    public String getUri() {
        return this.getProtocol() + Constants.PROTOCOL_SEPARATOR + this.getAddress();
    }
	
	/********************************  获取参数 ***********************************************/
    
    public String getParameterJson() {
        JSONObject jsonObject = new JSONObject(this.parameters.size());
	    jsonObject.putAll(this.parameters);
	    return jsonObject.toJSONString();
    }
	
	public Map<String, String> getParameters() {
		return parameters;
	}
	
    public String getParameter(String name) {
        return parameters.get(name);
    }
    
    public String getParameter(String name, String defaultValue) {
        String value = getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
	
    public boolean getBooleanParameter(String name, boolean defaultValue) {
        String value = getParameter(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }
    
    public int getIntParameter(String name, int defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.intValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(name, i);
        return i;
    }
    
    public long getLongParameter(String name, long defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.longValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        long l = Long.parseLong(value);
        getNumbers().put(name, l);
        return l;
    }
    
    public float getFloatParameter(String name, float defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.floatValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        float f = Float.parseFloat(value);
        getNumbers().put(name, f);
        return f;
    }
    
    private Map<String, Number> getNumbers() {
        if (numbers == null) { // 允许并发重复创建
            numbers = new ConcurrentHashMap<String, Number>();
        }
        return numbers;
    }
    
    /********************************  其他接口 ***********************************************/

	public URL createCopy() {
        Map<String, String> params = new HashMap<String, String>();
        if (this.parameters != null) {
            params.putAll(this.parameters);
        }

        return new URL(protocol, host, port, path, params);
    }
	
	public InetSocketAddress toInetSocketAddress() {
		return new InetSocketAddress(host, port);
	}
	
	/**
	 * 指定格式URL字符串转换为URL对象 
	 * 
	 * @param url
	 * @return
	 */
	public static URL valueOf(String url) {
		if (StringUtils.isBlank(url)) 
			throw new IllegalArgumentException("url is null");
		
		String protocol = null;
		String host = null;
		int port = 0;
		String path = null;
		Map<String, String> parameters = new HashMap<String, String>();
		
		int i = url.indexOf("?"); // seperator between body and parameters
		if (i >= 0) {
			String[] parts = url.substring(i + 1).split("\\&");

			for (String part : parts) {
				part = part.trim();
				if (part.length() > 0) {
					int j = part.indexOf('=');
					if (j >= 0) {
						parameters.put(part.substring(0, j), part.substring(j + 1));
					} else {
						parameters.put(part, part);
					}
				}
			}
			url = url.substring(0, i);
		}
		i = url.indexOf("://");
		if (i >= 0) {
			if (i == 0)
				throw new IllegalStateException("url missing protocol: \"" + url + "\"");
			protocol = url.substring(0, i);
			url = url.substring(i + 3);
		} else {
			i = url.indexOf(":/");
			if (i >= 0) {
				if (i == 0)
					throw new IllegalStateException("url missing protocol: \"" + url + "\"");
				protocol = url.substring(0, i);
				url = url.substring(i + 1);
			}
		}

		i = url.indexOf("/");
		if (i >= 0) {
			path = url.substring(i + 1);
			url = url.substring(0, i);
		}

		i = url.indexOf(":");
		if (i >= 0 && i < url.length() - 1) {
			port = Integer.parseInt(url.substring(i + 1));
			url = url.substring(0, i);
		}
		if (url.length() > 0)
			host = url;
		return new URL(protocol, host, port, path, parameters);
	}

	@Override
	public String toString() {
		return this.getProtocol() + Constants.PROTOCOL_SEPARATOR + this.getAddress() 
				+ Constants.PATH_SEPARATOR + this.getGroup() 
				+ Constants.PATH_SEPARATOR + this.getPath();
	}
	
}
