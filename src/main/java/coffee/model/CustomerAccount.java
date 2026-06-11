package coffee.model;

import coffee.util.PasswordHasher;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CustomerAccount — registered customer with securely hashed password.
 *
 * Password storage:
 *   Stored as "saltB64:hashB64" (salted SHA-256 via PasswordHasher).
 *   The raw password is never retained after hashing.
 *
 * CSV format (6 fields):
 *   username, saltedHash, email, joinedOn, totalOrders
 *   The CSV file itself is AES-256-GCM encrypted by AccountService.
 */
public class CustomerAccount {

    private final String    username;
    private final String    saltedHash;   // "saltB64:hashB64" — never the plaintext
    private final String    email;
    private final LocalDate joinedOn;
    private int             totalOrders;

    /** Constructor for new registrations — hashes the password immediately. */
    public CustomerAccount(String username, String password, String email) {
        this.username    = username.trim().toLowerCase();
        this.saltedHash  = PasswordHasher.hash(password);
        this.email       = email.trim().toLowerCase();
        this.joinedOn    = LocalDate.now();
        this.totalOrders = 0;
    }

    /**
     * Factory method for password change — preserves existing joinedOn and totalOrders
     * but re-hashes with a fresh salt.
     * A static factory is used here because the load-from-disk constructor has the
     * same (String, String, String, LocalDate, int) signature; the two cannot coexist
     * as overloaded constructors.
     */
    public static CustomerAccount withNewPassword(String username, String newPassword,
                                                   String email, LocalDate joinedOn,
                                                   int totalOrders) {
        return new CustomerAccount(
                username.trim().toLowerCase(),
                PasswordHasher.hash(newPassword),
                email.trim().toLowerCase(),
                joinedOn,
                totalOrders);
    }

    /** Constructor for loading from disk — hash already computed. */
    public CustomerAccount(String username, String saltedHash, String email,
                           LocalDate joinedOn, int totalOrders) {
        this.username     = username;
        this.saltedHash   = saltedHash;
        this.email        = email;
        this.joinedOn     = joinedOn;
        this.totalOrders  = totalOrders;
    }

    /** Verify a plaintext password attempt against the stored hash. */
    public boolean checkPassword(String password) {
        return PasswordHasher.verify(password, saltedHash);
    }

    public void recordOrder() { totalOrders++; }

    public String    getUsername()    { return username; }
    public String    getEmail()       { return email; }
    public String    getSaltedHash()  { return saltedHash; }
    public LocalDate getJoinedOn()    { return joinedOn; }
    public int       getTotalOrders() { return totalOrders; }

    /**
     * CSV serialisation — goes into an AES-encrypted file, but we still
     * store the hash rather than plaintext as defence-in-depth.
     * Fields: username, saltedHash, email, joinedOn, totalOrders
     */
    public String toCsvLine() {
        // Use | as separator — saltedHash contains ":" internally (salt:hash)
        return String.join("|",
                username,
                saltedHash,
                email,
                joinedOn.toString(),
                String.valueOf(totalOrders));
    }

    public static CustomerAccount fromCsvLine(String line) {
        String[] p = line.split("\\|", 5);
        return new CustomerAccount(
                p[0],
                p[1],
                p[2],
                LocalDate.parse(p[3]),
                Integer.parseInt(p[4]));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomerAccount a)) return false;
        return Objects.equals(username, a.username);
    }

    @Override public int hashCode() { return Objects.hash(username); }

    @Override
    public String toString() {
        return String.format("  %-16s  %-30s  joined %-12s  %d orders",
                username, email, joinedOn, totalOrders);
    }
}
