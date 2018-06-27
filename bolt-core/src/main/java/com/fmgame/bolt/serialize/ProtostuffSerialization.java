package com.fmgame.bolt.serialize;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * protostuff 序列化
 * 
 * @author luowei
 * @date 2017年10月23日 下午5:45:39
 */
public class ProtostuffSerialization implements Serialization {
	
	private static final Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

	@Override
	public <T> byte[] serialize(T obj) throws IOException {
		// this is lazily created and cached by RuntimeSchema
		// so its safe to call RuntimeSchema.getSchema(Foo.class) over and over
		// The getSchema method is also thread-safe
		@SuppressWarnings("unchecked")
		Schema<T> schema = (Schema<T>) getSchema(obj.getClass());
		LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

		/* -------- protostuff -------- (requires protostuff-core module) */
		// ser
		byte[] protostuff = null;
		try {
			protostuff = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
		} finally {
			buffer.clear();
		}

		return protostuff;
	}

	@Override
	public <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException {
		Schema<T> schema = getSchema(clz);
		// deser
		T obj = schema.newMessage();
		ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
		
		return obj;
	}
	
	/**
	 * 获取模式
	 * @param cls
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static <T> Schema<T> getSchema(Class<T> cls) {
		Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
		if (schema == null) {
			schema = RuntimeSchema.getSchema(cls);
			final Schema<T> existing = (Schema<T>) cachedSchema.putIfAbsent(cls, schema);
			if (existing != null)
				schema = existing;
		}

		return schema;
	}

}
