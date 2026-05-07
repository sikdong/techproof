package com.techproof.checker;

import com.techproof.model.CheckResult;
import com.techproof.model.IssueType;
import com.techproof.model.ParagraphBlock;
import com.techproof.model.SourceInfo;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParticleChecker {
    private static final SourceInfo PARTICLE_SOURCE = new SourceInfo(
        "국립국어원",
        "한국어 어문 규범 - 한글 맞춤법",
        "조사 선택은 앞말의 받침 여부에 따라 판단",
        "https://www.korean.go.kr/kornorms/main/main.do",
        "2026-05-04"
    );

    // Immediately preceding token (+ optional parenthesized drawing marker) + particle.
    private static final Pattern PARTICLE_PATTERN = Pattern.compile(
        "([\\uAC00-\\uD7A3A-Za-z0-9]+(?:[-_/\\.][\\uAC00-\\uD7A3A-Za-z0-9]+)*)(\\([^)]{1,30}\\))?(\uC740|\uB294|\uC774|\uAC00|\uC744|\uB97C)(?=$|\\s|[.,;:!?\\)\\]\\}])"
    );

    private final Komoran komoran;

    public ParticleChecker() {
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
        String text = block.text();
        Matcher matcher = PARTICLE_PATTERN.matcher(text);
        List<Token> posTokens = komoran == null ? List.of() : komoran.analyze(text).getTokenList();

        while (matcher.find()) {
            String nounPhrase = matcher.group(1);
            String drawingNo = matcher.group(2) == null ? "" : matcher.group(2);
            String particle = matcher.group(3);
            if (!isParticleToken(posTokens, matcher.start(3), matcher.end(3), particle)) {
                continue;
            }

            String base = nounPhrase == null ? "" : nounPhrase.trim();
            if (base.isEmpty()) {
                continue;
            }

            FinalSound finalSound = finalSound(base);
            if (finalSound == FinalSound.UNKNOWN) {
                continue;
            }

            String expected = expectedParticle(particle, finalSound);
            if (expected == null || expected.equals(particle)) {
                continue;
            }

            String original = nounPhrase + drawingNo + particle;
            String suggestion = nounPhrase + drawingNo + expected;
            String reason = "Particle mismatch by final consonant rule.";

            results.add(new CheckResult(
                block.paragraphNo(),
                block.location(),
                IssueType.PARTICLE,
                original,
                suggestion,
                reason,
                TextUtil.context(text, matcher.start(), matcher.end()),
                PARTICLE_SOURCE
            ));
        }

        return results;
    }

    private boolean isParticleToken(List<Token> posTokens, int start, int end, String particle) {
        if (komoran == null || posTokens.isEmpty()) {
            return true;
        }

        for (Token token : posTokens) {
            if (token.getBeginIndex() == start && token.getEndIndex() == end && particle.equals(token.getMorph())) {
                return token.getPos().startsWith("J");
            }
        }
        return false;
    }

    private FinalSound finalSound(String text) {
        for (int i = text.length() - 1; i >= 0; i--) {
            char ch = text.charAt(i);
            if (TextUtil.isHangulSyllable(ch)) {
                return hangulFinalSound(ch);
            }
            if (Character.isDigit(ch)) {
                return digitFinalSound(ch);
            }
            if (isAsciiLetter(ch)) {
                return latinFinalSound(text.substring(0, i + 1));
            }
        }
        return FinalSound.UNKNOWN;
    }

    private FinalSound hangulFinalSound(char ch) {
        if (!TextUtil.hasBatchim(ch)) {
            return FinalSound.VOWEL;
        }
        return FinalSound.CONSONANT;
    }

    private FinalSound digitFinalSound(char ch) {
        return switch (ch) {
            case '0', '3', '6' -> FinalSound.CONSONANT; // 영, 삼, 육
            case '1', '7', '8' -> FinalSound.CONSONANT; // 일, 칠, 팔
            case '2', '4', '5', '9' -> FinalSound.VOWEL; // 이, 사, 오, 구
            default -> FinalSound.UNKNOWN;
        };
    }

    private FinalSound latinFinalSound(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        char last = upper.charAt(upper.length() - 1);

        if (upper.endsWith("XML") || upper.endsWith("HTML") || upper.endsWith("URL") || last == 'L' || last == 'M' || last == 'N' || last == 'R') {
            return FinalSound.CONSONANT;
        }

        return switch (last) {
            case 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'O', 'P', 'Q', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' -> FinalSound.VOWEL;
            default -> FinalSound.UNKNOWN;
        };
    }

    private boolean isAsciiLetter(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    private String expectedParticle(String actual, FinalSound finalSound) {
        boolean hasBatchim = finalSound == FinalSound.CONSONANT;
        return switch (actual) {
            case "\uC740", "\uB294" -> hasBatchim ? "\uC740" : "\uB294";
            case "\uC774", "\uAC00" -> hasBatchim ? "\uC774" : "\uAC00";
            case "\uC744", "\uB97C" -> hasBatchim ? "\uC744" : "\uB97C";
            default -> null;
        };
    }

    private enum FinalSound {
        CONSONANT,
        VOWEL,
        UNKNOWN
    }
}
