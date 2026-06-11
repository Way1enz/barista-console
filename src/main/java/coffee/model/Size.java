package coffee.model;

/**
 * Enum with fields and methods — encapsulated constants with behavior
 */
public enum Size {
    SMALL("S", 0.00),
    MEDIUM("M", 0.50),
    LARGE("L", 1.00);

    private final String label;
    private final double priceModifier;

    Size(String label, double priceModifier) {
        this.label = label;
        this.priceModifier = priceModifier;
    }

    public String getLabel()         { return label; }
    public double getPriceModifier() { return priceModifier; }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase() + " (+" + Orderable.formatPrice(priceModifier) + ")";
    }
}
