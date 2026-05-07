package com.techproof.checker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.techproof.model.CheckResult;
import com.techproof.model.IssueType;
import com.techproof.model.ParagraphBlock;
import com.techproof.model.SourceInfo;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MorphologyChecker {
    private static final Pattern HANGUL_TOKEN = Pattern.compile("[\\p{IsHangul}]+");
    private static final SourceInfo MORPHOLOGY_SOURCE = new SourceInfo(
        "국립국어원",
        "한국어 어문 규범 - 한글 맞춤법",
        "용언 활용과 준말/본말 표기",
        "https://www.korean.go.kr/kornorms/main/main.do",
        "2026-05-04"
    );

    private static final List<SuffixRule> DEFAULT_RULES = List.of(
        new SuffixRule("\uB42C", "\uB410", "Incorrect past-tense stem form."),
        new SuffixRule("\uB418\uC5EC", "\uB418\uC5B4", "Use standard contraction form."),
        new SuffixRule("\uD558\uC600", "\uD588", "Use modern contracted form in plain style.")
    );

    private final List<SuffixRule> rules;
    private final Komoran komoran;

    public MorphologyChecker() {
        this.rules = RuleResourceLoader.loadList(
            "/rules/morphology-suffix-rules.json",
            new TypeReference<List<SuffixRule>>() {},
            DEFAULT_RULES
        );

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

        if (komoran != null) {
            addPosBasedResults(block, results, seen);
        }

        addSuffixFallbackResults(block, results, seen);
        return results;
    }

    private void addPosBasedResults(ParagraphBlock block, List<CheckResult> results, Set<String> seen) {
        String text = block.text();
        List<Token> tokens = komoran.analyze(text).getTokenList();

        for (int i = 0; i < tokens.size() - 1; i++) {
            Token current = tokens.get(i);
            Token next = tokens.get(i + 1);

            if (isDoeoContraction(current, next)) {
                addResult(block, text, current.getBeginIndex(), next.getEndIndex(), "\uB418\uC5B4", "\uB3FC", "Contraction is preferred in normal prose.", MORPHOLOGY_SOURCE, results, seen);
            }

            if (isHaetContraction(current, next)) {
                addResult(block, text, current.getBeginIndex(), next.getEndIndex(), "\uD558\uC600", "\uD588", "Contraction is preferred in normal prose.", MORPHOLOGY_SOURCE, results, seen);
            }

            if (i > 0 && isAnDoeeoPattern(tokens.get(i - 1), current, next)) {
                Token prev = tokens.get(i - 1);
                addResult(block, text, prev.getBeginIndex(), next.getEndIndex(), "\uC548 \uB418\uC5B4", "\uC548 \uB3FC", "Contraction is preferred in normal prose.", MORPHOLOGY_SOURCE, results, seen);
            }

            if (isDoelSooSpacingPattern(current, next)) {
                addResult(block, text, current.getBeginIndex(), next.getEndIndex(), "\uB420\uC218", "\uB420 \uC218", "Bound noun 'su' should be separated.", MORPHOLOGY_SOURCE, results, seen);
            }

            if (isHalGeotSpacingPattern(current, next)) {
                addResult(block, text, current.getBeginIndex(), next.getEndIndex(), "\uD560\uAC83", "\uD560 \uAC83", "Bound noun 'geot' should be separated.", MORPHOLOGY_SOURCE, results, seen);
            }
        }
    }

    private boolean isDoeoContraction(Token current, Token next) {
        return "\uB418".equals(current.getMorph())
            && current.getPos().startsWith("VV")
            && "\uC5B4".equals(next.getMorph())
            && next.getPos().startsWith("EC");
    }

    private boolean isHaetContraction(Token current, Token next) {
        return "\uD558".equals(current.getMorph())
            && current.getPos().startsWith("VV")
            && "\uC600".equals(next.getMorph())
            && next.getPos().startsWith("EP");
    }

    private boolean isAnDoeeoPattern(Token prev, Token current, Token next) {
        return "\uC548".equals(prev.getMorph())
            && prev.getPos().startsWith("MAG")
            && "\uB418".equals(current.getMorph())
            && current.getPos().startsWith("VV")
            && "\uC5B4".equals(next.getMorph())
            && next.getPos().startsWith("EC");
    }

    private boolean isDoelSooSpacingPattern(Token current, Token next) {
        return "\uB420".equals(current.getMorph())
            && current.getPos().startsWith("ETM")
            && "\uC218".equals(next.getMorph())
            && next.getPos().startsWith("NNB");
    }

    private boolean isHalGeotSpacingPattern(Token current, Token next) {
        return "\uD560".equals(current.getMorph())
            && current.getPos().startsWith("ETM")
            && "\uAC83".equals(next.getMorph())
            && next.getPos().startsWith("NNB");
    }

    private void addSuffixFallbackResults(ParagraphBlock block, List<CheckResult> results, Set<String> seen) {
        String text = block.text();
        Matcher tokenMatcher = HANGUL_TOKEN.matcher(text);

        while (tokenMatcher.find()) {
            String token = tokenMatcher.group();
            for (SuffixRule rule : rules) {
                if (rule.wrongPart == null || rule.wrongPart.isBlank()) {
                    continue;
                }

                int idx = token.indexOf(rule.wrongPart);
                if (idx < 0) {
                    continue;
                }

                String suggestion = token.replace(rule.wrongPart, rule.correctPart);
                if (suggestion.equals(token)) {
                    continue;
                }

                addResult(block, text, tokenMatcher.start(), tokenMatcher.end(), token, suggestion, rule.reason, rule.source, results, seen);
                break;
            }
        }
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
        Set<String> seen
    ) {
        String key = start + ":" + end + ":" + original + ":" + suggestion;
        if (!seen.add(key)) {
            return;
        }

        results.add(new CheckResult(
            block.paragraphNo(),
            block.location(),
            IssueType.MORPHOLOGY,
            original,
            suggestion,
            reason,
            TextUtil.context(text, start, end),
            source == null ? SourceInfo.NONE : source
        ));
    }

    public static class SuffixRule {
        public String wrongPart;
        public String correctPart;
        public String reason;
        public SourceInfo source = SourceInfo.NONE;

        public SuffixRule() {
        }

        public SuffixRule(String wrongPart, String correctPart, String reason) {
            this.wrongPart = wrongPart;
            this.correctPart = correctPart;
            this.reason = reason;
            this.source = SourceInfo.NONE;
        }
    }
}
