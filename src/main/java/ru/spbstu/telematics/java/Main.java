package ru.spbstu.telematics.java;

public class Main {
    public static void main(String[] args) {
        Crossroad crossroad = new Crossroad();

        Car car1 = new Car(Direction.N, Direction.S, crossroad);
        Car car2 = new Car(Direction.S, Direction.N, crossroad);
        Car car3 = new Car(Direction.E, Direction.S, crossroad);
        Car car4 = new Car(Direction.W, Direction.E, crossroad);

        Thread thread1 = new Thread(car1);
        Thread thread2 = new Thread(car2);
        Thread thread3 = new Thread(car3);
        Thread thread4 = new Thread(car4);

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
            thread4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        crossroad.stopTrafficLights();
    }
}