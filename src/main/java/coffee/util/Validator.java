package coffee.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator — password and email validation rules.
 *
 * Password rules (all must pass):
 *   - At least 8 characters
 *   - At least one uppercase letter
 *   - At least one lowercase letter
 *   - At least one digit
 *   - At least one special character  (!@#$%^&*()-_=+[]{}|;:'",.<>?/`~)
 *   - No whitespace
 *
 * Email rules — follows a practical subset of RFC 5321:
 *   - local@domain.tld format
 *   - local part: letters, digits, dots, +, -, _  (no consecutive dots)
 *   - domain: letters, digits, hyphens
 *   - TLD: 2-8 letters
 *
 * Demonstrates: regex Pattern, List accumulation, record for validation results
 */
public final class Validator {

    private Validator() {}

    // Precompiled patterns — compiled once, reused (Pattern is thread-safe)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9][a-zA-Z0-9.+_-]*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,8}$"
    );
    private static final Pattern NO_CONSECUTIVE_DOTS = Pattern.compile("\\.\\.");
    private static final Pattern HAS_UPPER   = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER   = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT   = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()|\\-_=+\\[\\]{}|;:'\",.<>?/`~]");
    private static final Pattern HAS_SPACE   = Pattern.compile("\\s");

    /**
     * Result record — holds pass/fail and a list of specific failure messages.
     * Callers can print all failures at once rather than fix one and retry.
     */
    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }
        public static ValidationResult fail(List<String> errors) {
            return new ValidationResult(false, List.of());  // immutable copy
        }
        /** Convenience: build a fail result with collected error strings */
        public static ValidationResult of(List<String> errors) {
            return errors.isEmpty()
                    ? new ValidationResult(true, List.of())
                    : new ValidationResult(false, List.copyOf(errors));
        }
    }

    public static ValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < 8)
            errors.add("at least 8 characters");
        if (password != null && HAS_SPACE.matcher(password).find())
            errors.add("no spaces allowed");
        if (password != null && !HAS_UPPER.matcher(password).find())
            errors.add("at least one uppercase letter (A-Z)");
        if (password != null && !HAS_LOWER.matcher(password).find())
            errors.add("at least one lowercase letter (a-z)");
        if (password != null && !HAS_DIGIT.matcher(password).find())
            errors.add("at least one number (0-9)");
        if (password != null && !HAS_SPECIAL.matcher(password).find())
            errors.add("at least one special character (!@#$%^&* etc.)");
        return ValidationResult.of(errors);
    }

    public static ValidationResult validateEmail(String email) {
        List<String> errors = new ArrayList<>();
        if (email == null || email.isBlank()) {
            errors.add("email required");
        } else {
            String e = email.trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(e).matches())
                errors.add("invalid email format (expected user@domain.tld)");
            if (NO_CONSECUTIVE_DOTS.matcher(e).find())
                errors.add("email cannot contain consecutive dots");
            if (e.length() > 254)
                errors.add("email too long (max 254 characters)");
        }
        return ValidationResult.of(errors);
    }

    public static ValidationResult validateUsername(String username) {
        List<String> errors = new ArrayList<>();
        if (username == null || username.isBlank()) {
            errors.add("username required");
        } else {
            if (username.length() < 3)  errors.add("at least 3 characters");
            if (username.length() > 30) errors.add("max 30 characters");
            if (!username.matches("[a-zA-Z0-9_]+"))
                errors.add("only letters, numbers, and underscores");
        }
        return ValidationResult.of(errors);
    }

    /** Print all validation errors to stdout, indented. */
    public static void printErrors(ValidationResult result) {
        result.errors().forEach(e -> System.out.println("    - " + e));
    }
}
