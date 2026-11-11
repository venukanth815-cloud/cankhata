package com.cankhata.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsTest {

    @Test
    void latencyBucketsAreCorrect() {
        assertEquals(0, Metrics.bucket(10));
        assertEquals(1, Metrics.bucket(50));     // 50 ms is NOT under 50, so next bucket
        assertEquals(3, Metrics.bucket(300));
        assertEquals(6, Metrics.bucket(99_999)); // slowest bucket
    }
}
