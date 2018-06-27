package com.fmgame.bolt.remoting.mina;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.codec.Codec;
import com.fmgame.bolt.common.Constants;
import com.fmgame.bolt.remoting.MessageHandler;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.remoting.Response;
import com.fmgame.bolt.remoting.ServiceException;
import com.fmgame.bolt.remoting.impl.DefaultResponse;
import com.fmgame.bolt.rpc.URL;

/**
 * mina 加解码
 * 
 * @author luowei
 * @date 2017年10月26日 下午4:27:07
 */
public class MinaCodecAdapter implements ProtocolCodecFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(MinaCodecAdapter.class);
	
	/** 统一资源定位符 */
    protected final URL url;
    /** 消息处理 */
    protected final MessageHandler handler;
    /** 加解密 */
	private final Codec codec;
	
    private final ProtocolEncoder encoder = new InternalEncoder();
    private final ProtocolDecoder decoder = new InternalDecoder();

	public MinaCodecAdapter(URL url, MessageHandler handler, Codec codec) {
		this.url = url;
		this.handler = handler;
		this.codec = codec;
	}

	@Override
	public ProtocolEncoder getEncoder(IoSession session) throws Exception {
		return encoder;
	}

	@Override
	public ProtocolDecoder getDecoder(IoSession session) throws Exception {
		return decoder;
	}
	
    private class InternalEncoder extends ProtocolEncoderAdapter {

		@Override
		public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
			long requestId = getRequestId(message);
			byte[] data = null;
			
			MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
			if (message instanceof Response) {
				try {
					data = codec.encode(channel, message);
				} catch (Exception e) {
					logger.error("MinaEncoder encode error, url=" + channel.getUrl().getUri(), e);
					Response response = buildExceptionResponse(requestId, e);
					data = codec.encode(channel, response);
				}
			} else {
				data = codec.encode(channel, message);
			}
			
			IoBuffer buffer = IoBuffer.allocate(Constants.CODEC_HEADER + data.length);
			buffer.putShort(Constants.CODEC_MAGIC_TYPE);
			buffer.putShort(getType(message));
			buffer.putLong(getRequestId(message));
			buffer.putInt(data.length);
			buffer.put(data);
			buffer.flip();
			
			out.write(buffer);
		}
		
		private long getRequestId(Object message) {
			if (message instanceof Request) {
				return ((Request) message).getRequestId();
			} else if (message instanceof Response) {
				return ((Response) message).getRequestId();
			} else {
				throw new IllegalArgumentException("message is not request or response");
			}
		}
		
		private byte getType(Object message) {
			if (message instanceof Request) {
				return Constants.FLAG_REQUEST;
			} else if (message instanceof Response) {
				return Constants.FLAG_RESPONSE;
			} else {
				throw new IllegalArgumentException("message is not request or response");
			}
		}
		
		private Response buildExceptionResponse(long requestId, Exception e) {
			DefaultResponse response = new DefaultResponse(requestId);
			response.setException(e);
			return response;
		}
   
    }

    private class InternalDecoder extends CumulativeProtocolDecoder {

        @Override
		protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
    		if (in.remaining() <= Constants.CODEC_HEADER)
    			return false;
    		
    		in.mark();
    		short type = in.getShort();
    		if (type != Constants.CODEC_MAGIC_TYPE) {
    			throw new ServiceException("MinaDecoder transport header not support, type: " + type);
    		}
    		
    		byte messageType = (byte) in.getShort();
    		long requestId = in.getLong();
    		int dataLength = in.getInt();
    		if (in.remaining() < dataLength) {
    			in.reset();
    			return false;
    		}
    		
    		byte[] data = new byte[dataLength];
    		in.get(data);
    		try {
    			MinaChannel channel = MinaChannel.getOrAddChannel(session, url, handler);
    		    String remoteIp = getRemoteIp(session);
    			Object obj = codec.decode(channel, remoteIp, data, messageType);
    			out.write(obj);
    			
    			return true;
    		} catch (Exception e) {
    			if (messageType == Constants.FLAG_REQUEST) {
    				Response resonse = buildExceptionResponse(requestId, e);
    				session.write(resonse);
    			} else {
    				Response resonse = buildExceptionResponse(requestId, e);
    				out.write(resonse);
    			}
    		}
    		return false;
		}

    	private Response buildExceptionResponse(long requestId, Exception e) {
    		DefaultResponse response = new DefaultResponse(requestId);
    		response.setException(e);
    		return response;
    	}
    	
        private String getRemoteIp(IoSession session) {
            String ip = "";
            SocketAddress remote = session.getRemoteAddress();
            if (remote != null) {
                try {
                    ip = ((InetSocketAddress) remote).getAddress().getHostAddress();
                } catch (Exception e) {
                    logger.warn("get remoteIp error!dedault will use. msg:" + e.getMessage() + ", remote:" + remote.toString());
                }
            }
            return ip;
        }
    }

}
