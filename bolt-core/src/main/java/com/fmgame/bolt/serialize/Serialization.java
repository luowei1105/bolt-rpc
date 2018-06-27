package com.fmgame.bolt.serialize;

import java.io.IOException;
import com.fmgame.bolt.extension.SPI;

/**
 * 序列化. (SPI, Singleton, ThreadSafe)
 * 
 * @author luowei
 * @date 2017年10月23日 下午5:13:13
 */
@SPI
public interface Serialization {
	
	/**
	 * 序列化对象
	 * 
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	<T> byte[] serialize(T obj) throws IOException;

	/**
	 * 反序列化,生成对象
	 * 
	 * @param bytes
	 * @param clz
	 * @return
	 * @throws IOException
	 */
	<T> T deserialize(byte[] bytes, Class<T> clz) throws IOException;
	
}
