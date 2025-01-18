package ru.spbstu.telematics.java;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TrafficLight {
    public enum State {
        GREEN, YELLOW, RED
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Lock lock = new ReentrantLock();
    private final int greenDuration;
    private final int yellowDuration;
    private State state;

    public TrafficLight(int greenDuration, int yellowDuration) {
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.state = State.RED;
    }

    public State getState() {
        lock.lock();
        try {
            return state;
        } finally {
            lock.unlock();
        }
    }

    public void switchState(State newState) {
        lock.lock();
        try {
            this.state = newState;
            System.out.println("Traffic light switched to " + newState);
        } finally {
            lock.unlock();
        }
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                if (state == State.GREEN) {
                    switchState(State.YELLOW);
                    scheduler.schedule(() -> {
                        lock.lock();
                        try {
                            switchState(State.RED);
                        } finally {
                            lock.unlock();
                        }
                    }, yellowDuration, TimeUnit.SECONDS);
                } else if (state == State.RED) {
                    switchState(State.GREEN);
                }
            } finally {
                lock.unlock();
            }
        }, 0, greenDuration + yellowDuration, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
