package coffee.service;

import coffee.exception.CoffeeShopException;
import coffee.model.Coupon;

import java.time.LocalDate;
import java.util.*;

/**
 * CouponService — create, redeem, and manage coupons.
 * Demonstrates: HashMap, Optional, stream filter, guard clauses
 */
public class CouponService {

    private final Map<String, Coupon> coupons = new HashMap<>();

    /**
     * Create a new coupon.
     * @param maxUses  pass Coupon.UNLIMITED (-1) for unlimited uses
     */
    public Coupon create(String code, int percentOff, LocalDate expiresOn, int maxUses) {
        String key = code.trim().toUpperCase();
        if (coupons.containsKey(key))
            throw new CoffeeShopException("Code \"" + key + "\" already exists.");
        Coupon c = new Coupon(key, percentOff, expiresOn, maxUses);
        coupons.put(key, c);
        return c;
    }

    /**
     * Validate and return a coupon ready to apply.
     * Does NOT record use — call recordUse() after the order is confirmed.
     */
    public Optional<Coupon> validate(String code) {
        return Optional.ofNullable(coupons.get(code.trim().toUpperCase()))
                .filter(Coupon::isValid);
    }

    /** Record one use on a coupon. Call once payment succeeds. */
    public void recordUse(String code) {
        Coupon c = coupons.get(code.trim().toUpperCase());
        if (c != null) c.recordUse();
    }

    /**
     * Barista: deactivate a coupon early.
     * @return false if not found
     */
    public boolean deactivate(String code) {
        Coupon c = coupons.get(code.trim().toUpperCase());
        if (c == null) return false;
        c.deactivate();
        return true;
    }

    public boolean exists(String code) {
        return coupons.containsKey(code.trim().toUpperCase());
    }

    public List<Coupon> listAll() {
        return coupons.values().stream()
                .sorted(Comparator.comparing(Coupon::getCode))
                .toList();
    }

    public List<Coupon> listActive() {
        return coupons.values().stream()
                .filter(Coupon::isValid)
                .sorted(Comparator.comparing(Coupon::getCode))
                .toList();
    }
}
