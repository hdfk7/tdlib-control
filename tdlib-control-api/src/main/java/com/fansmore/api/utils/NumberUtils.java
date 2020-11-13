package com.fansmore.api.utils;

import java.util.regex.Pattern;

public class NumberUtils {
    private static final Pattern INT = Pattern.compile("^-?[1-9]\\d*$");
    private static final Pattern DOUBLE = Pattern.compile("^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$");

    public static boolean isInt(String str) {
        return INT.matcher(str).matches();
    }

    public static boolean isDouble(String str) {
        return DOUBLE.matcher(str).matches();
    }

    public static boolean isNumber(String str) {
        return isInt(str) || isDouble(str);
    }

    public static boolean isAllNumber(String... str) {
        for (String s : str) {
            if (!isNumber(s)) {
                return false;
            }
        }
        return true;
    }
}
