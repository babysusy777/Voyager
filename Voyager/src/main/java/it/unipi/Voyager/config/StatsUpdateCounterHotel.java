package it.unipi.Voyager.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StatsUpdateCounterHotel {

    private final AtomicInteger hotelCounter = new AtomicInteger(0);

    private static final int BATCH_SIZE_HOTEL = 20;

    public boolean increment() {
        return hotelCounter.incrementAndGet() % BATCH_SIZE_HOTEL == 0;
    }
}