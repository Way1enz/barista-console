package coffee.model;

import java.util.Set;

/**
 * Concrete subclass: Pastry
 * Utilizing: inheritance, Set usage, polymorphism via prepareDescription
 */
public class Pastry extends MenuItem {
    private final boolean isVegan;
    private final Set<String> allergens;   // HashSet internally — O(1) lookup

    public Pastry(String id, String name, double basePrice, int stock,
                  boolean isVegan, Set<String> allergens) {
        super(id, name, basePrice, "Pastry", stock);
        this.isVegan   = isVegan;
        this.allergens = Set.copyOf(allergens); // immutable defensive copy
    }

    @Override
    public String prepareDescription() {
        String tag = isVegan ? "[VEGAN] " : "";
        return String.format("Warming up your %s%s...", tag, getName());
    }

    public boolean isVegan()           { return isVegan; }
    public Set<String> getAllergens()  { return allergens; }
    public boolean containsAllergen(String allergen) {
        return allergens.contains(allergen.toLowerCase()); // O(1) HashSet lookup
    }
}
