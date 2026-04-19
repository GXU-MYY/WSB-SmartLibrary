package com.wsb.book.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public final class BookKeywordUtils {

    private BookKeywordUtils() {
    }

    public static List<String> splitKeywords(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }

        return Arrays.stream(keyword.split("[,，、;；|/]+"))
                .map(BookKeywordUtils::normalizeKeyword)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    private static String normalizeKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        return keyword.replaceAll("\\s+", " ").trim();
    }
}
