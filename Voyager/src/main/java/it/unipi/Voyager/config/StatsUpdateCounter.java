package it.unipi.Voyager.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StatsUpdateCounter {
    private final AtomicInteger tripCounter = new AtomicInteger(0);
    private static final int BATCH_SIZE = 100;

    // Ritorna true se si è raggiunto il threshold
    public boolean increment() {
        return tripCounter.incrementAndGet() % BATCH_SIZE == 0;
    }
}
