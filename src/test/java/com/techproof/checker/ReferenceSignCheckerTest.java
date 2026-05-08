package com.techproof.checker;

import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceSignCheckerTest {
    private final ReferenceSignChecker checker = new ReferenceSignChecker();

    @Test
    void detectsDifferentReferenceSignForSameTermInParagraph() {
        List<CheckResult> results = checker.check(new ParagraphBlock(
            1,
            "body",
            "프로세서(120)는 신호를 처리한다. 이후 프로세서(220)는 결과를 출력한다."
        ));

        assertEquals(1, results.size());
        assertEquals("프로세서(220)", results.get(0).getOriginal());
        assertEquals("프로세서(120)", results.get(0).getSuggestion());
    }

    @Test
    void detectsDifferentReferenceSignForSameTermAcrossParagraphs() {
        List<CheckResult> results = checker.check(List.of(
            new ParagraphBlock(1, "body", "프로세서(120)는 신호를 처리한다."),
            new ParagraphBlock(2, "body", "이후 프로세서(220)는 결과를 출력한다.")
        ));

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getParagraphNo());
        assertEquals("프로세서(220)", results.get(0).getOriginal());
        assertEquals("프로세서(120)", results.get(0).getSuggestion());
    }

    @Test
    void detectsDifferentReferenceSignForThreeWordTerm() {
        List<CheckResult> results = checker.check(List.of(
            new ParagraphBlock(1, "body", "제1 신호 처리 프로세서(120)는 입력 신호를 처리한다."),
            new ParagraphBlock(2, "body", "신호 처리 프로세서(220)는 결과를 출력한다.")
        ));

        assertEquals(1, results.size());
        assertEquals("신호 처리 프로세서(220)", results.get(0).getOriginal());
        assertEquals("신호 처리 프로세서(120)", results.get(0).getSuggestion());
    }

    @Test
    void removesLeadingGrammarWordsByPartOfSpeech() {
        List<CheckResult> results = checker.check(List.of(
            new ParagraphBlock(1, "body", "프로세서(120)는 신호를 처리한다."),
            new ParagraphBlock(2, "body", "상기 프로세서(220)는 결과를 출력한다."),
            new ParagraphBlock(3, "body", "제1 프로세서(320)는 결과를 다시 처리한다."),
            new ParagraphBlock(4, "body", "복수의 프로세서(420)는 병렬로 동작한다.")
        ));

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(r -> r.getSuggestion().equals("프로세서(120)")));
    }

    @Test
    void ignoresSameReferenceSignForSameTerm() {
        List<CheckResult> results = checker.check(new ParagraphBlock(
            1,
            "body",
            "프로세서(120)는 신호를 처리하고 프로세서(120, 도 2)는 결과를 출력한다."
        ));

        assertTrue(results.isEmpty());
    }

    @Test
    void ignoresDifferentTermsWithDifferentReferenceSigns() {
        List<CheckResult> results = checker.check(new ParagraphBlock(
            1,
            "body",
            "프로세서(120)는 신호를 처리하고 메모리(220)는 데이터를 저장한다."
        ));

        assertTrue(results.isEmpty());
    }
}
