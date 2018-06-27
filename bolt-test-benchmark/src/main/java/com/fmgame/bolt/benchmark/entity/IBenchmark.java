package com.fmgame.bolt.benchmark.entity;

import java.util.List;
import java.util.Map;

/**
 * @author luowei
 * @date 2018年4月23日 下午6:06:46
 */
public interface IBenchmark {

	Object echoService(Object request);
	
	void emptyService();
	
	Map<Long, Integer> getUserTypes(List<Long> uids);
	
	long[] getLastStatusIds(long[] uids);
	
}
