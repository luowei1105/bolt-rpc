package com.fmgame.bolt.cluster.loadbalance;

import java.util.List;
import java.util.Random;
import com.fmgame.bolt.cluster.AbstractLoadBalance;
import com.fmgame.bolt.remoting.Request;
import com.fmgame.bolt.rpc.Invoker;
import com.fmgame.bolt.rpc.URL;

/**
 * 随机负载均衡
 * 
 * @author luowei
 * @date 2018年4月4日 下午12:38:38
 */
public class RandomLoadBalance extends AbstractLoadBalance {

	public static final String NAME = "random";

	private final Random random = new Random();

	@Override
	protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Request request) {
		int length = invokers.size(); // 总个数
		int totalWeight = 0; // 总权重
		boolean sameWeight = true; // 权重是否都一样
		for (int i = 0; i < length; i++) {
			int weight = getWeight(invokers.get(i), request);
			totalWeight += weight; // 累计总权重
			if (sameWeight && i > 0 && weight != getWeight(invokers.get(i - 1), request)) {
				sameWeight = false; // 计算所有权重是否一样
			}
		}
		if (totalWeight > 0 && !sameWeight) {
			// 如果权重不相同且权重大于0则按总权重数随机
			int offset = random.nextInt(totalWeight);
			// 并确定随机值落在哪个片断上
			for (int i = 0; i < length; i++) {
				offset -= getWeight(invokers.get(i), request);
				if (offset < 0) {
					return invokers.get(i);
				}
			}
		}
		// 如果权重相同或权重为0则均等随机
		return invokers.get(random.nextInt(length));
	}

}
