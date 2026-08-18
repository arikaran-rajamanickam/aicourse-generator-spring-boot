package com.aicourse.utils.id;

public class SnowflakeIdGenerator {
    private static final long EPOCH = 1609459200000L; // Jan 1, 2021
    private static final long MACHINE_ID = 1L;
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    public synchronized static long generateId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095;
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << 22)
                | (MACHINE_ID << 12)
                | sequence;
    }
}
