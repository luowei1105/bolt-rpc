package com.fmgame.bolt.codec;

import java.io.IOException;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.remoting.Channel;
import com.fmgame.bolt.remoting.impl.DefaultRequest;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.serialize.ProtostuffSerialization;

/**
 * 默认rpc加解码
 * 
 * @author luowei
 * @date 2017年10月23日 下午5:14:34
 */
public class DefaultRPCCodec implements Codec {
	
	private static final ProtostuffSerialization serialization = new ProtostuffSerialization();

	@Override
	public byte[] encode(Channel channel, Object message) throws IOException {
		return serialization.serialize(message);
	}

	@Override
	public Object decode(Channel channel, String remoteIp, byte[] buffer, byte messageType) throws IOException {
		Class<?> clazz = messageType == Constants.FLAG_REQUEST ? DefaultRequest.class : DefaultResponse.class;
		return serialization.deserialize(buffer, clazz);
	}
	
}
