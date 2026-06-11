package coffee.util;

import coffee.model.MenuItem;

import java.util.function.*;

/**
 * PricingEngine
 * Demonstrates: Functional interfaces (Function, Predicate, BiFunction, UnaryOperator),
 *               lambdas, method composition
 */
public class PricingEngine {

    // Predicate<T> — boolean test, composable with .and() / .or() / .negate()
    public static final Predicate<MenuItem> IS_AVAILABLE    = MenuItem::isAvailable;
    public static final Predicate<MenuItem> IS_AFFORDABLE   = item -> item.getPrice() < 5.00;
    public static final Predicate<MenuItem> IS_PREMIUM      = item -> item.getPrice() >= 5.00;

    // Composed predicate — available AND affordable
    public static final Predicate<MenuItem> AVAILABLE_AND_AFFORDABLE =
            IS_AVAILABLE.and(IS_AFFORDABLE);

    // Function<T, R> — transform a value
    public static final Function<Double, String> FORMAT_CURRENCY =
            price -> String.format("$%.2f", price);

    // UnaryOperator<T> — same type in and out; used for discount transforms
    public static UnaryOperator<Double> discount(double percent) {
        return price -> price * (1.0 - percent / 100.0);
    }

    // BiFunction<T, U, R> — combine two inputs
    public static final BiFunction<Double, Integer, Double> TOTAL_COST =
            (unitPrice, qty) -> unitPrice * qty;

    // Function composition: apply discount then format
    public static String applyAndFormat(double price, double discountPercent) {
        return discount(discountPercent)
                .andThen(FORMAT_CURRENCY)          // Function chaining with .andThen()
                .apply(price);
    }

    // Consumer<T> — performs side effects, returns void
    public static final java.util.function.Consumer<MenuItem> PRINT_ITEM =
            item -> System.out.println("  " + item.getSummary());
}
