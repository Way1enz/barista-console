package coffee.model;

import java.util.Objects;

public abstract class MenuItem implements Orderable, Comparable<MenuItem> {
    private final String id;
    private final String name;
    private final double basePrice;
    private final String category;
    private int stockCount;

    protected MenuItem(String id, String name, double basePrice, String category, int stockCount) {
        this.id         = id;
        this.name       = name;
        this.basePrice  = basePrice;
        this.category   = category;
        this.stockCount = stockCount;
    }

    public abstract String prepareDescription();

    @Override public String getName()     { return name; }
    @Override public double getPrice()    { return basePrice; }
    @Override public String getCategory() { return category; }

    public String getId()        { return id; }
    public int getStockCount()   { return stockCount; }
    public boolean isAvailable() { return stockCount > 0; }

    public void decrementStock(int qty) {
        if (stockCount < qty)
            throw new coffee.exception.OutOfStockException(name, stockCount, qty);
        stockCount -= qty;
        if (stockCount == 0) {
            System.out.println("  [out of stock] " + name);
        } else if (stockCount <= 3) {
            System.out.println("  [low stock] " + name + " — " + stockCount + " left");
        }
    }

    public void decrementStock() { decrementStock(1); }

    public void restock(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Restock quantity must be positive.");
        stockCount += qty;
    }

    @Override
    public int compareTo(MenuItem other) {
        return Double.compare(this.basePrice, other.basePrice);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuItem m)) return false;
        return Objects.equals(id, m.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        String stock = stockCount == 0  ? "  [sold out]"
                     : stockCount <= 3  ? "  [" + stockCount + " left]"
                     :                    "  " + stockCount;
        return String.format("%-4s  %-22s  %s%s", id, name, Orderable.formatPrice(basePrice), stock);
    }
}
