package com.cankhata.util;

import com.zaxxer.hikari.HikariPoolMXBean;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Numbers for the /metrics page: traffic, latency, memory, CPU, threads, GC and database pool.
 * Everything here is cheap (a few atomic counters) so it is safe on every request.
 */
public final class Metrics {
    /** Latency buckets in milliseconds; the last bucket is "slower than 2500". */
    static final long[] BOUNDS = {50, 100, 250, 500, 1000, 2500};

    private static final AtomicLong requests = new AtomicLong();
    private static final AtomicLong errors4xx = new AtomicLong();
    private static final AtomicLong errors5xx = new AtomicLong();
    private static final AtomicLong totalMs = new AtomicLong();
    private static final AtomicLong slow = new AtomicLong();
    private static final AtomicLongArray latency = new AtomicLongArray(BOUNDS.length + 1);
    private static final AtomicLongArray secCount = new AtomicLongArray(60);
    private static final AtomicLongArray secStamp = new AtomicLongArray(60);
    private static final long START = System.currentTimeMillis();

    private Metrics() {}

    public static void record(int status, long millis) {
        requests.incrementAndGet();
        totalMs.addAndGet(millis);
        if (status >= 500) errors5xx.incrementAndGet();
        else if (status >= 400) errors4xx.incrementAndGet();
        if (millis > 1000) slow.incrementAndGet();
        latency.incrementAndGet(bucket(millis));

        long sec = System.currentTimeMillis() / 1000;
        int i = (int) (sec % 60);
        if (secStamp.get(i) != sec) { // a new second: reuse the slot (tiny race is acceptable for a counter)
            secStamp.set(i, sec);
            secCount.set(i, 0);
        }
        secCount.incrementAndGet(i);
    }

    static int bucket(long ms) {
        for (int i = 0; i < BOUNDS.length; i++) {
            if (ms < BOUNDS[i]) return i;
        }
        return BOUNDS.length;
    }

    /** Approximate 95th percentile: the upper edge of the bucket where 95% of requests are already counted. */
    public static long p95Ms() {
        long total = 0;
        for (int i = 0; i < latency.length(); i++) total += latency.get(i);
        if (total == 0) return 0;
        long running = 0;
        for (int i = 0; i < latency.length(); i++) {
            running += latency.get(i);
            if (running * 100 >= total * 95) return i < BOUNDS.length ? BOUNDS[i] : BOUNDS[BOUNDS.length - 1] * 2;
        }
        return 0;
    }

    static long lastMinute() {
        long now = System.currentTimeMillis() / 1000, sum = 0;
        for (int i = 0; i < 60; i++) {
            if (now - secStamp.get(i) < 60) sum += secCount.get(i);
        }
        return sum;
    }

    public static Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        long n = requests.get();

        Map<String, Object> traffic = new LinkedHashMap<>();
        traffic.put("requestsTotal", n);
        traffic.put("requestsLastMinute", lastMinute());
        traffic.put("avgMs", n == 0 ? 0 : totalMs.get() / n);
        traffic.put("p95MsApprox", p95Ms());
        traffic.put("slowOver1s", slow.get());
        traffic.put("errors4xx", errors4xx.get());
        traffic.put("errors5xx", errors5xx.get());
        m.put("traffic", traffic);

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("heapUsedMb", mem.getHeapMemoryUsage().getUsed() >> 20);
        memory.put("heapMaxMb", mem.getHeapMemoryUsage().getMax() >> 20);
        memory.put("nonHeapUsedMb", mem.getNonHeapMemoryUsage().getUsed() >> 20);
        m.put("memory", memory);

        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("cores", Runtime.getRuntime().availableProcessors());
        cpu.put("loadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
            cpu.put("processCpuPercent", Math.round(sun.getProcessCpuLoad() * 1000) / 10.0);
            cpu.put("systemCpuPercent", Math.round(sun.getCpuLoad() * 1000) / 10.0);
        }
        m.put("cpu", cpu);

        ThreadMXBean th = ManagementFactory.getThreadMXBean();
        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("live", th.getThreadCount());
        threads.put("peak", th.getPeakThreadCount());
        m.put("threads", threads);

        long gcCount = 0, gcMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(0, gc.getCollectionCount());
            gcMs += Math.max(0, gc.getCollectionTime());
        }
        Map<String, Object> gc = new LinkedHashMap<>();
        gc.put("collections", gcCount);
        gc.put("totalPauseMs", gcMs);
        m.put("gc", gc);

        Map<String, Object> db = new LinkedHashMap<>();
        HikariPoolMXBean pool = DbConnection.pool();
        if (pool != null) {
            db.put("active", pool.getActiveConnections());
            db.put("idle", pool.getIdleConnections());
            db.put("total", pool.getTotalConnections());
            db.put("waitingThreads", pool.getThreadsAwaitingConnection());
            db.put("max", DbConnection.maxPoolSize());
        }
        m.put("dbPool", db);

        m.put("uptimeSeconds", (System.currentTimeMillis() - START) / 1000);
        return m;
    }
}
