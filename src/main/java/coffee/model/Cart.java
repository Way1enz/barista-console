package coffee.model;

import java.util.*;

/**
 * Cart — customer's in-progress selection before checkout.
 * Utilizing: LinkedHashMap (insertion-ordered), record inner type, Optional
 */
public class Cart {

    public record CartEntry(MenuItem item, Size size, int qty) {
        public double subtotal() {
            return (item.getPrice() + size.getPriceModifier()) * qty;
        }
        @Override public String toString() {
            return String.format("  %-4s  %-22s  [%s] x%d   $%.2f",
                    item.getId(), item.getName(), size.getLabel(), qty, subtotal());
        }
    }

    private final Map<String, CartEntry> entries = new LinkedHashMap<>();
    private final String customerName;
    private Coupon appliedCoupon = null;

    public Cart(String customerName) { this.customerName = customerName; }

    public void add(MenuItem item, Size size, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive.");
        String key = item.getId() + "-" + size.getLabel();

        int already     = entries.containsKey(key) ? entries.get(key).qty() : 0;
        int totalWanted = already + qty;

        if (totalWanted > item.getStockCount())
            throw new coffee.exception.OutOfStockException(item.getName(), item.getStockCount(), totalWanted);

        if (entries.containsKey(key)) {
            CartEntry e = entries.get(key);
            entries.put(key, new CartEntry(item, size, e.qty() + qty));
        } else {
            entries.put(key, new CartEntry(item, size, qty));
        }
    }

    public boolean removeByIndex(int index) {
        List<String> keys = new ArrayList<>(entries.keySet());
        if (index < 1 || index > keys.size()) return false;
        entries.remove(keys.get(index - 1));
        return true;
    }

    public boolean updateQty(int index, int newQty, MenuItem item) {
        List<String> keys = new ArrayList<>(entries.keySet());
        if (index < 1 || index > keys.size()) return false;
        String key = keys.get(index - 1);
        if (newQty <= 0) {
            entries.remove(key);
        } else {
            if (newQty > item.getStockCount())
                throw new coffee.exception.OutOfStockException(item.getName(), item.getStockCount(), newQty);
            CartEntry e = entries.get(key);
            entries.put(key, new CartEntry(e.item(), e.size(), newQty));
        }
        return true;
    }

    public void applyCoupon(Coupon c) { this.appliedCoupon = c; }
    public void removeCoupon()        { this.appliedCoupon = null; }
    public Optional<Coupon> getCoupon() { return Optional.ofNullable(appliedCoupon); }

    public void clear()             { entries.clear(); }
    public boolean isEmpty()        { return entries.isEmpty(); }
    public int lineCount()          { return entries.size(); }
    public String getCustomerName() { return customerName; }
    public List<CartEntry> getEntries() { return List.copyOf(entries.values()); }

    public double getSubtotal() {
        return entries.values().stream().mapToDouble(CartEntry::subtotal).sum();
    }

    public double getTotal() {
        double sub = getSubtotal();
        return appliedCoupon != null ? appliedCoupon.apply(sub) : sub;
    }

    public int getTotalItems() {
        return entries.values().stream().mapToInt(CartEntry::qty).sum();
    }

    public Order checkout() {
        if (isEmpty()) throw new coffee.exception.CoffeeShopException("Cart is empty.");
        Order order = new Order(customerName);
        for (CartEntry e : entries.values()) {
            e.item().decrementStock(e.qty());
            order.addItem(new OrderItem(e.item(), e.size(), e.qty()));
        }
        if (appliedCoupon != null) order.setCoupon(appliedCoupon);
        return order;
    }

    public void print() {
        System.out.println();
        if (isEmpty()) { System.out.println("  cart is empty"); return; }
        System.out.println("  --- cart ---");
        List<CartEntry> list = getEntries();
        for (int i = 0; i < list.size(); i++)
            System.out.printf("  [%d]%s%n", i + 1, list.get(i));
        System.out.println("  -----------");
        double sub = getSubtotal();
        if (appliedCoupon != null) {
            System.out.printf("  subtotal   $%.2f%n", sub);
            System.out.printf("  coupon %-10s (%d%% off)   -$%.2f%n",
                    appliedCoupon.getCode(), appliedCoupon.getPercentOff(),
                    appliedCoupon.savings(sub));
        }
        System.out.printf("  total  (%d items)   $%.2f%n", getTotalItems(), getTotal());
    }
}
