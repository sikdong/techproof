package com.techproof.checker;

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

public class ContextualParticleChecker {
    private static final SourceInfo SOURCE = new SourceInfo(
        "국립국어원",
        "한국어 어문 규범 - 한글 맞춤법",
        "은/는은 주제·대조, 이/가는 주어·초점 문맥에서 자연스러운지 검토",
        "https://www.korean.go.kr/kornorms/main/main.do",
        "2026-05-04"
    );

    private static final Pattern SUBJECT_PARTICLE_PATTERN = Pattern.compile(
        "([\\uAC00-\\uD7A3A-Za-z0-9]+(?:[-_/\\.][\\uAC00-\\uD7A3A-Za-z0-9]+)*)(\\([^)]{1,30}\\))?(\uC740|\uB294|\uC774|\uAC00)(?=$|\\s|[.,;:!?\\)\\]\\}])"
    );
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?\uB2E4\uC694\uB2C8\uAE4C])\\s+");
    private static final Pattern CONTRAST_MARKER = Pattern.compile("(\uBC18\uBA74|\uADF8\uB7EC\uB098|\uD558\uC9C0\uB9CC|\uC774\uC640 \uB2EC\uB9AC|\uBC18\uB300\uB85C|\uBE44\uAD50)");
    private static final Pattern FOCUS_MARKER = Pattern.compile("(\uBB34\uC5C7|\uB204\uAC00|\uC5B4\uB5A4|\uC5B4\uB290|\uC5B4\uB514|\uC65C|\uC5B8\uC81C|\uC815\uD655\uD788|\uD2B9\uD788|\uBC14\uB85C)");
    private static final Pattern DEFINITION_MARKER = Pattern.compile("(\uB780|\uC774\uB780|\uC740\\s+.*\uB97C\\s+\uB9D0\uD55C\uB2E4|\uB294\\s+.*\uB97C\\s+\uB9D0\uD55C\uB2E4|\uC815\uC758\uB41C\uB2E4|\uC758\uBBF8\uD55C\uB2E4)");

    private final Komoran komoran;

    public ContextualParticleChecker() {
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
        Set<String> previousNouns = new HashSet<>();
        List<Token> posTokens = komoran == null ? List.of() : komoran.analyze(text).getTokenList();

        int sentenceStart = 0;
        Matcher splitter = SENTENCE_SPLIT.matcher(text);
        while (splitter.find()) {
            addSentenceResults(block, text, sentenceStart, splitter.start(), previousNouns, posTokens, results);
            sentenceStart = splitter.end();
        }
        addSentenceResults(block, text, sentenceStart, text.length(), previousNouns, posTokens, results);

        return results;
    }

    private void addSentenceResults(
        ParagraphBlock block,
        String fullText,
        int sentenceStart,
        int sentenceEnd,
        Set<String> previousNouns,
        List<Token> posTokens,
        List<CheckResult> results
    ) {
        if (sentenceStart >= sentenceEnd) {
            return;
        }

        String sentence = fullText.substring(sentenceStart, sentenceEnd);
        Matcher matcher = SUBJECT_PARTICLE_PATTERN.matcher(sentence);
        Set<String> nounsInSentence = new HashSet<>();
        int subjectCount = 0;

        while (matcher.find()) {
            subjectCount++;
            String noun = matcher.group(1);
            String drawingNo = matcher.group(2) == null ? "" : matcher.group(2);
            String particle = matcher.group(3);
            int particleStart = sentenceStart + matcher.start(3);
            int particleEnd = sentenceStart + matcher.end(3);
            if (!isParticleToken(posTokens, particleStart, particleEnd, particle)) {
                continue;
            }
            String normalizedNoun = normalizeNoun(noun);
            nounsInSentence.add(normalizedNoun);

            if (isContrastContext(sentence) || isDefinitionContext(sentence)) {
                continue;
            }
            if (isTopicParticle(particle) && shouldPreferSubjectParticle(sentence, matcher.start(), normalizedNoun, previousNouns, subjectCount)) {
                addResult(block, fullText, sentenceStart + matcher.start(), sentenceStart + matcher.end(), noun, drawingNo, particle, matchingSubjectParticle(particle), "First-mentioned or focus-like subject may read more naturally with 이/가.", results);
            } else if (isSubjectParticle(particle) && shouldPreferTopicParticle(normalizedNoun, previousNouns, subjectCount)) {
                addResult(block, fullText, sentenceStart + matcher.start(), sentenceStart + matcher.end(), noun, drawingNo, particle, matchingTopicParticle(particle), "Previously introduced subject may read more naturally with 은/는.", results);
            }
        }

        previousNouns.addAll(nounsInSentence);
    }

    private boolean shouldPreferSubjectParticle(String sentence, int particleStart, String noun, Set<String> previousNouns, int subjectCount) {
        return !previousNouns.contains(noun)
            && subjectCount == 1
            && (isFocusContext(sentence) || particleStart < Math.min(12, sentence.length()));
    }

    private boolean shouldPreferTopicParticle(String noun, Set<String> previousNouns, int subjectCount) {
        return previousNouns.contains(noun) && subjectCount == 1;
    }

    private boolean isTopicParticle(String particle) {
        return "\uC740".equals(particle) || "\uB294".equals(particle);
    }

    private boolean isSubjectParticle(String particle) {
        return "\uC774".equals(particle) || "\uAC00".equals(particle);
    }

    private String matchingSubjectParticle(String particle) {
        return "\uC740".equals(particle) ? "\uC774" : "\uAC00";
    }

    private String matchingTopicParticle(String particle) {
        return "\uC774".equals(particle) ? "\uC740" : "\uB294";
    }

    private boolean isContrastContext(String sentence) {
        return CONTRAST_MARKER.matcher(sentence).find() || countTopicParticles(sentence) >= 2;
    }

    private boolean isDefinitionContext(String sentence) {
        return DEFINITION_MARKER.matcher(sentence).find();
    }

    private boolean isFocusContext(String sentence) {
        return FOCUS_MARKER.matcher(sentence).find();
    }

    private int countTopicParticles(String sentence) {
        Matcher matcher = SUBJECT_PARTICLE_PATTERN.matcher(sentence);
        int count = 0;
        while (matcher.find()) {
            if (isTopicParticle(matcher.group(3))) {
                count++;
            }
        }
        return count;
    }

    private String normalizeNoun(String noun) {
        return noun == null ? "" : noun.toLowerCase();
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

    private void addResult(
        ParagraphBlock block,
        String fullText,
        int start,
        int end,
        String noun,
        String drawingNo,
        String particle,
        String suggestionParticle,
        String reason,
        List<CheckResult> results
    ) {
        String original = noun + drawingNo + particle;
        String suggestion = noun + drawingNo + suggestionParticle;
        results.add(new CheckResult(
            block.paragraphNo(),
            block.location(),
            IssueType.PARTICLE_CONTEXT,
            original,
            suggestion,
            reason,
            TextUtil.context(fullText, start, end),
            SOURCE
        ));
    }
}
