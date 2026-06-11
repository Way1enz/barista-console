package coffee.util;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.file.*;
import java.security.*;
import java.util.Base64;

/**
 * FileEncryptor — AES-256-GCM encryption for the accounts file.
 *
 * AES-GCM (Galois/Counter Mode):
 *   - Authenticated encryption: detects tampering (unlike plain AES-CBC)
 *   - 256-bit key derived from a passphrase using PBKDF2 + random salt
 *   - 12-byte random IV (initialisation vector) per encryption operation
 *   - File format: base64(salt) : base64(iv) : base64(ciphertext)
 *
 * The key is derived from a fixed app-level passphrase + a per-file salt.
 * In a real system the passphrase would come from an env variable or keystore.
 *
 * Demonstrates: javax.crypto, KeySpec, SecretKeyFactory, GCMParameterSpec, Base64
 */
public final class FileEncryptor {

    private static final String APP_PASSPHRASE = "BrewAndBean-SecretKey-2024";
    private static final String KEY_ALGORITHM  = "PBKDF2WithHmacSHA256";
    private static final String CIPHER         = "AES/GCM/NoPadding";
    private static final int    KEY_BITS        = 256;
    private static final int    ITERATIONS      = 65_536;
    private static final int    SALT_BYTES      = 16;
    private static final int    IV_BYTES        = 12;
    private static final int    GCM_TAG_BITS    = 128;

    private FileEncryptor() {}

    /**
     * Encrypt plaintext and return the encoded string to write to disk.
     * Format: saltB64:ivB64:ciphertextB64
     */
    public static String encrypt(String plaintext) throws GeneralSecurityException {
        byte[] salt      = randomBytes(SALT_BYTES);
        byte[] iv        = randomBytes(IV_BYTES);
        SecretKey key    = deriveKey(salt);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

        Base64.Encoder enc = Base64.getEncoder();
        return enc.encodeToString(salt) + ":"
             + enc.encodeToString(iv)   + ":"
             + enc.encodeToString(ciphertext);
    }

    /**
     * Decrypt an encoded string previously produced by encrypt().
     * Throws AEADBadTagException if the file was tampered with.
     */
    public static String decrypt(String encoded) throws GeneralSecurityException {
        String[] parts = encoded.split(":", 3);
        if (parts.length != 3) throw new IllegalArgumentException("Invalid encrypted format.");

        Base64.Decoder dec  = Base64.getDecoder();
        byte[] salt         = dec.decode(parts[0]);
        byte[] iv           = dec.decode(parts[1]);
        byte[] ciphertext   = dec.decode(parts[2]);
        SecretKey key       = deriveKey(salt);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(ciphertext));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static SecretKey deriveKey(byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(
                APP_PASSPHRASE.toCharArray(), salt, ITERATIONS, KEY_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }
}
