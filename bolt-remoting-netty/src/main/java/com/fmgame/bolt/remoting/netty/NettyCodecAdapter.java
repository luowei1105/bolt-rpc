package com.fmgame.bolt.remoting.netty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
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
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;

/**
 * @author luowei
 * @date 2018年1月10日 上午10:45:24
 */
public class NettyCodecAdapter {

	private static final Logger logger = LoggerFactory.getLogger(NettyCodecAdapter.class);
	
	/** 统一资源定位符 */
    protected final URL url;
    /** 消息处理 */
    protected final MessageHandler handler;
    /** 加解密 */
	private final Codec codec;
	
    private final ChannelHandler encoder = new InternalEncoder();
    private final ChannelHandler decoder = new InternalDecoder();

	public NettyCodecAdapter(URL url, MessageHandler handler, Codec codec) {
		this.url = url;
		this.handler = handler;
		this.codec = codec;
	}
	
    public ChannelHandler getEncoder() {
        return encoder;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }
	
	private Response buildExceptionResponse(long requestId, Exception e) {
		DefaultResponse response = new DefaultResponse(requestId);
		response.setException(e);
		return response;
	}
	
    private class InternalEncoder extends MessageToMessageEncoder<Object> {

		@Override
		protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
			long requestId = getRequestId(msg);
			byte[] data = null;
			
			NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
			if (msg instanceof Response) {
				try {
					data = codec.encode(channel, msg);
				} catch (Exception e) {
					logger.error("NettyEncoder encode error, url=" + channel.getUrl().getUri(), e);
					Response response = buildExceptionResponse(requestId, e);
					data = codec.encode(channel, response);
				}
			} else {
				data = codec.encode(channel, msg);
			}
			
			ByteBuf byteBuf = ctx.channel().alloc().ioBuffer(Constants.CODEC_HEADER + data.length);
			byteBuf.writeShort(Constants.CODEC_MAGIC_TYPE);
			byteBuf.writeShort(getType(msg));
			byteBuf.writeLong(getRequestId(msg));
			byteBuf.writeInt(data.length);
			byteBuf.writeBytes(data);
			
			if (out != null)
				out.add(byteBuf);
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
   
    }

    private class InternalDecoder extends ByteToMessageDecoder {

    	@Override
    	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    		if (in.readableBytes() <= Constants.CODEC_HEADER)
    			return;
    		
    		in.markReaderIndex();
    		short type = in.readShort();
       		if (type != Constants.CODEC_MAGIC_TYPE) {
    			throw new ServiceException("NettyDecoder transport header not support, type: " + type);
    		}
    		byte messageType = (byte) in.readShort();
    		long requestId = in.readLong();
    		int dataLength = in.readInt();
    		if (in.readableBytes() < dataLength) {
    			in.resetReaderIndex();
    			return;
    		}
    		byte[] data = new byte[dataLength];
    		in.readBytes(data);
    		
    		try {
    			NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
    		    String remoteIp = getRemoteIp(ctx.channel());
    			Object obj = codec.decode(channel, remoteIp, data, messageType);
    			if (out != null)
    				out.add(obj);
    		} catch (Exception e) {
    			if (messageType == Constants.FLAG_REQUEST) {
    				Response resonse = buildExceptionResponse(requestId, e);
    				ctx.channel().writeAndFlush(resonse);
    			} else {
    				Response resonse = buildExceptionResponse(requestId, e);
    				if (out != null)
    					out.add(resonse);
    			}
    		}
    	}
    	
        private String getRemoteIp(Channel channel) {
            String ip = "";
            SocketAddress remote = channel.remoteAddress();
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
