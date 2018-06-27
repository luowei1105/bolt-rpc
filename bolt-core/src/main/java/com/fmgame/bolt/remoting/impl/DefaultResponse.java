package com.fmgame.bolt.remoting.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.fmgame.bolt.remoting.Response;

/**
 * Response received rpc.
 * 
 * @author luowei
 * @date 2017年10月17日 下午7:33:43
 */
public class DefaultResponse implements Response {
	
	/** 请求id */
	private long requestId;
	/** 处理时间 */
	private long processTime;
	/** 额外参数 */
	private Map<String, Object> attachments;
	/** 返回值 */
	private Object resultValue;
	/** 异常 */
	private Exception exception;
	
	public DefaultResponse() {
		
	}
	
    public DefaultResponse(long requestId) {
        this.requestId = requestId;
    }
    
    public DefaultResponse(Object value) {
        this.resultValue = value;
    }
    
    public DefaultResponse(long requestId, Object value) {
    	 this.requestId = requestId;
    	 this.resultValue = value;
    }
    
    public DefaultResponse(Response response) {
    	this.requestId = response.getRequestId();
        this.resultValue = response.getResult();
        this.exception = response.getException();
        this.processTime = response.getProcessTime();
    }

	@Override
	public long getRequestId() {
		return requestId;
	}

	@Override
	public long getProcessTime() {
		return processTime;
	}

	@Override
	public void setProcessTime(long time) {
		this.processTime = time;
	}

	@Override
	public Object getResult() {
        if (exception != null) {
            throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RuntimeException(
                    exception.getMessage(), exception);
        }

        return resultValue;
	}

	@Override
	public Exception getException() {
		return exception;
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

	public void setValue(Object value) {
		this.resultValue = value;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public void setAttachments(Map<String, Object> attachments) {
		this.attachments = attachments;
	}

	@Override
	public String toString() {
		return "requestId=" + requestId + ", resultValue=" + resultValue + ", processTime=" + processTime;
	}
	
}
