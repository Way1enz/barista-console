package coffee.model;

import java.time.LocalDate;

/**
 * Coupon — mutable (not a record) because usesRemaining and active flag change over time.
 * Demonstrates: class vs record tradeoff, encapsulation, Optional
 */
public class Coupon {

    public static final int UNLIMITED = -1;

    private final String    code;
    private final int       percentOff;
    private final LocalDate expiresOn;      // null = no expiry
    private int             usesRemaining;  // -1 = unlimited
    private boolean         active;

    public Coupon(String code, int percentOff, LocalDate expiresOn, int maxUses) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Code cannot be blank.");
        if (percentOff < 1 || percentOff > 100)
            throw new IllegalArgumentException("Percent must be 1-100.");
        if (maxUses != UNLIMITED && maxUses < 1)
            throw new IllegalArgumentException("Max uses must be at least 1 or UNLIMITED (-1).");
        this.code          = code.trim().toUpperCase();
        this.percentOff    = percentOff;
        this.expiresOn     = expiresOn;
        this.usesRemaining = maxUses;
        this.active        = true;
    }

    /** Returns true if this coupon can still be redeemed. */
    public boolean isValid() {
        if (!active) return false;
        if (expiresOn != null && LocalDate.now().isAfter(expiresOn)) return false;
        if (usesRemaining == 0) return false;
        return true;
    }

    /** Deducts one use. Call only after confirming isValid(). */
    public void recordUse() {
        if (usesRemaining > 0) usesRemaining--;
        if (usesRemaining == 0) active = false;
    }

    /** Barista can deactivate a coupon early at any time. */
    public void deactivate() { this.active = false; }

    public double apply(double amount)   { return amount * (1.0 - percentOff / 100.0); }
    public double savings(double amount) { return amount - apply(amount); }

    public String getCode()       { return code; }
    public int getPercentOff()    { return percentOff; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public int getUsesRemaining() { return usesRemaining; }
    public boolean isActive()     { return active; }

    public String statusLabel() {
        if (!active && usesRemaining == 0) return "[used up]";
        if (!active) return "[disabled]";
        if (expiresOn != null && LocalDate.now().isAfter(expiresOn)) return "[expired]";
        return "[active]";
    }

    @Override
    public String toString() {
        String uses   = usesRemaining == UNLIMITED ? "unlimited" : usesRemaining + " uses left";
        String expiry = expiresOn != null ? "  exp " + expiresOn : "";
        return String.format("%-12s  %d%% off  %-14s%s  %s",
                code, percentOff, uses, expiry, statusLabel());
    }
}
