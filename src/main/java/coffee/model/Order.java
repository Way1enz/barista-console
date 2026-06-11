package coffee.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Order {

    public enum Status { PENDING, PREPARING, READY, COMPLETED, CANCELLED }

    private static int counter = 1000;

    private final int orderId;
    private final String customerName;
    private final List<OrderItem> items;
    private final LocalDateTime createdAt;
    private Status status;
    private String paymentMethod = "unknown";
    private Coupon coupon = null;

    public Order(String customerName) {
        this.orderId     = ++counter;
        this.customerName = customerName;
        this.items       = new ArrayList<>();
        this.createdAt   = LocalDateTime.now();
        this.status      = Status.PENDING;
    }

    public void addItem(OrderItem oi)           { items.add(oi); }
    public void setStatus(Status s)             { this.status = s; }
    public void setPaymentMethod(String method) { this.paymentMethod = method; }
    public void setCoupon(Coupon c)             { this.coupon = c; }

    public double getSubtotal() {
        return items.stream().mapToDouble(OrderItem::subtotal).sum();
    }

    public double getTotal() {
        double sub = getSubtotal();
        return coupon != null ? coupon.apply(sub) : sub;
    }

    public int getTotalItems() {
        return items.stream().mapToInt(OrderItem::quantity).sum();
    }

    public Map<String, List<OrderItem>> itemsByCategory() {
        return items.stream().collect(Collectors.groupingBy(oi -> oi.item().getCategory()));
    }

    public Optional<OrderItem> getMostExpensive() {
        return items.stream().max(Comparator.comparingDouble(OrderItem::subtotal));
    }

    public Status getStatus()            { return status; }
    public int getOrderId()              { return orderId; }
    public String getCustomerName()      { return customerName; }
    public String getPaymentMethod()     { return paymentMethod; }
    public Optional<Coupon> getCoupon()  { return Optional.ofNullable(coupon); }
    public List<OrderItem> getItems()    { return Collections.unmodifiableList(items); }
    public boolean isEmpty()             { return items.isEmpty(); }

    public String getReceipt() {
        String sep = "-".repeat(46);
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(sep).append("\n");
        sb.append("  BREW & BEAN   order #").append(orderId).append("\n");
        sb.append("  ").append(customerName).append("\n");
        sb.append("  ").append(createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm"))).append("\n");
        sb.append("  via ").append(paymentMethod).append("\n");
        sb.append(sep).append("\n");
        for (OrderItem i : items) sb.append(i).append("\n");
        sb.append(sep).append("\n");
        double sub = getSubtotal();
        if (coupon != null) {
            sb.append(String.format("  subtotal                     $%.2f%n", sub));
            sb.append(String.format("  coupon %-8s (%d%% off)   -$%.2f%n",
                    coupon.getCode(), coupon.getPercentOff(), coupon.savings(sub)));
        }
        sb.append(String.format("  total  (%d items)             $%.2f%n", getTotalItems(), getTotal()));
        sb.append(sep).append("\n");
        return sb.toString();
    }
}
