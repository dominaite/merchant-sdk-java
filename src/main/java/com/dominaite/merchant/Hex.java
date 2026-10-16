package com.dominaite.merchant;

/** Lowercase hex. {@code java.util.HexFormat} is Java 17, and this SDK runs on 11. */
final class Hex {

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private Hex() {
    }

    static String encode(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            out[i * 2] = DIGITS[(bytes[i] >> 4) & 0x0f];
            out[i * 2 + 1] = DIGITS[bytes[i] & 0x0f];
        }
        return new String(out);
    }

    /** Decodes lowercase hex only. The caller checks the grammar first. */
    static byte[] decodeLowercase(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) ((value(hex.charAt(i * 2)) << 4) | value(hex.charAt(i * 2 + 1)));
        }
        return out;
    }

    static boolean isLowercaseHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private static int value(char c) {
        return c <= '9' ? c - '0' : c - 'a' + 10;
    }
}
