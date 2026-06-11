package coffee.service;

import coffee.model.Order;

import java.util.List;
import java.util.concurrent.*;

/**
 * VirtualThreadBarista
 * Demonstrates: Virtual Threads (Project Loom, Java 21), ExecutorService,
 *               Structured concurrency concepts, CompletableFuture
 */
public class VirtualThreadBarista {

    /**
     * Process multiple orders concurrently using Virtual Threads.
     *
     * Virtual Threads vs Platform Threads:
     * - Platform threads: ~1MB stack, OS-managed, expensive context switch
     * - Virtual threads: ~few KB, JVM-managed, millions possible, ideal for I/O-bound work
     *
     * newVirtualThreadPerTaskExecutor() — creates a new VT per submitted task
     */
    public static List<String> processOrdersConcurrently(List<Order> orders) throws InterruptedException {
        List<String> results = new CopyOnWriteArrayList<>(); // thread-safe list

        // try-with-resources on ExecutorService — auto-shuts down (Java 19+)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<String>> futures = orders.stream()
                    .map(order -> executor.submit(() -> simulatePrepare(order)))
                    .toList();  // immutable list — Java 16+

            for (Future<String> future : futures) {
                try {
                    results.add(future.get(5, TimeUnit.SECONDS));
                } catch (ExecutionException | TimeoutException e) {
                    results.add(" Order prep failed: " + e.getMessage());
                }
            }
        }

        return results;
    }

    /**
     * Simulates blocking I/O work (e.g., sending to kitchen display, payment processing).
     * Virtual threads shine here — they unmount from carrier thread during sleep/IO,
     * freeing the OS thread to do other work.
     */
    private static String simulatePrepare(Order order) throws InterruptedException {
        Thread.sleep(100); // simulates I/O wait — VT unmounts here, not blocking OS thread
        order.setStatus(Order.Status.PREPARING);
        return String.format("Order #%d for %s is being prepared on: %s",
                order.getOrderId(),
                order.getCustomerName(),
                Thread.currentThread()); // prints VirtualThread[#N]/runnable@...
    }

    /**
     * CompletableFuture example — async payment verification
     * Demonstrates: async pipelines, thenApply, exceptionally
     */
    public static CompletableFuture<String> verifyPaymentAsync(double amount) {
        return CompletableFuture
                .supplyAsync(() -> {
                    // Simulate payment gateway call
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    if (amount > 0) return "APPROVED";
                    throw new RuntimeException("Invalid amount");
                }, Executors.newVirtualThreadPerTaskExecutor())
                .thenApply(status -> "Payment " + status + " for $" + String.format("%.2f", amount))
                .exceptionally(ex -> "Payment FAILED: " + ex.getMessage());
    }
}
