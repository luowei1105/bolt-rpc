# bolt
用于快速开发高性能分布式服务的远程过程调用(RPC)框架。

# 简介
* 参考dubbo进行设计，简化版rpc框架。
* 支持netty4或者mina进行网络通讯
* 支持zookeeper对服务进行管理
* 支持protostuff或者json对数据进行序列化
* 支持服务集群

# Quick Start
1. Add dependencies to pom.<br>
```java
  <dependency>
      <groupId>com.weibo</groupId>
      <artifactId>motan-core</artifactId>
      <version>1.0.0</version>
  </dependency>
```
2. 创建一个服务提供者和服务消费者需要引用接口
```java
package com.fmgame.bolt.demo;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:24:07
 */
public interface IHello {

	/**
	 * 同步调用
	 * @return
	 */
	String sayHello();
	
	/**
	 * 异步调用，所有异步调用方法名后面必须带上<p>Async</p>
	 * @return
	 */
	String sayHelloAsync();
	
}

```
3. 服务提供方实现接口
```java
package com.fmgame.bolt.demo;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:24:17
 */
public class HelloImpl implements IHello {

	@Override
	public String sayHello() {
		return "i am a rpc framework for bolt";
	}

	@Override
	public String sayHelloAsync() {
		return "async";
	}

}

```
4. 定义rpc服务启动配置文件
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xml>
<provider>
	
	<!-- 注册中心配置 使用不同注册中心需要依赖对应的jar包。-->
   	<registry name="local" address="127.0.0.1:0" /> -->
    <!-- <registry name="zookeeper" address="127.0.0.1:2181" connectTimeout="2000" /> -->

 	<!-- 协议配置 -->
	<protocol id="demo1" name="default" port="18080" />
   
 	<!-- 具体rpc服务配置，声明实现的接口类。-->
	<service interface="com.fmgame.bolt.demo.IHello" impl="com.fmgame.bolt.demo.HelloImpl" />

</provider>
```
5. 启动服务提供者(RPC Server)
```java
package com.fmgame.bolt.demo;

import com.fmgame.bolt.config.impl.DefaultProviderHander;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:23:00
 */
public class Provider {

	public static void main(String[] args) {
		DefaultProviderHander handler = new DefaultProviderHander("bolt_demo_provider.xml");
		handler.initialize();
          // 阻止JVM退出
          synchronized (DefaultProviderHander.class) {
              while (true) {
                  try {
                    DefaultProviderHander.class.wait();
                  } catch (InterruptedException e) {
                  }
              }
          }
	}
	
}

```
6. 创建服务消费者配置文件
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xml>
<provider>
	
	<!-- 注册中心配置 使用不同注册中心需要依赖对应的jar包。-->
   	<registry name="local" address="127.0.0.1:0" /> -->
    <!-- <registry name="zookeeper" address="127.0.0.1:2181" connectTimeout="2000" /> -->

 	<!-- 协议配置 -->
	<protocol id="demo1" name="default" port="18080" />
   
 	<!-- 具体rpc服务配置，声明实现的接口类。-->
	<service interface="com.fmgame.bolt.demo.IHello" impl="com.fmgame.bolt.demo.HelloImpl" />

</provider>
```
7. 启动服务器消费者(RPC client)
```java
package com.fmgame.bolt.demo;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import com.fmgame.bolt.config.impl.DefaultRefererHandler;
import com.fmgame.bolt.remoting.ResponseFuture;
import com.fmgame.bolt.remoting.impl.FutureListenerWrapper;
import com.fmgame.bolt.rpc.RpcContext;

/**
 * @author luowei
 * @date 2017年10月30日 下午4:27:58
 */
public class Refer {

	public static void main(String[] args) {
		DefaultRefererHandler handler = new DefaultRefererHandler("bolt_demo_referer.xml");
		handler.initialize();
		
		IHello hello = handler.get(IHello.class);
		
		// 同步执行10次
		int count = 10;
		long now = System.currentTimeMillis();
		final CountDownLatch latch = new CountDownLatch(count);
		IntStream.range(0, count).parallel().forEach(action -> {
			System.out.println(hello.sayHello());
			latch.countDown();
		});
		
		try {
			latch.await();
			System.out.println("总执行时间：" + (System.currentTimeMillis() - now) + "ms");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// 异步执行
		RpcContext.getContext().asyncCall(hello::sayHelloAsync, new FutureListenerWrapper("player", 1) {

			@Override
			public void operationComplete(ResponseFuture future) throws Exception {
				int ctx = this.getParams().getIntValue("player");
				System.out.println(future.get() + "-----" + ctx);
			}
			
		});
		

	}
	
}
```
