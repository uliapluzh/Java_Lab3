package ru.spbstu.telematics.java;


public class Car implements Runnable{
    private final Direction from;
    private final Direction to;
    private final Crossroad crossroad;

    public Car(Direction from, Direction to, Crossroad crossroad) {
        this.from = from;
        this.to = to;
        this.crossroad = crossroad;
    }

    @Override
    public void run() {
        try {
            // въезжаем в перекресток
            crossroad.entrance(this);

            // время проезда
            Thread.sleep(500);

            // покидаем перекресток
            crossroad.leave(this);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Direction getFrom(){
        return this.from;
    }

    public Direction getTo(){
        return this.to;
    }

}
