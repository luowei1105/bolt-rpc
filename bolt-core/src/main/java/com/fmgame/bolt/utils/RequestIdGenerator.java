package com.fmgame.bolt.utils;

/**
 * Id操作(id = time + node + sequence, 默认time39位，workerId(区)13位，序列12位)生成ID
 * 
 * @author luowei
 * @date 2017年10月23日 下午2:15:06
 */
public final class RequestIdGenerator {
	
	/**
	 * 起始时间戳
	 */
	private static final long twepoch = System.currentTimeMillis() - 100000;

	private static final long workerIdBits = 13L;

	private static final long maxWorkerId = -1L ^ (-1L << workerIdBits);

	private static final long sequenceBits = 11L;

	private static final long workerIdShift = sequenceBits;

	private static final long timestampLeftShift = sequenceBits + workerIdBits;

	private static final long sequenceMask = -1L ^ (-1L << sequenceBits);
	
	private long sequence = 0L;
	private long lastTimestamp = -1L;
	private final long workerId;
	
//	public volatile AtomicInteger testGeneratorNum = new AtomicInteger(0);
	
	private static final RequestIdGenerator INSTANCE = new RequestIdGenerator(0);

	public static long getRequestId() {
		return INSTANCE.nextId();
	}
	
	/**
	 * id = time + node + sequence, 默认time39位，结点(区)13位，序列12位，
	 * 即每ms可生成4096个不重复ID，如果满4096个序列，则强制从下一ms开始生成ID，保证ID不重复。可支撑8192个结点(区)，可用16年
	 */
	private RequestIdGenerator(final long workerId) {
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format(
					"worker Id can't be greater than %d or less than 0", maxWorkerId));
		}
		
		this.workerId = workerId;
	}
	
	/**
	 * 根据workerId值获取ID
	 * @param workerId ID或可以直接理解为区ID
	 * @return
	 */
	private synchronized long nextId()  {
	    long timestamp = timeGen();

		if (timestamp < this.lastTimestamp) {
			try {
				throw new Exception(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
						this.lastTimestamp - timestamp));
			} catch (Exception e) {
			//	ExceptionLogger.printLogger(e);
			}
		}

		if (lastTimestamp == timestamp) {
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == 0) {
				timestamp = tilNextMillis(lastTimestamp);
			}
		} else {
			sequence = 0;
		}

		lastTimestamp = timestamp;
		
//		this.testGeneratorNum.incrementAndGet();
		
		return ((timestamp - twepoch) << timestampLeftShift) | (workerId << workerIdShift)
				| sequence;
	}

	private long tilNextMillis(long lastTimestamp) {
		long timestamp = this.timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}
		return timestamp;
	}

	private long timeGen() {
		return System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "workerIdBits:" + workerIdBits
				+ ",sequenceBits:" + sequenceBits
				+ ",可支撑:" + (maxWorkerId + 1) +"个结点（区）"
				+ ",每ms生成:" + (sequenceMask + 1) +"个id"
				+ ",可使用:" + Math.pow(2, 64 - workerIdBits - sequenceBits);
	}
	
}
