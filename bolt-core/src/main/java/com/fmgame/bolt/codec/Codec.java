package com.fmgame.bolt.codec;

import java.io.IOException;
import com.fmgame.bolt.extension.SPI;
import com.fmgame.bolt.remoting.Channel;

/**
 * 加解码
 * 
 * @author luowei
 * @date 2017年10月16日 下午4:26:51
 */
@SPI
public interface Codec {

	/**
	 * 加码
	 * 
	 * @param channel
	 * @param message
	 * @return
	 * @throws IOException
	 */
	byte[] encode(Channel channel, Object message) throws IOException;

	/**
	 * 解码
	 * 
	 * @param channel
	 * @param remoteIp 用来在server端decode request时能获取到client的ip。
	 * @param buffer
	 * @param messageType
	 * @return
	 * @throws IOException
	 */
	Object decode(Channel channel, String remoteIp, byte[] buffer, byte messageType) throws IOException;
	
}
