package com.android.player.util;

/**
 * Created by wangye on 16-8-23.
 */
public class RotAlgo {

    /**
     * Applies a ROT-47 Caesar cipher to the supplied value.
     * @param value The text to be rotated.
     * @return The rotated text.
     */
    public static String rotate(String value) {
        int length = value.length();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            // Process letters, numbers, and symbols -- ignore spaces.
            if (c != ' ') {
                // Add 47 (it is ROT-47, after all).
                c += 47;
                if (c > '~')
                    c -= 94;
            }
            result.append(c);
        }

        return result.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        s = s.toLowerCase();
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
