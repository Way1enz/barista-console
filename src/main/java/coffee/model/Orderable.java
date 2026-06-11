package coffee.model;

/**
 * Interface: Orderable
 * Utilizing: interfaces, generics, default methods
 */
public interface Orderable {
    String getName();
    double getPrice();
    String getCategory();

    // Default method — interface with behavior
    default String getSummary() {
        return String.format("[%s] %s — $%.2f", getCategory(), getName(), getPrice());
    }

    // Static factory helper on interface
    static String formatPrice(double price) {
        return String.format("$%.2f", price);
    }
}
