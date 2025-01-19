package ru.spbstu.telematics.java;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;


public class Crossroad {
    private final Lock lock = new ReentrantLock();
    private final Map<Direction, Condition> conditions;
    private final Map<Direction, Queue<Car>> queues;
    private final Map<Direction, Boolean> directionStates;
    private final TrafficLight trafficLight;

    // Матрица пересечений траекторий
    public final boolean[][] trConflicts = {
            //       N->S    N->E   N->W   E->W   E->N   E->S   S->N   S->E   S->W   W->E   W->N   W->S
            /*N->S*/ {false, false, false, true,  false, false, false, false, true,  true,  true,  false},
            /*N->E*/ {false, false, false, true,  false, true,  true,  false, true,  false, false, false},
            /*N->W*/ {false, false, false, false, false, false, false, false, false, false, false, false},
            /*E->W*/ {true,  true,  false, false, false, false, true,  false, false, false, false, false},
            /*E->N*/ {false, false, false, false, false, false, false, false, false, false, false, false},
            /*E->S*/ {false, true,  false, false, false, false, true, false, true,  true,  false, false},
            /*S->N*/ {false, true,  false, true,  false, true,  false, false, false, true,  false, false},
            /*S->E*/ {false, false, false, false, false, false, false, false, false, false, false, false},
            /*S->W*/ {true,  true,  false, false, false, true,  false, false, false, true,  true,  false},
            /*W->E*/ {true,  false, false, false, false, true,  true,  false, true,  false, false, false},
            /*W->N*/ {true,  true,  false, true,  false, true,  false, false, true,  false, false, false},
            /*W->S*/ {false, false, false, false, false, false, false, false, false, false, false,  false}
    };

    public Crossroad() {
        conditions = new HashMap<>();
        queues = new HashMap<>();
        directionStates = new HashMap<>();
        trafficLight = new TrafficLight(10, 2);

        for (Direction direction : Direction.values()) {
            queues.put(direction, new LinkedList<>());
            conditions.put(direction, lock.newCondition());
            directionStates.put(direction, false);
        }

        trafficLight.start();
    }

    // проверка, конфликтуют ли два направления
    private int getTrIndex(Direction from, Direction to) {
        if (from == to) {
            return -1;
        }

        if (from == Direction.N) {
            if (to == Direction.S) return 0;
            if (to == Direction.E) return 1;
            if (to == Direction.W) return 2;
        } else if (from == Direction.E) {
            if (to == Direction.W) return 3;
            if (to == Direction.N) return 4;
            if (to == Direction.S) return 5;
        } else if (from == Direction.S) {
            if (to == Direction.N) return 6;
            if (to == Direction.E) return 7;
            if (to == Direction.W) return 8;
        } else if (from == Direction.W) {
            if (to == Direction.E) return 9;
            if (to == Direction.N) return 10;
            if (to == Direction.S) return 11;
        }

        throw new IllegalArgumentException("Invalid trajectory from " + from + " to " + to);
    }

    public boolean areTrConflicting(Direction from1, Direction to1, Direction from2, Direction to2) {
        int index1 = getTrIndex(from1, to1);
        int index2 = getTrIndex(from2, to2);

        if (index1 == -1 || index2 == -1) {
            return false;
        }

        return trConflicts[index1][index2];
    }

    // проверка, могут ли два автомобиля двигаться одновременно
    private boolean canMove(Direction from, Direction to) {
        for (Direction dir : Direction.values()) {
            if (directionStates.get(dir)) {
                for (Direction dest : Direction.values()) {
                    if (directionStates.get(dest) && areTrConflicting(from, to, dir, dest)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void entrance(Car car) throws InterruptedException {
        Direction from = car.getFrom();
        Direction to = car.getTo();

        lock.lock();
        try {
            queues.get(from).add(car);
            // ждем, пока автомобиль станет первым в очереди, путь будет свободен и светофор зеленый
            while (queues.get(from).peek() != car || !canMove(from, to) ||
                    trafficLight.getState() != TrafficLight.State.GREEN) {

                if (queues.get(from).peek() != car) {
                    System.out.println("Car from " + from + " to " + to + " " +
                            "is waiting because it's not first in the queue");
                } else if (!canMove(from, to)) {
                    System.out.println("Car from " + from + " to " + to + " " +
                            "is waiting due to conflicting directions");
                } else if (trafficLight.getState() != TrafficLight.State.GREEN) {
                    System.out.println("Car from " + from + " to " + to + " " +
                            "is waiting because the traffic light is not green");
                }

                conditions.get(from).await();
            }

            System.out.println("Car moving from " + from + " to " + to);

            // блокируем направления
            directionStates.put(from, true);
            directionStates.put(to, true);
        } finally {
            lock.unlock();
        }
    }


    public void leave(Car car) {
        Direction from = car.getFrom();
        Direction to = car.getTo();

        System.out.println("Car has left from " + from + " to " + to);
        queues.get(from).poll();

        lock.lock();
        try {
            directionStates.put(from, false);
            directionStates.put(to, false);

            for (Direction direction : Direction.values()) {
                if (!queues.get(direction).isEmpty()) {
                    conditions.get(direction).signalAll();
                }
            }
        } finally {
            lock.unlock();
        }

        //queues.get(from).poll();

    }


    public void stopTrafficLights() {
        trafficLight.stop();
    }
}
