package com.techproof.checker;

import com.techproof.dictionary.TypoDictionary;
import com.techproof.model.CheckResult;
import com.techproof.model.IssueType;
import com.techproof.model.ParagraphBlock;
import com.techproof.model.SourceInfo;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypoChecker {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHangul}\\p{IsAlphabetic}\\p{IsDigit}]+");
    private static final Pattern CONTEXTUAL_DI_PATTERN = Pattern.compile("([\\p{IsHangul}]{2,}디)(\\s+)([\\p{IsHangul}]+)");
    private static final SourceInfo SPELLING_SOURCE = new SourceInfo(
        "국립국어원",
        "한국어 어문 규범 - 한글 맞춤법",
        "맞춤법 자주 틀리는 표현 점검 규칙",
        "https://www.korean.go.kr/kornorms/main/main.do",
        "2026-05-04"
    );
    private static final List<SpellingRule> SPELLING_RULES = List.of(
        new SpellingRule(Pattern.compile("\uC54A\uB410"), "\uC548 \uB410", "Use the adverb '안' for simple negation before '됐다'."),
        new SpellingRule(Pattern.compile("\uC54A\uB41C"), "\uC548 \uB41C", "Use the adverb '안' for simple negation before '된'."),
        new SpellingRule(Pattern.compile("\uC54A\uB420"), "\uC548 \uB420", "Use the adverb '안' for simple negation before '될'."),
        new SpellingRule(Pattern.compile("\uB42C"), "\uB410", "Use '됐' for the contracted past form of '되다'."),
        new SpellingRule(Pattern.compile("\uB400"), "\uB41C", "Use '된' for the modifier form of '되다'."),
        new SpellingRule(Pattern.compile("\uB404"), "\uB420", "Use '될' for the modifier form of '되다'."),
        new SpellingRule(Pattern.compile("\uB3FC\uB2E4"), "\uB418\uB2E4", "Use the base verb form '되다'."),
        new SpellingRule(Pattern.compile("\uB3FC\uACE0"), "\uB418\uACE0", "Use '되고' in this connective form."),
        new SpellingRule(Pattern.compile("\uB3FC\uB294"), "\uB418\uB294", "Use '되는' in this modifier form."),
        new SpellingRule(Pattern.compile("\uB418\uC694"), "\uB3FC\uC694", "Use '돼요' for the polite ending."),
        new SpellingRule(Pattern.compile("\uB418\uC11C"), "\uB3FC\uC11C", "Use '돼서' for the contracted connective form."),
        new SpellingRule(Pattern.compile("\uC54A\uB418"), "\uC548 \uB418", "Use the adverb '안' for simple negation before '되다'."),
        new SpellingRule(Pattern.compile("\uC54A\uB3FC"), "\uC548 \uB3FC", "Use the adverb '안' for simple negation before '돼'.")
    );
    private static final Set<String> SINGLE_JOSA = Set.of(
        "\uC740", "\uB294", "\uC774", "\uAC00", "\uC744", "\uB97C", "\uC640", "\uACFC", "\uB3C4", "\uC758", "\uC5D0"
    );
    private static final Set<String> DOUBLE_JOSA = Set.of(
        "\uC5D0\uC11C", "\uC5D0\uAC8C", "\uC5D0\uAC8C\uC11C", "\uC73C\uB85C", "\uB85C\uC11C", "\uB85C", "\uB9CC", "\uAE4C\uC9C0", "\uBD80\uD130"
    );
    private static final Set<String> DI_EXCEPTION_WORDS = Set.of(
        "어디", "부디", "잔디", "마디", "반디", "깍디", "꼭디"
    );
    private static final List<String> PASSIVE_CONTEXT_STEMS = List.of(
        "수행", "생성", "갱신", "저장", "출력", "제공", "검출", "산출", "결정", "선택",
        "적용", "표시", "전송", "수신", "변경", "설정", "변환", "연산", "계산", "비교",
        "판단", "제어", "처리", "분리", "결합", "연결", "배치", "삽입", "삭제", "추출"
    );
    private static final List<String> OBJECT_CONTEXT_PREFIXES = List.of(
        "수행하", "생성하", "갱신하", "저장하", "출력하", "제공하", "검출하", "산출하", "결정하", "선택하",
        "적용하", "표시하", "전송하", "수신하", "변경하", "설정하", "변환하", "연산하", "계산하", "비교하",
        "판단하", "제어하", "처리하", "포함하", "구비하", "획득하", "추출하", "분리하", "결합하", "연결하"
    );

    private final TypoDictionary dictionary;
    private final Set<String> loanwordWhitelist;
    private final Komoran komoran;

    public TypoChecker(TypoDictionary dictionary) {
        this.dictionary = dictionary;
        this.loanwordWhitelist = loadLoanwordWhitelist();
        Komoran analyzer;
        try {
            analyzer = new Komoran(DEFAULT_MODEL.FULL);
        } catch (Exception ex) {
            analyzer = null;
        }
        this.komoran = analyzer;
    }

    public List<CheckResult> check(ParagraphBlock block) {
        List<CheckResult> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<Span> occupiedSpans = new ArrayList<>();
        String text = block.text();
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(text);
        List<Token> posTokens = komoran == null ? List.of() : komoran.analyze(text).getTokenList();

        addDictionaryPhraseResults(block, results, seen, occupiedSpans);
        addContextualDiTypoResults(block, results, seen, occupiedSpans);

        while (tokenMatcher.find()) {
            String token = tokenMatcher.group();
            String normalized = normalizeToken(token);
            int start = tokenMatcher.start();
            int end = tokenMatcher.end();
            if (isWhitelisted(token, normalized)) {
                continue;
            }

            String correct = dictionary.entries().get(token);
            String sourceKey = token;
            if ((correct == null || correct.isBlank()) && !normalized.equals(token)) {
                correct = dictionary.entries().get(normalized);
                sourceKey = normalized;
                if (correct != null && !correct.isBlank()) {
                    correct = correct + token.substring(normalized.length());
                }
            }
            if (correct == null || correct.isBlank()) {
                continue;
            }
            if (shouldSkipByPos(posTokens, start, end) && sourceKey.equals(token)) {
                continue;
            }

            addResult(
                block,
                text,
                start,
                end,
                token,
                correct,
                "Dictionary-based typo candidate.",
                dictionary.sourceFor(sourceKey),
                results,
                seen,
                occupiedSpans
            );
        }

        addSpellingRuleResults(block, results, seen, occupiedSpans);

        return results;
    }

    private void addDictionaryPhraseResults(
        ParagraphBlock block,
        List<CheckResult> results,
        Set<String> seen,
        List<Span> occupiedSpans
    ) {
        String text = block.text();

        for (Map.Entry<String, String> entry : dictionary.entries().entrySet()) {
            String wrong = entry.getKey();
            String correct = entry.getValue();
            if (wrong == null || wrong.isBlank() || correct == null || correct.isBlank()) {
                continue;
            }

            int from = 0;
            while (from < text.length()) {
                int idx = text.indexOf(wrong, from);
                if (idx < 0) {
                    break;
                }

                int end = idx + wrong.length();
                if (!isDictionaryBoundary(text, idx, end)) {
                    from = end;
                    continue;
                }

                addResult(
                    block,
                    text,
                    idx,
                    end,
                    wrong,
                    correct,
                    "Dictionary-based typo candidate.",
                    dictionary.sourceFor(wrong),
                    results,
                    seen,
                    occupiedSpans
                );
                from = end;
            }
        }
    }

    private void addSpellingRuleResults(
        ParagraphBlock block,
        List<CheckResult> results,
        Set<String> seen,
        List<Span> occupiedSpans
    ) {
        String text = block.text();

        for (SpellingRule rule : SPELLING_RULES) {
            Matcher matcher = rule.pattern.matcher(text);
            while (matcher.find()) {
                addResult(
                    block,
                    text,
                    matcher.start(),
                    matcher.end(),
                    matcher.group(),
                    rule.pattern.matcher(matcher.group()).replaceFirst(rule.suggestion),
                    rule.reason,
                    SPELLING_SOURCE,
                    results,
                    seen,
                    occupiedSpans
                );
            }
        }
    }

    private void addContextualDiTypoResults(
        ParagraphBlock block,
        List<CheckResult> results,
        Set<String> seen,
        List<Span> occupiedSpans
    ) {
        String text = block.text();
        Matcher matcher = CONTEXTUAL_DI_PATTERN.matcher(text);

        while (matcher.find()) {
            String original = matcher.group(1);
            String nextWord = matcher.group(3);
            if (DI_EXCEPTION_WORDS.contains(original)) {
                continue;
            }

            String noun = original.substring(0, original.length() - 1);
            if (noun.length() < 2 || !TextUtil.isHangulSyllable(noun.charAt(noun.length() - 1))) {
                continue;
            }

            String particle = contextualParticleFor(nextWord, noun);
            if (particle.isBlank()) {
                continue;
            }

            addResult(
                block,
                text,
                matcher.start(1),
                matcher.end(1),
                original,
                noun + particle,
                "Contextual typo candidate: final '디' may be a mistyped Korean particle.",
                SourceInfo.NONE,
                results,
                seen,
                occupiedSpans
            );
        }
    }

    private String contextualParticleFor(String nextWord, String noun) {
        if (startsWithPassiveContext(nextWord)) {
            return TextUtil.hasBatchim(noun.charAt(noun.length() - 1)) ? "이" : "가";
        }
        if (startsWithAny(nextWord, OBJECT_CONTEXT_PREFIXES)) {
            return TextUtil.hasBatchim(noun.charAt(noun.length() - 1)) ? "을" : "를";
        }
        return "";
    }

    private boolean startsWithAny(String value, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithPassiveContext(String value) {
        for (String stem : PASSIVE_CONTEXT_STEMS) {
            if (value.startsWith(stem + "되")
                || value.startsWith(stem + "된")
                || value.startsWith(stem + "될")
                || value.startsWith(stem + "됨")) {
                return true;
            }
        }
        return false;
    }

    private void addResult(
        ParagraphBlock block,
        String text,
        int start,
        int end,
        String original,
        String suggestion,
        String reason,
        SourceInfo source,
        List<CheckResult> results,
        Set<String> seen,
        List<Span> occupiedSpans
    ) {
        if (original == null || suggestion == null || original.equals(suggestion)) {
            return;
        }
        if (overlapsExistingTypo(occupiedSpans, start, end)) {
            return;
        }
        String key = start + ":" + end + ":" + original + ":" + suggestion;
        if (!seen.add(key)) {
            return;
        }

        results.add(new CheckResult(
                block.paragraphNo(),
                block.location(),
                IssueType.TYPO,
                original,
                suggestion,
                reason,
                TextUtil.context(text, start, end),
                source
        ));
        occupiedSpans.add(new Span(start, end));
    }

    private boolean overlapsExistingTypo(List<Span> occupiedSpans, int start, int end) {
        for (Span span : occupiedSpans) {
            if (start < span.end() && span.start() < end) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldSkipByPos(List<Token> posTokens, int start, int end) {
        for (Token t : posTokens) {
            if (t.getBeginIndex() == start && t.getEndIndex() == end) {
                String pos = t.getPos();
                if ("NNP".equals(pos) || "SL".equals(pos) || "NNG".equals(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWhitelisted(String token, String normalized) {
        return loanwordWhitelist.contains(token.toLowerCase())
            || loanwordWhitelist.contains(normalized.toLowerCase());
    }

    private boolean isDictionaryBoundary(String text, int start, int end) {
        boolean leftWord = start > 0 && TextUtil.isKoreanWordChar(text.charAt(start - 1));
        boolean rightWord = end < text.length() && TextUtil.isKoreanWordChar(text.charAt(end));
        return !(leftWord || rightWord);
    }

    private String normalizeToken(String token) {
        if (token.length() < 2) {
            return token;
        }

        String lower = token.toLowerCase();
        for (String josa : DOUBLE_JOSA) {
            if (lower.endsWith(josa) && lower.length() > josa.length() + 1) {
                return token.substring(0, token.length() - josa.length());
            }
        }

        String last = token.substring(token.length() - 1);
        if (SINGLE_JOSA.contains(last)) {
            return token.substring(0, token.length() - 1);
        }

        return token;
    }

    private Set<String> loadLoanwordWhitelist() {
        Set<String> words = new HashSet<>();
        try (InputStream in = TypoChecker.class.getResourceAsStream("/dictionary/loanword-whitelist.txt")) {
            if (in == null) {
                return words;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    words.add(trimmed.toLowerCase());
                }
            }
        } catch (Exception ignored) {
            return words;
        }
        return words;
    }

    private record SpellingRule(Pattern pattern, String suggestion, String reason) {
    }

    private record Span(int start, int end) {
    }
}
