package it.unibo.fantasyf1.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash adattivo e salato basato su PBKDF2-HMAC-SHA256.
 *
 * <p>Il formato persistito è
 * {@code pbkdf2-sha256$iterazioni$sale-base64$hash-base64}. Sono inoltre
 * verificabili gli hash SHA-256 esadecimali del seed storico; un login valido
 * con tale formato può quindi essere seguito da una migrazione trasparente.</p>
 */
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT_ID = "pbkdf2-sha256";
    private static final int ITERATIONS = 210_000;
    private static final int MAX_ACCEPTED_ITERATIONS = 2_000_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final Pattern LEGACY_SHA_256 =
        Pattern.compile("(?i)[0-9a-f]{64}");

    private final SecureRandom secureRandom;

    public Pbkdf2PasswordHasher() {
        this(new SecureRandom());
    }

    Pbkdf2PasswordHasher(final SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(
            secureRandom,
            "Il generatore casuale non può essere null"
        );
    }

    @Override
    public String hash(final String rawPassword) {
        requirePassword(rawPassword);
        final byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        final byte[] derived = derive(
            rawPassword,
            salt,
            ITERATIONS,
            HASH_BYTES
        );
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return "%s$%d$%s$%s".formatted(
            FORMAT_ID,
            ITERATIONS,
            encoder.encodeToString(salt),
            encoder.encodeToString(derived)
        );
    }

    @Override
    public boolean verify(
        final String rawPassword,
        final String encodedHash
    ) {
        if (rawPassword == null || encodedHash == null) {
            return false;
        }
        if (isLegacy(encodedHash)) {
            return verifyLegacy(rawPassword, encodedHash);
        }

        final EncodedPassword parsed = parse(encodedHash);
        if (parsed == null) {
            return false;
        }
        final byte[] candidate = derive(
            rawPassword,
            parsed.salt(),
            parsed.iterations(),
            parsed.hash().length
        );
        return MessageDigest.isEqual(candidate, parsed.hash());
    }

    @Override
    public boolean isLegacy(final String encodedHash) {
        return encodedHash != null
            && LEGACY_SHA_256.matcher(encodedHash).matches();
    }

    @Override
    public boolean needsRehash(final String encodedHash) {
        if (isLegacy(encodedHash)) {
            return true;
        }
        final EncodedPassword parsed = parse(encodedHash);
        return parsed == null
            || parsed.iterations() != ITERATIONS
            || parsed.salt().length != SALT_BYTES
            || parsed.hash().length != HASH_BYTES;
    }

    private static boolean verifyLegacy(
        final String rawPassword,
        final String encodedHash
    ) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] actual = digest.digest(
                rawPassword.getBytes(StandardCharsets.UTF_8)
            );
            final byte[] expected = HexFormat.of().parseHex(
                encodedHash.toLowerCase(Locale.ROOT)
            );
            return MessageDigest.isEqual(actual, expected);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                "SHA-256 non è disponibile nella JVM",
                exception
            );
        }
    }

    private static EncodedPassword parse(final String encodedHash) {
        if (encodedHash == null) {
            return null;
        }
        final String[] components = encodedHash.split("\\$", -1);
        if (components.length != 4 || !FORMAT_ID.equals(components[0])) {
            return null;
        }

        try {
            final int iterations = Integer.parseInt(components[1]);
            if (iterations <= 0 || iterations > MAX_ACCEPTED_ITERATIONS) {
                return null;
            }
            final Base64.Decoder decoder = Base64.getUrlDecoder();
            final byte[] salt = decoder.decode(components[2]);
            final byte[] hash = decoder.decode(components[3]);
            if (salt.length == 0 || hash.length == 0) {
                return null;
            }
            return new EncodedPassword(iterations, salt, hash);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static byte[] derive(
        final String rawPassword,
        final byte[] salt,
        final int iterations,
        final int hashBytes
    ) {
        final char[] password = rawPassword.toCharArray();
        final PBEKeySpec specification = new PBEKeySpec(
            password,
            salt,
            iterations,
            Math.multiplyExact(hashBytes, Byte.SIZE)
        );
        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                .generateSecret(specification)
                .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                "PBKDF2-HMAC-SHA256 non è disponibile nella JVM",
                exception
            );
        } finally {
            specification.clearPassword();
            Arrays.fill(password, '\0');
        }
    }

    private static void requirePassword(final String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException(
                "La password non può essere vuota"
            );
        }
    }

    private record EncodedPassword(
        int iterations,
        byte[] salt,
        byte[] hash
    ) {
    }
}
