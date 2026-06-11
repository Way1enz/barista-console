package coffee.service;

import coffee.exception.*;
import coffee.model.*;
import coffee.repository.*;
import coffee.util.*;

import java.util.*;
import java.util.stream.*;

/**
 * OrderService — core business logic.
 * Demonstrates: Pattern matching, Optional chaining, service layer design, exception handling
 */
public class OrderService {

    private final MenuRepository menuRepo;
    private final OrderQueue orderQueue;

    public OrderService(MenuRepository menuRepo, OrderQueue orderQueue) {
        this.menuRepo   = menuRepo;
        this.orderQueue = orderQueue;
    }

    /**
     * Add item to cart.
     * Demonstrates: Pattern matching instanceof (Java 16+)
     * Stock is NOT deducted here — only validated. Deduction happens at Cart.checkout().
     */
    public void addToCart(Cart cart, String itemId, Size size, int qty) {
        MenuItem item = menuRepo.findById(itemId)
                .orElseThrow(() -> new CoffeeShopException("Item not found: \"" + itemId + "\". Check the ID."));

        if (!item.isAvailable())
            throw new OutOfStockException(item.getName());

        // Pattern matching instanceof
        if (item instanceof Coffee coffee) {
            System.out.println(coffee.prepareDescription());
            if (coffee.hasMilk() && size == Size.LARGE)
                System.out.println("       Extra milk added for large.");
        } else if (item instanceof Pastry pastry) {
            System.out.println(pastry.prepareDescription());
            if (!pastry.getAllergens().isEmpty())
                System.out.println("       Allergens: " + String.join(", ", pastry.getAllergens()));
        }

        cart.add(item, size, qty); // validates qty vs stock inside Cart
    }

    /**
     * Checkout: convert cart → order, then enqueue.
     * Demonstrates: switch expression
     */
    public Order checkout(Cart cart, FileLogger logger) {
        if (cart.isEmpty()) throw new CoffeeShopException("Cart is empty.");
        Order order = cart.checkout(); // stock deducted inside here

        String message = switch (order.getStatus()) {
            case PENDING   -> "Order #" + order.getOrderId() + " is queued for preparation!";
            case CANCELLED -> throw new CoffeeShopException("Cannot enqueue a cancelled order.");
            default        -> "Order already in progress.";
        };

        orderQueue.enqueue(order);
        logger.logOrder(order);
        System.out.println(message);
        System.out.printf("     Queue position: %d%n", orderQueue.pendingCount());
        return order;
    }

    public Optional<Order> processNext() {
        return orderQueue.dequeue().map(order -> {
            order.setStatus(Order.Status.PREPARING);
            return order;
        });
    }

    public void markReady(Order order) {
        order.setStatus(Order.Status.READY);
    }

    public void completeOrder(Order order, FileLogger logger) {
        orderQueue.complete(order);
        logger.logOrder(order);
    }

    public void cancelOrder(Order order, FileLogger logger) {
        order.setStatus(Order.Status.CANCELLED);
        logger.logOrder(order);
        // Restock items
        for (OrderItem oi : order.getItems()) {
            oi.item().restock(oi.quantity());
        }
        System.out.println("    Stock restored for cancelled order #" + order.getOrderId());
    }

    public List<MenuItem> getAffordableMenu() {
        return menuRepo.findAvailable().stream()
                .filter(PricingEngine.AVAILABLE_AND_AFFORDABLE)
                .collect(Collectors.toList());
    }

    public MenuRepository getMenuRepo()  { return menuRepo; }
    public OrderQueue getOrderQueue()    { return orderQueue; }
}
