package com.techproof.checker;

import com.techproof.model.CheckResult;
import com.techproof.model.IssueType;
import com.techproof.model.ParagraphBlock;
import com.techproof.model.ReferenceSignEntry;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReferenceSignChecker {
    private static final int MAX_NAME_WORDS = 3;
    private static final Pattern REFERENCE_SIGN_PATTERN = Pattern.compile("\\(\\s*([0-9][0-9A-Za-z-]*)\\s*\\)");
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{IsHangul}\\p{IsAlphabetic}\\p{IsDigit}_-]+");

    private final Komoran komoran;

    public ReferenceSignChecker() {
        Komoran analyzer;
        try {
            analyzer = new Komoran(DEFAULT_MODEL.FULL);
        } catch (Exception ex) {
            analyzer = null;
        }
        this.komoran = analyzer;
    }

    public List<CheckResult> check(ParagraphBlock block) {
        return check(List.of(block));
    }

    public List<ReferenceSignEntry> entries(ParagraphBlock block) {
        return entries(List.of(block));
    }

    public List<CheckResult> check(List<ParagraphBlock> blocks) {
        return scan(blocks).issues();
    }

    public List<ReferenceSignEntry> entries(List<ParagraphBlock> blocks) {
        return scan(blocks).entries();
    }

    private ScanResult scan(List<ParagraphBlock> blocks) {
        List<CheckResult> results = new ArrayList<>();
        List<ReferenceSignEntry> entries = new ArrayList<>();
        Map<String, ReferenceSign> firstSigns = new LinkedHashMap<>();

        for (ParagraphBlock block : blocks) {
            String text = block.text();
            Matcher matcher = REFERENCE_SIGN_PATTERN.matcher(text);
            while (matcher.find()) {
                List<ReferenceName> referenceNames = extractReferenceNames(text, matcher.start());
                if (referenceNames.isEmpty()) {
                    continue;
                }

                String sign = matcher.group(1);
                ReferenceName matchedName = findMatchedName(referenceNames, firstSigns);
                ReferenceSign first = matchedName == null ? null : firstSigns.get(matchedName.name());
                ReferenceName displayName = matchedName == null ? referenceNames.get(0) : matchedName;
                String expectedSign = first == null ? sign : first.sign();

                entries.add(new ReferenceSignEntry(
                    block.paragraphNo(),
                    block.location(),
                    displayName.name(),
                    sign,
                    expectedSign,
                    TextUtil.context(text, displayName.start(), matcher.end())
                ));

                if (first == null) {
                    ReferenceName primaryName = referenceNames.get(0);
                    for (ReferenceName referenceName : referenceNames) {
                        firstSigns.putIfAbsent(
                            referenceName.name(),
                            new ReferenceSign(sign, block.paragraphNo(), primaryName.wordCount())
                        );
                    }
                    continue;
                }

                if (first.sign().equals(sign)) {
                    continue;
                }

                String original = text.substring(matchedName.start(), matcher.end());
                String suggestion = matchedName.name() + "(" + first.sign() + ")";
                results.add(new CheckResult(
                    block.paragraphNo(),
                    block.location(),
                    IssueType.REFERENCE_SIGN,
                    original,
                    suggestion,
                    "Same term uses a different reference sign in this file. First occurrence is "
                        + matchedName.name() + "(" + first.sign() + ") in paragraph " + first.paragraphNo() + ".",
                    TextUtil.context(text, matchedName.start(), matcher.end())
                ));
            }
        }

        return new ScanResult(results, entries);
    }

    private ReferenceName findMatchedName(List<ReferenceName> referenceNames, Map<String, ReferenceSign> firstSigns) {
        for (ReferenceName referenceName : referenceNames) {
            ReferenceSign first = firstSigns.get(referenceName.name());
            if (first != null && referenceName.wordCount() >= first.sourceWordCount()) {
                return referenceName;
            }
        }
        return null;
    }

    private List<ReferenceName> extractReferenceNames(String text, int openParenIndex) {
        int boundary = findLeftBoundary(text, openParenIndex);
        String prefix = text.substring(boundary, openParenIndex).stripTrailing();
        if (prefix.isEmpty()) {
            return List.of();
        }

        List<WordSpan> words = extractWords(prefix, boundary);
        if (words.isEmpty()) {
            return List.of();
        }

        int from = Math.max(0, words.size() - MAX_NAME_WORDS);
        List<WordSpan> candidateWords = new ArrayList<>(words.subList(from, words.size()));
        removeLeadingGrammarWords(candidateWords);
        if (candidateWords.isEmpty()) {
            return List.of();
        }

        List<ReferenceName> referenceNames = new ArrayList<>();
        for (int i = 0; i < candidateWords.size(); i++) {
            List<WordSpan> suffixWords = candidateWords.subList(i, candidateWords.size());
            String name = joinWords(suffixWords);
            if (name.isBlank()) {
                continue;
            }
            referenceNames.add(new ReferenceName(name, suffixWords.get(0).start(), suffixWords.size()));
        }
        return referenceNames;
    }

    private List<WordSpan> extractWords(String prefix, int offset) {
        List<WordSpan> words = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(prefix);
        while (matcher.find()) {
            words.add(new WordSpan(offset + matcher.start(), offset + matcher.end(), matcher.group()));
        }
        return words;
    }

    private void removeLeadingGrammarWords(List<WordSpan> words) {
        while (words.size() > 1 && isPatentLeadingModifier(words.get(0).word())) {
            words.remove(0);
        }
        while (words.size() > 1 && isClauseTail(words.get(0).word())) {
            words.remove(0);
        }
        while (words.size() > 1 && isRemovableCaseParticlePhrase(words.get(0).word())) {
            words.remove(0);
            while (words.size() > 1 && isClauseTail(words.get(0).word())) {
                words.remove(0);
            }
        }
    }

    private boolean isPatentLeadingModifier(String word) {
        return isGrammaticalPrefix(word) || isRemovableCaseParticlePhrase(word);
    }

    private boolean isGrammaticalPrefix(String word) {
        if (komoran == null || !containsHangul(word)) {
            return false;
        }

        try {
            List<Token> tokens = komoran.analyze(word).getTokenList();
            if (tokens.isEmpty()) {
                return false;
            }

            boolean hasSubstantive = false;
            boolean hasNonNamePrefix = false;
            for (Token token : tokens) {
                String pos = token.getPos();
                if (isNonNamePrefixPos(pos)) {
                    hasNonNamePrefix = true;
                }
                if (isSubstantivePos(pos)) {
                    hasSubstantive = true;
                }
            }
            return hasNonNamePrefix && !hasSubstantive;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isNonNamePrefixPos(String pos) {
        return pos.startsWith("J")
            || pos.startsWith("E")
            || pos.equals("MM")
            || pos.equals("MAG")
            || pos.equals("MAJ")
            || pos.equals("IC")
            || pos.equals("XPN");
    }

    private boolean isSubstantivePos(String pos) {
        return pos.startsWith("NN")
            || pos.equals("SL")
            || pos.equals("SN")
            || pos.equals("SH")
            || pos.equals("XR");
    }

    private boolean containsHangul(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (TextUtil.isHangulSyllable(word.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private int findLeftBoundary(String text, int openParenIndex) {
        for (int i = openParenIndex - 1; i >= 0; i--) {
            if (isReferenceBoundary(text.charAt(i))) {
                return i + 1;
            }
        }
        return 0;
    }

    private boolean isReferenceBoundary(char ch) {
        return ch == ','
            || ch == '.'
            || ch == ';'
            || ch == ':'
            || ch == '('
            || ch == ')'
            || ch == '['
            || ch == ']'
            || ch == '{'
            || ch == '}'
            || ch == '\n'
            || ch == '\r'
            || ch == '\t'
            || ch == '，'
            || ch == '。'
            || ch == '、'
            || ch == '；'
            || ch == '：';
    }

    private boolean isClauseTail(String word) {
        return word.endsWith("고")
            || word.endsWith("며")
            || word.endsWith("면")
            || word.endsWith("서")
            || word.endsWith("다");
    }

    private boolean hasCaseParticle(String word) {
        return word.length() > 1
            && (word.endsWith("을")
                || word.endsWith("를")
                || word.endsWith("은")
                || word.endsWith("는")
                || word.endsWith("이")
                || word.endsWith("가")
                || word.endsWith("의")
                || word.endsWith("에")
                || word.endsWith("에서"));
    }

    private boolean isRemovableCaseParticlePhrase(String word) {
        return hasCaseParticle(word) && !word.matches("제\\d+의");
    }

    private String joinWords(List<WordSpan> words) {
        List<String> values = new ArrayList<>();
        for (WordSpan word : words) {
            values.add(word.word());
        }
        return String.join(" ", values);
    }

    private record ReferenceName(String name, int start, int wordCount) {
    }

    private record ReferenceSign(String sign, int paragraphNo, int sourceWordCount) {
    }

    private record WordSpan(int start, int end, String word) {
    }

    private record ScanResult(List<CheckResult> issues, List<ReferenceSignEntry> entries) {
    }
}
