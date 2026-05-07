package com.techproof.checker;

import com.techproof.model.ParagraphBlock;

import java.util.ArrayList;
import java.util.List;

public class SentenceSplitter {
    public List<ParagraphBlock> splitAsParagraphBlocks(ParagraphBlock paragraph) {
        String text = paragraph.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<ParagraphBlock> sentences = new ArrayList<>();
        int sentenceNo = 1;
        int start = 0;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!isSentenceBoundary(text, i, ch)) {
                continue;
            }

            int end = includeClosingPunctuation(text, i + 1);
            addSentence(paragraph, sentences, sentenceNo++, start, end);
            start = skipWhitespace(text, end);
            i = start - 1;
        }

        addSentence(paragraph, sentences, sentenceNo, start, text.length());
        return sentences.isEmpty() ? List.of(paragraph) : sentences;
    }

    private boolean isSentenceBoundary(String text, int index, char ch) {
        if (ch == '\n' || ch == '\r') {
            return true;
        }
        if (ch == '?' || ch == '!' || ch == '\u3002') {
            return true;
        }
        if (ch == '.') {
            return isPeriodBoundary(text, index);
        }
        return isKoreanEndingBoundary(text, index);
    }

    private boolean isPeriodBoundary(String text, int index) {
        if (isDecimalPoint(text, index) || isLikelyAbbreviation(text, index)) {
            return false;
        }
        return index == text.length() - 1 || Character.isWhitespace(text.charAt(index + 1));
    }

    private boolean isDecimalPoint(String text, int index) {
        return index > 0
            && index < text.length() - 1
            && Character.isDigit(text.charAt(index - 1))
            && Character.isDigit(text.charAt(index + 1));
    }

    private boolean isLikelyAbbreviation(String text, int index) {
        int start = index - 1;
        while (start >= 0 && Character.isLetter(text.charAt(start))) {
            start--;
        }
        String token = text.substring(start + 1, index);
        return token.length() <= 3 && token.chars().allMatch(c -> c >= 'A' && c <= 'Z');
    }

    private boolean isKoreanEndingBoundary(String text, int index) {
        if (index >= text.length() - 1 || !Character.isWhitespace(text.charAt(index + 1))) {
            return false;
        }

        return endsWithAt(text, index, "\uB2E4")
            || endsWithAt(text, index, "\uC694")
            || endsWithAt(text, index, "\uB2C8\uB2E4")
            || endsWithAt(text, index, "\uAE4C")
            || endsWithAt(text, index, "\uB2E4.");
    }

    private boolean endsWithAt(String text, int endInclusive, String suffix) {
        int start = endInclusive - suffix.length() + 1;
        return start >= 0 && text.startsWith(suffix, start);
    }

    private int includeClosingPunctuation(String text, int end) {
        while (end < text.length()) {
            char ch = text.charAt(end);
            if (ch == ')' || ch == ']' || ch == '}' || ch == '"' || ch == '\'') {
                end++;
                continue;
            }
            break;
        }
        return end;
    }

    private int skipWhitespace(String text, int index) {
        int current = index;
        while (current < text.length() && Character.isWhitespace(text.charAt(current))) {
            current++;
        }
        return current;
    }

    private void addSentence(ParagraphBlock paragraph, List<ParagraphBlock> sentences, int sentenceNo, int start, int end) {
        if (start >= end) {
            return;
        }

        String sentence = paragraph.text().substring(start, end).trim();
        if (sentence.isEmpty()) {
            return;
        }

        sentences.add(new ParagraphBlock(
            paragraph.paragraphNo(),
            paragraph.location() + " S" + sentenceNo,
            sentence
        ));
    }
}
