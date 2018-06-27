package com.fmgame.bolt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * api配置
 * @author luowei
 * @date 2018年1月6日 下午1:34:03
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfApiProtocol {
	
	/**
	 * 指定协议
	 * @return
	 */
	String value();
	
}
