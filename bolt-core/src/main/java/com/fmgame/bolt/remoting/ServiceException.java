package com.fmgame.bolt.remoting;

/**
 * Service exception
 * 
 * @author luowei
 * @date 2017年10月23日 下午4:41:15
 */
public class ServiceException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String errorMsg = null;

	public ServiceException() {
		super();
	}

	public ServiceException(String message) {
		super(message);
		this.errorMsg = message;
	}

	public ServiceException(String message, Throwable cause) {
		super(message, cause);
		this.errorMsg = message;
	}

	public ServiceException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getMessage() {
		return errorMsg;
	}
	
}
