package com.fmgame.bolt.serialize;

import java.io.IOException;
import java.nio.charset.Charset;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * fastjson 序列化
 * 
 * @author luowei
 * @date 2017年10月23日 下午5:30:42
 */
public class FastJSONSerialization implements Serialization {
	
	private final static Charset UTF8 = Charset.forName("UTF-8");

	@Override
	public <T> byte[] serialize(T obj) throws IOException {
        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = new JSONSerializer(out);
        serializer.config(SerializerFeature.WriteEnumUsingToString, true);
        serializer.config(SerializerFeature.WriteClassName, true);
        serializer.write(obj);
        
        return out.toBytes(UTF8);
	}

	@Override
	public <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException {
		return JSON.parseObject(new String(bytes, UTF8), clz);
	}

}
