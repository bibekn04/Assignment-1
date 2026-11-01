package edu.sezg534.ratelimiter.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Deterministic mutable clock for unit testing token bucket math without sleep or jitter.
 */
public class MutableClock extends Clock {

    private Instant currentInstant;
    private final ZoneId zone;

    public MutableClock(Instant initialInstant) {
        this(initialInstant, ZoneOffset.UTC);
    }

    public MutableClock(Instant initialInstant, ZoneId zone) {
        this.currentInstant = initialInstant;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(this.currentInstant, zone);
    }

    @Override
    public Instant instant() {
        return currentInstant;
    }

    public synchronized void setInstant(Instant newInstant) {
        this.currentInstant = newInstant;
    }

    public synchronized void advance(Duration duration) {
        this.currentInstant = this.currentInstant.plus(duration);
    }

    public synchronized void advanceSeconds(long seconds) {
        this.currentInstant = this.currentInstant.plusSeconds(seconds);
    }

    public synchronized void advanceMillis(long millis) {
        this.currentInstant = this.currentInstant.plusMillis(millis);
    }
}
