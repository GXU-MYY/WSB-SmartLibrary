package com.wsb.rag.util;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QueryTextAnalyzer {

    private static final Pattern LATIN_TOKEN = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_-]{1,}");
    private static final Pattern CJK_SEQUENCE = Pattern.compile("\\p{IsHan}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final List<String> INTENT_PHRASES = List.of(
            "我想看", "想看点", "想看", "帮我找", "帮我推荐", "推荐一下", "推荐",
            "有没有", "有没有关于", "找一本", "找点", "看点", "读点", "关于", "适合",
            "一点", "一些", "这类", "这种", "那种"
    );

    private static final List<String> DOMAIN_TERMS = List.of(
            "轻松", "治愈", "温暖", "幽默", "历史", "故事", "小说", "文学", "名著",
            "散文", "诗歌", "传记", "哲学", "宗教", "心理", "教育", "经济", "管理",
            "法律", "政治", "军事", "艺术", "语言", "英语", "儿童", "科普", "科学",
            "数学", "物理", "化学", "生物", "医学", "农业", "工业", "计算机",
            "编程", "人工智能", "数据", "安全", "环境", "地理", "悬疑", "推理",
            "爱情", "古代", "现代", "外国", "中国"
    );

    private static final Set<String> STOP_TERMS = Set.of(
            "我们", "你们", "他们", "这个", "那个", "一本", "一些", "一点", "有关",
            "关于", "推荐", "看看", "想看", "看点", "读点", "图书", "书籍"
    );

    private QueryTextAnalyzer() {
    }

    public static List<String> expandQueries(String query, int maxQueries) {
        String normalized = normalize(query);
        if (StringUtils.isBlank(normalized)) {
            return List.of();
        }

        LinkedHashSet<String> queries = new LinkedHashSet<>();
        queries.add(normalized);

        String intentRemoved = removeIntentWords(normalized);
        if (StringUtils.isNotBlank(intentRemoved)) {
            queries.add(intentRemoved);
        }

        List<String> terms = extractTerms(normalized, 8);
        if (!terms.isEmpty()) {
            queries.add(String.join(" ", terms));
        }

        return queries.stream()
                .filter(StringUtils::isNotBlank)
                .limit(Math.max(maxQueries, 1))
                .toList();
    }

    public static List<String> extractTerms(String query, int maxTerms) {
        String normalized = normalize(query);
        if (StringUtils.isBlank(normalized)) {
            return List.of();
        }

        LinkedHashSet<String> terms = new LinkedHashSet<>();
        collectLatinTerms(normalized, terms, maxTerms);
        collectChineseTerms(normalized, terms, maxTerms);

        return terms.stream()
                .filter(term -> term.length() >= 2)
                .filter(term -> !STOP_TERMS.contains(term))
                .limit(Math.max(maxTerms, 1))
                .toList();
    }

    public static String normalize(String query) {
        if (StringUtils.isBlank(query)) {
            return "";
        }
        return WHITESPACE.matcher(query.trim()).replaceAll(" ");
    }

    private static void collectLatinTerms(String normalized, LinkedHashSet<String> terms, int maxTerms) {
        Matcher matcher = LATIN_TOKEN.matcher(normalized.toLowerCase(Locale.ROOT));
        while (matcher.find() && terms.size() < maxTerms) {
            terms.add(matcher.group());
        }
    }

    private static void collectChineseTerms(String normalized, LinkedHashSet<String> terms, int maxTerms) {
        Matcher matcher = CJK_SEQUENCE.matcher(normalized);
        while (matcher.find() && terms.size() < maxTerms) {
            String sequence = removeIntentWords(matcher.group());
            if (StringUtils.isBlank(sequence)) {
                continue;
            }

            for (String term : DOMAIN_TERMS) {
                if (sequence.contains(term)) {
                    terms.add(term);
                    if (terms.size() >= maxTerms) {
                        return;
                    }
                }
            }

            if (terms.size() >= maxTerms) {
                return;
            }

            if (sequence.length() <= 6) {
                terms.add(sequence);
            } else {
                collectNgrams(sequence, terms, maxTerms);
            }
        }
    }

    private static void collectNgrams(String sequence, LinkedHashSet<String> terms, int maxTerms) {
        for (int i = 0; i + 2 <= sequence.length() && terms.size() < maxTerms; i++) {
            terms.add(sequence.substring(i, i + 2));
        }
    }

    private static String removeIntentWords(String text) {
        String cleaned = StringUtils.defaultString(text);
        for (String phrase : INTENT_PHRASES) {
            cleaned = cleaned.replace(phrase, "");
        }
        cleaned = cleaned.replace("的", "")
                .replace("了", "")
                .replace("吗", "")
                .replace("呢", "")
                .replace("吧", "");
        return normalize(cleaned);
    }
}
