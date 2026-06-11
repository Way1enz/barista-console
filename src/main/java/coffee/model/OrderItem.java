package coffee.model;

/**
 * Record: OrderItem
 * Utilizing: Records, compact constructors, value semantics
 * Records auto-generate: constructor, getters, equals, hashCode, toString
 */
public record OrderItem(MenuItem item, Size size, int quantity) {

    // Validation without repeating field assignment
    public OrderItem {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    }

    public double subtotal() {
        return (item.getPrice() + size.getPriceModifier()) * quantity;
    }

    @Override
    public String toString() {
        return String.format("  %-22s [%s] x%d  = $%.2f",
                item.getName(), size.getLabel(), quantity, subtotal());
    }
}
