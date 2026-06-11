package coffee.service;

import coffee.model.*;
import coffee.repository.MenuRepository;

import java.util.Set;

/**
 * MenuDataLoader — seed the menu with initial items
 */
public class MenuDataLoader {

    public static void load(MenuRepository repo) {
        // Coffees
        repo.save(new Coffee("C01", "Espresso",       3.50, 20, false, 2));
        repo.save(new Coffee("C02", "Americano",      4.00, 15, false, 2));
        repo.save(new Coffee("C03", "Flat White",     4.50, 12, true,  2));
        repo.save(new Coffee("C04", "Cappuccino",     4.75, 10, true,  2));
        repo.save(new Coffee("C05", "Caramel Latte",  5.25, 8,  true,  3));
        repo.save(new Coffee("C06", "Cold Brew",      5.00, 6,  false, 4));
        repo.save(new Coffee("C07", "Matcha Latte",   5.50, 5,  true,  1));

        // Pastries
        repo.save(new Pastry("P01", "Butter Croissant", 3.75, 8,  false, Set.of("gluten", "dairy")));
        repo.save(new Pastry("P02", "Blueberry Muffin", 3.50, 6,  false, Set.of("gluten", "eggs")));
        repo.save(new Pastry("P03", "Banana Bread",     3.25, 4,  true,  Set.of("gluten")));
        repo.save(new Pastry("P04", "Almond Danish",    4.00, 5,  false, Set.of("gluten", "nuts", "dairy")));
        repo.save(new Pastry("P05", "Vegan Brownie",    4.25, 3,  true,  Set.of("gluten")));
    }
}
