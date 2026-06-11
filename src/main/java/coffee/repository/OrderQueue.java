package coffee.repository;

import coffee.model.Order;

import java.util.*;

/**
 * OrderQueue
 * Demonstrates: Queue (FIFO for orders), Deque, Stack (for order history),
 *               LinkedList internals (nodes, O(1) head/tail ops)
 */
public class OrderQueue {

    // Queue — FIFO: new orders added to tail, barista processes from head
    // LinkedList: each node holds data + pointer to next/prev — O(1) add/remove at ends
    private final Queue<Order> pendingOrders = new LinkedList<>();

    // ArrayDeque as Stack for completed order history — O(1) push/pop
    private final Deque<Order> completedOrders = new ArrayDeque<>();

    // HashMap for O(1) lookup by order ID across all states
    private final Map<Integer, Order> allOrders = new HashMap<>();

    public void enqueue(Order order) {
        pendingOrders.offer(order);   // offer = add to tail (non-throwing)
        allOrders.put(order.getOrderId(), order);
    }

    /** Barista takes the next order — O(1) remove from head */
    public Optional<Order> dequeue() {
        return Optional.ofNullable(pendingOrders.poll());
    }

    /** Peek without removing — O(1) */
    public Optional<Order> peek() {
        return Optional.ofNullable(pendingOrders.peek());
    }

    public void complete(Order order) {
        completedOrders.push(order);   // push to top of stack
        order.setStatus(Order.Status.COMPLETED);
    }

    /** Most recently completed order — O(1) */
    public Optional<Order> lastCompleted() {
        return completedOrders.isEmpty()
                ? Optional.empty()
                : Optional.of(completedOrders.peek());
    }

    public Optional<Order> findById(int id) {
        return Optional.ofNullable(allOrders.get(id));
    }

    public int pendingCount()   { return pendingOrders.size(); }
    public int completedCount() { return completedOrders.size(); }
    public int totalCount()     { return allOrders.size(); }

    public List<Order> getCompleted() {
        return new ArrayList<>(completedOrders);
    }
}
