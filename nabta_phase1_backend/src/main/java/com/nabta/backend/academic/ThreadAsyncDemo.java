package com.nabta.backend.academic;

public class ThreadAsyncDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Nabta Thread Demonstration");
        System.out.println("Main thread: " + Thread.currentThread().getName());

        Runnable notificationTask = () -> {
            System.out.println("Notification task started.");
            System.out.println("Worker thread: " + Thread.currentThread().getName());
            System.out.println("Preparing notification for Nabta supervisors...");
            System.out.println("Notification task completed successfully.");
        };

        Thread worker = new Thread(notificationTask, "nabta-notification-worker");

        System.out.println("Starting background task...");
        worker.start();

        worker.join();

        System.out.println("Main thread: background task finished.");
        System.out.println("DEMONSTRATION SUCCESSFUL");
    }
}