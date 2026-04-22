package com.wsb.rag.util;

import org.apache.commons.lang3.StringUtils;

public final class ClcCategoryUtils {

    private ClcCategoryUtils() {
    }

    public static String resolveCategory(String clc) {
        if (StringUtils.isBlank(clc)) {
            return null;
        }
        for (int i = 0; i < clc.length(); i++) {
            char code = Character.toUpperCase(clc.charAt(i));
            String category = resolveCategoryByCode(code);
            if (category != null) {
                return category;
            }
        }
        return null;
    }

    private static String resolveCategoryByCode(char code) {
        return switch (code) {
            case 'A' -> "马克思主义、列宁主义、毛泽东思想、邓小平理论";
            case 'B' -> "哲学、宗教";
            case 'C' -> "社会科学总论";
            case 'D' -> "政治、法律";
            case 'E' -> "军事";
            case 'F' -> "经济";
            case 'G' -> "文化、科学、教育、体育";
            case 'H' -> "语言、文字";
            case 'I' -> "文学";
            case 'J' -> "艺术";
            case 'K' -> "历史、地理";
            case 'N' -> "自然科学总论";
            case 'O' -> "数理科学和化学";
            case 'P' -> "天文学、地球科学";
            case 'Q' -> "生物科学";
            case 'R' -> "医药、卫生";
            case 'S' -> "农业科学";
            case 'T' -> "工业技术";
            case 'U' -> "交通运输";
            case 'V' -> "航空、航天";
            case 'X' -> "环境科学、安全科学";
            case 'Z' -> "综合性图书";
            default -> null;
        };
    }
}
