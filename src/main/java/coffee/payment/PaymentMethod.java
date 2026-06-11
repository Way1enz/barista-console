package coffee.payment;

/**
 * PaymentMethod — sealed interface (Java 17+)
 * Demonstrates: sealed interfaces, records as subtypes, pattern matching on types
 */
public sealed interface PaymentMethod
        permits PaymentMethod.CreditCard, PaymentMethod.PayPal, PaymentMethod.Cash {

    String getDisplayName();
    boolean process(double amount);

    record CreditCard(String cardHolder, String maskedNumber, String expiry) implements PaymentMethod {
        public CreditCard {
            if (maskedNumber == null || maskedNumber.length() < 4)
                throw new IllegalArgumentException("Invalid card number.");
        }
        @Override public String getDisplayName() {
            return "card ending " + maskedNumber;
        }
        @Override public boolean process(double amount) {
            System.out.printf("  charging card ...%n");
            simulateDelay();
            boolean approved = amount <= 200.0;
            System.out.println(approved ? "  approved" : "  declined — limit exceeded");
            return approved;
        }
    }

    record PayPal(String email) implements PaymentMethod {
        @Override public String getDisplayName() { return "paypal " + email; }
        @Override public boolean process(double amount) {
            System.out.printf("  sending paypal request to %s ...%n", email);
            simulateDelay();
            System.out.println("  confirmed");
            return true;
        }
    }

    record Cash() implements PaymentMethod {
        @Override public String getDisplayName() { return "cash"; }
        @Override public boolean process(double amount) {
            System.out.printf("  cash $%.2f%n", amount);
            return true;
        }
    }

    private static void simulateDelay() {
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
    }
}
