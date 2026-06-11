package coffee.service;

import coffee.exception.CoffeeShopException;
import coffee.model.CustomerAccount;
import coffee.util.FileEncryptor;
import coffee.util.Validator;
import coffee.util.Validator.ValidationResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AccountService — register, authenticate, and persist customer accounts.
 *
 * Security layers:
 *   1. Validator enforces strong password + email rules before accepting input
 *   2. PasswordHasher stores salted SHA-256 hashes — never plaintext
 *   3. FileEncryptor writes the CSV as AES-256-GCM ciphertext on disk
 *
 * Demonstrates: NIO, HashMap O(1) lookup, Optional, try-with-resources,
 *               layered security (validate → hash → encrypt)
 */
public class AccountService {

    private final Path dataFile;
    private final Map<String, CustomerAccount> accounts = new HashMap<>();

    public AccountService(String dataDirectory) throws IOException {
        Path dir = Path.of(dataDirectory);
        Files.createDirectories(dir);
        this.dataFile = dir.resolve("accounts.dat");   // .dat — opaque encrypted blob
        loadFromDisk();
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Register a new account.
     * Validates username, password, and email before creating the account.
     * Throws CoffeeShopException with the first category of failures found.
     */
    public CustomerAccount register(String username, String password, String email) {
        // Validate each field — collect ALL errors per field before failing
        ValidationResult uResult = Validator.validateUsername(username);
        if (!uResult.valid()) {
            throw new CoffeeShopException("username: " + String.join(", ", uResult.errors()));
        }

        if (usernameExists(username)) {
            throw new CoffeeShopException("username \"" + username.trim().toLowerCase() + "\" is already taken");
        }

        ValidationResult pResult = Validator.validatePassword(password);
        if (!pResult.valid()) {
            throw new CoffeeShopException("password does not meet requirements");
            // caller should use Validator.printErrors() to show the list
        }

        ValidationResult eResult = Validator.validateEmail(email);
        if (!eResult.valid()) {
            throw new CoffeeShopException("email: " + String.join(", ", eResult.errors()));
        }

        CustomerAccount account = new CustomerAccount(username, password, email);
        accounts.put(account.getUsername(), account);
        saveToDisk();
        return account;
    }

    /**
     * Authenticate. Returns empty Optional on wrong credentials (no detail —
     * don't tell attackers whether the username or password was wrong).
     */
    public Optional<CustomerAccount> login(String username, String password) {
        return Optional.ofNullable(accounts.get(username.trim().toLowerCase()))
                .filter(a -> a.checkPassword(password));
    }

    public boolean usernameExists(String username) {
        return accounts.containsKey(username.trim().toLowerCase());
    }

    public void recordOrder(CustomerAccount account) {
        account.recordOrder();
        saveToDisk();
    }

    /**
     * Change password — verifies old password first, validates the new one,
     * then replaces the stored account with a fresh hash+salt and persists.
     * Returns the updated account on success, empty if old password was wrong.
     */
    public Optional<CustomerAccount> changePassword(CustomerAccount account,
                                                     String oldPassword,
                                                     String newPassword) {
        if (!account.checkPassword(oldPassword)) return Optional.empty();

        CustomerAccount updated = CustomerAccount.withNewPassword(
                account.getUsername(),
                newPassword,
                account.getEmail(),
                account.getJoinedOn(),
                account.getTotalOrders());
        accounts.put(updated.getUsername(), updated);
        saveToDisk();
        return Optional.of(updated);
    }

    public int accountCount() { return accounts.size(); }

    // ─── Persistence — encrypted ─────────────────────────────────────────────

    private void loadFromDisk() {
        if (!Files.exists(dataFile)) return;
        try {
            byte[] raw     = Files.readAllBytes(dataFile);
            String encoded = new String(raw, StandardCharsets.UTF_8).trim();
            if (encoded.isBlank()) return;

            String csv = FileEncryptor.decrypt(encoded);   // AES-256-GCM decrypt

            csv.lines()
               .filter(line -> !line.isBlank())
               .map(CustomerAccount::fromCsvLine)
               .forEach(a -> accounts.put(a.getUsername(), a));

        } catch (GeneralSecurityException e) {
            System.err.println("  error: accounts file could not be decrypted — " + e.getMessage());
        } catch (IOException e) {
            System.err.println("  warning: could not read accounts — " + e.getMessage());
        }
    }

    private void saveToDisk() {
        // Build CSV in memory first
        String csv = accounts.values().stream()
                .map(CustomerAccount::toCsvLine)
                .collect(Collectors.joining(System.lineSeparator()));

        try {
            String encrypted = FileEncryptor.encrypt(csv);   // AES-256-GCM encrypt
            Files.writeString(dataFile, encrypted,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (GeneralSecurityException e) {
            System.err.println("  error: could not encrypt accounts — " + e.getMessage());
        } catch (IOException e) {
            System.err.println("  warning: could not save accounts — " + e.getMessage());
        }
    }
}
