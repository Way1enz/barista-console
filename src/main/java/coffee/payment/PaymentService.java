package coffee.payment;

import coffee.exception.CoffeeShopException;
import coffee.model.Order;

import java.util.Scanner;

/**
 * PaymentService — collects and processes payment.
 * Demonstrates: sealed type pattern matching switch (Java 21)
 */
public class PaymentService {

    private final Scanner scanner;

    public PaymentService(Scanner scanner) { this.scanner = scanner; }

    public boolean collectAndProcess(Order order, double amount) {
        System.out.println();
        System.out.printf("  amount due: $%.2f%n", amount);
        System.out.println();
        System.out.println("  payment method:");
        System.out.println("  1  credit card");
        System.out.println("  2  paypal");
        System.out.println("  3  cash");

        String choice = prompt("").trim();
        PaymentMethod method;
        try {
            method = switch (choice) {
                case "1" -> collectCreditCard();
                case "2" -> collectPayPal();
                case "3" -> new PaymentMethod.Cash();
                default  -> throw new CoffeeShopException("Select 1, 2, or 3.");
            };
        } catch (CoffeeShopException e) {
            System.out.println("  " + e.getMessage());
            return false;
        }

        // Exhaustive switch — sealed type, no default needed
        String label = switch (method) {
            case PaymentMethod.CreditCard cc -> cc.getDisplayName() + "  " + cc.cardHolder() + "  exp " + cc.expiry();
            case PaymentMethod.PayPal pp     -> pp.getDisplayName();
            case PaymentMethod.Cash c        -> c.getDisplayName();
        };
        System.out.println("  " + label);
        System.out.println();

        boolean success = method.process(amount);
        if (success && order != null) {
            order.setPaymentMethod(label);
        }
        return success;
    }

    private PaymentMethod.CreditCard collectCreditCard() {
        String name = prompt("name on card").trim();
        if (name.isBlank()) throw new CoffeeShopException("Name required.");

        String raw = prompt("card number").replaceAll("\\s+", "");
        if (!raw.matches("\\d+"))          throw new CoffeeShopException("Card number must contain digits only.");
        if (raw.length() < 13 || raw.length() > 19)
                                             throw new CoffeeShopException("Card number must be 13-19 digits.");
        if (!luhn(raw))                      throw new CoffeeShopException("Invalid card number.");
        String last4 = raw.substring(raw.length() - 4);

        String expiry = prompt("expiry MM/YY").trim();
        if (!expiry.matches("\\d{2}/\\d{2}")) throw new CoffeeShopException("Format must be MM/YY.");
        int mm = Integer.parseInt(expiry.substring(0, 2));
        int yy = Integer.parseInt(expiry.substring(3));
        if (mm < 1 || mm > 12) throw new CoffeeShopException("Month must be 01-12.");
        java.time.YearMonth now = java.time.YearMonth.now();
        java.time.YearMonth cardExp = java.time.YearMonth.of(2000 + yy, mm);
        if (cardExp.isBefore(now)) throw new CoffeeShopException("Card has expired.");

        return new PaymentMethod.CreditCard(name, last4, expiry);
    }

    /** Luhn algorithm — industry-standard check digit validation for card numbers. */
    private static boolean luhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    private PaymentMethod.PayPal collectPayPal() {
        String email = prompt("paypal email").trim();
        if (!email.contains("@")) throw new CoffeeShopException("Invalid email.");
        return new PaymentMethod.PayPal(email);
    }

    private String prompt(String label) {
        if (!label.isBlank()) System.out.print("  " + label + ": ");
        else System.out.print("  > ");
        return scanner.nextLine();
    }
}
