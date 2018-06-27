package com.fmgame.bolt.extension;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Service Provider Interface.扩展点接口的标识,是指一些提供给你继承、扩展，完成自定义功能的类、接口或者方法
 * 
 * @author luowei
 * @date 2017年11月2日 上午11:10:52
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface SPI {
	
	boolean singleton() default true;
}
