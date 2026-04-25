package com.wsb.book.util;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BookClcUtils {

    private static final Map<Character, String> CLC_CATEGORY_NAMES = new LinkedHashMap<>();

    static {
        CLC_CATEGORY_NAMES.put('A', "马克思主义、列宁主义、毛泽东思想、邓小平理论");
        CLC_CATEGORY_NAMES.put('B', "哲学、宗教");
        CLC_CATEGORY_NAMES.put('C', "社会科学总论");
        CLC_CATEGORY_NAMES.put('D', "政治、法律");
        CLC_CATEGORY_NAMES.put('E', "军事");
        CLC_CATEGORY_NAMES.put('F', "经济");
        CLC_CATEGORY_NAMES.put('G', "文化、科学、教育、体育");
        CLC_CATEGORY_NAMES.put('H', "语言、文字");
        CLC_CATEGORY_NAMES.put('I', "文学");
        CLC_CATEGORY_NAMES.put('J', "艺术");
        CLC_CATEGORY_NAMES.put('K', "历史、地理");
        CLC_CATEGORY_NAMES.put('N', "自然科学总论");
        CLC_CATEGORY_NAMES.put('O', "数理科学和化学");
        CLC_CATEGORY_NAMES.put('P', "天文学、地球科学");
        CLC_CATEGORY_NAMES.put('Q', "生物科学");
        CLC_CATEGORY_NAMES.put('R', "医药、卫生");
        CLC_CATEGORY_NAMES.put('S', "农业科学");
        CLC_CATEGORY_NAMES.put('T', "工业技术");
        CLC_CATEGORY_NAMES.put('U', "交通运输");
        CLC_CATEGORY_NAMES.put('V', "航空、航天");
        CLC_CATEGORY_NAMES.put('X', "环境科学、安全科学");
        CLC_CATEGORY_NAMES.put('Z', "综合性图书");
    }

    private BookClcUtils() {
    }

    public static String resolveCategory(String clc) {
        Character code = resolveCode(clc);
        if (code == null) {
            return "未分类";
        }

        String categoryName = CLC_CATEGORY_NAMES.get(code);
        if (categoryName == null) {
            return "其他";
        }
        return categoryName;
    }

    private static Character resolveCode(String clc) {
        if (StringUtils.isBlank(clc)) {
            return null;
        }

        for (int i = 0; i < clc.length(); i++) {
            char current = Character.toUpperCase(clc.charAt(i));
            if (current >= 'A' && current <= 'Z') {
                return current;
            }
        }
        return null;
    }
}
