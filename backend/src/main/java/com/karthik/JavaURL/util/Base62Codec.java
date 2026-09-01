package com.karthik.JavaURL.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Base62 codec (digits + lowercase + uppercase) plus a cryptographically strong
 * random code generator used to mint short codes.
 */
public final class Base62Codec {

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final int BASE = ALPHABET.length();

    private Base62Codec() {
    }

    /** Encodes a non-negative number as a Base62 string. */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative");
        }
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    /** Decodes a Base62 string back into a number. */
    public static long decode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code must not be empty");
        }
        long result = 0;
        for (char c : code.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: '" + c + "'");
            }
            result = Math.addExact(Math.multiplyExact(result, BASE), index);
        }
        return result;
    }

    /**
     * Generates a random code of the given length using the supplied random source.
     * 62^7 ≈ 3.5 trillion combinations, so collisions are rare at any realistic scale.
     */
    public static String randomCode(int length, Random random) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(BASE)));
        }
        return sb.toString();
    }

    /** Generates a random code of the given length backed by a {@link SecureRandom}. */
    public static String randomCode(int length) {
        return randomCode(length, new SecureRandom());
    }

    /** Number of possible codes for a given length (as a BigInteger to avoid overflow). */
    public static BigInteger combinations(int length) {
        return BigInteger.valueOf(BASE).pow(length);
    }
}