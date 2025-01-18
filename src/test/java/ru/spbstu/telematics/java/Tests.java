package ru.spbstu.telematics.java;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Tests {
    private Crossroad crossroad;
    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUp() {
        crossroad = new Crossroad();
        // перенаправляем вывод в пустой поток, чтобы ничего не писалось в консоль во время тестов
        System.setOut(new PrintStream(outStream));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        crossroad.stopTrafficLights();
    }

    @Test
    public void testTrafficLightCycle() throws InterruptedException {
        TrafficLight trafficLight = new TrafficLight(3, 1); // 3 секунды зеленый,
                                                                                     // 1 секунда желтый
        trafficLight.start();
        assertEquals(TrafficLight.State.RED, trafficLight.getState());

        Thread.sleep(3100);
        assertEquals(TrafficLight.State.GREEN, trafficLight.getState());

        Thread.sleep(1100);
        assertEquals(TrafficLight.State.YELLOW, trafficLight.getState());

        Thread.sleep(3100);
        assertEquals(TrafficLight.State.RED, trafficLight.getState());

        trafficLight.stop();
    }

    @Test
    public void testCarQueueOrder() throws InterruptedException {
        Crossroad crossroad = new Crossroad();
        Car car1 = new Car(Direction.N, Direction.S, crossroad);
        Car car2 = new Car(Direction.E, Direction.W, crossroad);

        Thread thread1 = new Thread(car1);
        thread1.start();
        thread1.join();

        Thread thread2 = new Thread(car2);
        thread2.start();
        thread2.join();

        assertTrue(outStream.toString().contains("Car has left from N to S"));
        assertTrue(outStream.toString().contains("Car has left from E to W"));
    }

    @Test
    public void testMultipleCarsInQueue() throws InterruptedException {
        Crossroad crossroad = new Crossroad();
        Car car1 = new Car(Direction.N, Direction.S, crossroad);
        Car car2 = new Car(Direction.N, Direction.S, crossroad);
        Car car3 = new Car(Direction.E, Direction.W, crossroad);

        Thread thread1 = new Thread(car1);
        Thread thread2 = new Thread(car2);
        Thread thread3 = new Thread(car3);

        thread1.start();
        thread2.start();
        thread3.start();

        thread1.join();
        thread2.join();
        thread3.join();

        String output = outStream.toString();

        // проверяем, что машины в одном направлении (N -> S) покидают перекресток в правильном порядке
        int firstCarIndex = output.indexOf("Car has left from N to S");
        int secondCarIndex = output.indexOf("Car has left from N to S", firstCarIndex + 1);
        assertTrue(firstCarIndex < secondCarIndex,
                "Первый автомобиль должен уехать раньше второго.");

        // проверяем, что автомобиль в другом направлении (E -> W) уезжает после автомобилей N -> S
        assertTrue(firstCarIndex < output.indexOf("Car has left from E to W"),
                "Автомобиль E -> W должен уехать после автомобилей N -> S");
    }

    @Test
    public void testMultipleCarsInVariousDirections() throws InterruptedException {
        Crossroad crossroad = new Crossroad();

        Car[] cars = new Car[10];
        cars[0] = new Car(Direction.N, Direction.S, crossroad);
        cars[1] = new Car(Direction.N, Direction.E, crossroad);
        cars[2] = new Car(Direction.N, Direction.W, crossroad);
        cars[3] = new Car(Direction.E, Direction.N, crossroad);
        cars[4] = new Car(Direction.E, Direction.S, crossroad);
        cars[5] = new Car(Direction.E, Direction.W, crossroad);
        cars[6] = new Car(Direction.S, Direction.N, crossroad);
        cars[7] = new Car(Direction.S, Direction.E, crossroad);
        cars[8] = new Car(Direction.S, Direction.W, crossroad);
        cars[9] = new Car(Direction.W, Direction.N, crossroad);

        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(cars[i]);
            threads[i].start();
        }

        for (int i = 0; i < 10; i++) {
            threads[i].join();
        }

        for (int i = 0; i < 10; i++) {
            assertTrue(outStream.toString().contains("Car has left from " + cars[i].getFrom() + " to " + cars[i].getTo()));
        }
    }

    @Test
    public void testCarWaitingInQueue() throws InterruptedException {
        Crossroad crossroad = new Crossroad();

        Car car1 = new Car(Direction.N, Direction.S, crossroad);
        Car car2 = new Car(Direction.N, Direction.S, crossroad);
        Car car3 = new Car(Direction.N, Direction.S, crossroad);
        Car car4 = new Car(Direction.N, Direction.S, crossroad);
        Car car5 = new Car(Direction.N, Direction.S, crossroad);

        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(i == 0 ? car1 : i == 1 ? car2 : i == 2 ? car3 : i == 3 ? car4 : car5);
            threads[i].start();
        }

        Thread.sleep(1000);
        String output = outStream.toString();

        assertTrue(output.contains("Car from N to S is waiting"));

        for (int i = 0; i < 5; i++) {
            threads[i].join();
        }

        for (int i = 0; i < 5; i++) {
            assertTrue(output.contains("Car has left from N to S"));
        }
    }

}
