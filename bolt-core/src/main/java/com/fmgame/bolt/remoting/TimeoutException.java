package com.fmgame.bolt.remoting;

import java.net.InetSocketAddress;

/**
 * 超时异常. (API, Prototype, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月24日 下午2:06:06
 */
public class TimeoutException extends RemotingException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final int CLIENT_SIDE = 0;
    public static final int SERVER_SIDE = 1;
    private final int phase;

    public TimeoutException(boolean serverSide, Channel channel, String message) {
        super(channel, message);
        this.phase = serverSide ? SERVER_SIDE : CLIENT_SIDE;
    }

    public TimeoutException(boolean serverSide, InetSocketAddress localAddress,
                            InetSocketAddress remoteAddress, String message) {
        super(localAddress, remoteAddress, message);
        this.phase = serverSide ? SERVER_SIDE : CLIENT_SIDE;
    }

    public int getPhase() {
        return phase;
    }

    public boolean isServerSide() {
        return phase == 1;
    }

    public boolean isClientSide() {
        return phase == 0;
    }

}