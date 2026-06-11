package coffee.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordHasher — salted SHA-256 hashing using only the Java standard library.
 *
 * Why not String.hashCode()?
 *   - 32-bit int: only ~4 billion possible values, trivially brute-forced
 *   - No salt: identical passwords produce identical hashes (rainbow tables)
 *   - Not a cryptographic hash: collision-prone by design
 *
 * This implementation:
 *   - Generates a random 16-byte salt per password (SecureRandom)
 *   - Hashes salt+password with SHA-256 (256-bit output)
 *   - Stores as "base64(salt):base64(hash)" — both parts needed to verify
 *
 * Demonstrates: MessageDigest, SecureRandom, Base64, byte[] manipulation
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final int    SALT_BYTES = 16;

    private PasswordHasher() {}   // utility class — no instances

    /**
     * Hash a plaintext password, generating a fresh random salt.
     * @return "saltB64:hashB64" — store this string
     */
    public static String hash(String password) {
        byte[] salt = generateSalt();
        byte[] hash = digest(salt, password);
        return Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verify a plaintext password against a stored hash string.
     * Constant-time comparison via MessageDigest.isEqual prevents timing attacks.
     */
    public static boolean verify(String password, String stored) {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return false;
        byte[] salt         = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash   = digest(salt, password);
        return MessageDigest.isEqual(actualHash, expectedHash); // constant-time
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);   // cryptographically random
        return salt;
    }

    private static byte[] digest(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);                              // feed salt first
            return md.digest(password.getBytes());        // then hash password
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the Java spec — this cannot happen
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
