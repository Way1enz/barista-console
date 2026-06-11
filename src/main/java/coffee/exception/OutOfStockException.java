package coffee.exception;

public class OutOfStockException extends CoffeeShopException {
    private final String itemName;
    private final int available;
    private final int requested;

    // Full constructor
    public OutOfStockException(String itemName, int available, int requested) {
        super(String.format(
            "\"%s\" — only %d in stock, you requested %d.",
            itemName, available, requested));
        this.itemName  = itemName;
        this.available = available;
        this.requested = requested;
    }

    // Zero-stock shorthand
    public OutOfStockException(String itemName) {
        this(itemName, 0, 1);
    }

    public String getItemName() { return itemName; }
    public int getAvailable()   { return available; }
    public int getRequested()   { return requested; }
}
