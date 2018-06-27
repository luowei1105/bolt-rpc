package com.fmgame.bolt.remoting.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.fmgame.bolt.remoting.Request;

/**
 * 默认rpc request
 * 
 * @author luowei
 * @date 2017年10月17日 下午7:26:56
 */
public class DefaultRequest implements Request {
	
	/** 请求id */
	private long requestId;
	/** 接口名 */
	private String interfaceName;
	/** 方法名 */
	private String methodName;
	/** 参数描述 */
	private String paramtersDesc;
	/** 参数列表 */
	private Object[] arguments;
	/** 附加参数 */
	private Map<String, Object> attachments;

	@Override
	public long getRequestId() {
		return requestId;
	}

	@Override
	public String getInterfaceName() {
		return interfaceName;
	}

	@Override
	public String getMethodName() {
		return methodName;
	}

	@Override
	public String getParamtersDesc() {
		return paramtersDesc;
	}

	@Override
	public Object[] getArguments() {
		return arguments;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getAttachments() {
		return attachments != null ? attachments : Collections.EMPTY_MAP;
	}

	@Override
	public void setAttachment(String key, Object value) {
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }

        this.attachments.put(key, value);
	}

	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void setParamtersDesc(String paramtersDesc) {
		this.paramtersDesc = paramtersDesc;
	}

	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}

	public void setAttachments(Map<String, Object> attachments) {
		this.attachments = attachments;
	}
	
	@Override
    public String toString() {
        return interfaceName + "." + methodName + "(" + paramtersDesc + ") requestId=" + requestId;
    }

}
