package coffee.model;

import java.util.List;

/**
 * Concrete subclass: Coffee
 * Utilizing: inheritance, method overriding (polymorphism), immutable list
 */
public class Coffee extends MenuItem {
    private final boolean hasMilk;
    private final int espressoShots;
    private final List<String> availableAddons;

    public Coffee(String id, String name, double basePrice, int stock,
                  boolean hasMilk, int espressoShots) {
        super(id, name, basePrice, "Coffee", stock);
        this.hasMilk        = hasMilk;
        this.espressoShots  = espressoShots;
        this.availableAddons = List.of("Extra Shot +$0.75", "Oat Milk +$0.50", "Vanilla Syrup +$0.25");
    }

    @Override
    public String prepareDescription() {
        return String.format("Brewing %d-shot %s%s... ",
                espressoShots, getName(), hasMilk ? " with milk" : "");
    }

    public boolean hasMilk()              { return hasMilk; }
    public int getEspressoShots()         { return espressoShots; }
    public List<String> getAvailableAddons() { return availableAddons; }
}
