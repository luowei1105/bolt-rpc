package com.fmgame.bolt.benchmark.entity;

import java.util.List;
import java.util.Map;

/**
 * @author luowei
 * @date 2018年4月23日 下午6:07:07
 */
public class BenchmarkImpl implements IBenchmark {

	@Override
	public Object echoService(Object request) {
		return request;
	}

	@Override
	public void emptyService() {
	}

	@Override
	public Map<Long, Integer> getUserTypes(List<Long> uids) {
		return null;
	}

	@Override
	public long[] getLastStatusIds(long[] uids) {
		return new long[0];
	}
}
