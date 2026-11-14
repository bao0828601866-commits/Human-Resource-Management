package com.hr.utils;

public final class EnumUtil {
    private EnumUtil() { }

    /** Đọc chuỗi từ DB thành enum; null -> null. */
    public static <E extends Enum<E>> E parse(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value.trim());
    }
}
