package com.techproof.checker;

import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceSignCheckerCheckTest {
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
    void detectsDifferentAlphabeticReferenceSignForSameTerm() {
        List<CheckResult> results = checker.check(List.of(
            new ParagraphBlock(1, "body", "센서 모듈(10A)은 신호를 감지한다."),
            new ParagraphBlock(2, "body", "센서 모듈(10B)은 감지 결과를 출력한다.")
        ));

        assertEquals(1, results.size());
        assertEquals("센서 모듈(10B)", results.get(0).getOriginal());
        assertEquals("센서 모듈(10A)", results.get(0).getSuggestion());
    }

    @Test
    void detectsDifferentLetterFirstReferenceSignForSameTerm() {
        List<CheckResult> results = checker.check(List.of(
            new ParagraphBlock(1, "body", "ILD layer(ILD1) is formed on the substrate."),
            new ParagraphBlock(2, "body", "ILD layer(ILD2) covers the wiring.")
        ));

        assertEquals(1, results.size());
        assertEquals("ILD layer(ILD2)", results.get(0).getOriginal());
        assertEquals("ILD layer(ILD1)", results.get(0).getSuggestion());
    }

    @Test
    void detectsDifferentUppercaseLetterOnlyReferenceSignForSameTerm() {
        List<CheckResult> results = checker.check(List.of(
            new ParagraphBlock(1, "body", "gate line(GL) extends in a first direction."),
            new ParagraphBlock(2, "body", "gate line(DL) crosses the pixel area.")
        ));

        assertEquals(1, results.size());
        assertEquals("gate line(DL)", results.get(0).getOriginal());
        assertEquals("gate line(GL)", results.get(0).getSuggestion());
    }

    @Test
    void detectsDifferentLowercaseLetterOnlyReferenceSignForSameTerm() {
        List<CheckResult> results = checker.check(List.of(
            new ParagraphBlock(1, "body", "gate line(gl) extends in a first direction."),
            new ParagraphBlock(2, "body", "gate line(dl) crosses the pixel area.")
        ));

        assertEquals(1, results.size());
        assertEquals("gate line(dl)", results.get(0).getOriginal());
        assertEquals("gate line(gl)", results.get(0).getSuggestion());
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
    void removesLeadingGrammarWordsByPartOfSpeech2() {
        List<CheckResult> results = checker.check(List.of(
                new ParagraphBlock(1, "body", "뉴로모픽 프로세서(120)는 신호를 처리한다."),
                new ParagraphBlock(2, "body", "상기 뉴로모픽 프로세서(220)는 결과를 출력한다."),
                new ParagraphBlock(3, "body", "제1 뉴로모픽 프로세서(320)는 결과를 다시 처리한다."),
                new ParagraphBlock(4, "body", "복수의 뉴로모픽 프로세서(420)는 병렬로 동작한다.")
        ));

        assertEquals(3, results.size());
        assertEquals("뉴로모픽 프로세서(220)", results.get(0).getOriginal());
        assertEquals("뉴로모픽 프로세서(120)", results.get(0).getSuggestion());
        assertTrue(results.stream().allMatch(r -> r.getSuggestion().equals("뉴로모픽 프로세서(120)")));
    }

    @Test
    void keepsOrdinalPossessivePrefixAsPartOfReferenceName() {
        List<CheckResult> results = checker.check(List.of(
            new ParagraphBlock(1, "body", "제1의 뉴로모픽 프로세서(120)는 신호를 처리한다."),
            new ParagraphBlock(2, "body", "제1의 뉴로모픽 프로세서(220)는 결과를 출력한다.")
        ));

        assertEquals(1, results.size());
        assertEquals("제1의 뉴로모픽 프로세서(220)", results.get(0).getOriginal());
        assertEquals("제1의 뉴로모픽 프로세서(120)", results.get(0).getSuggestion());
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

    @Test
    void ignoresParenthesizedEnglishLoanwordLabels() {
        ParagraphBlock block = new ParagraphBlock(
            1,
            "body",
            "기계적 벨로시티(Velocity) 일관성 데이터, 다이나믹 평탄도 데이터, "
                + "합성 포먼트(Synthetic Formant)를 비교한다."
        );

        assertTrue(checker.check(block).isEmpty());
        assertTrue(checker.entries(block).isEmpty());
    }

    @Test
    void ignoresDifferentReferenceSignsForDifferentTermsInSamePhrase() {
        List<CheckResult> results = checker.check(new ParagraphBlock(
            1,
            "body",
            "센서 모듈(400)과 통신 모듈(410)"
        ));

        assertTrue(results.isEmpty());
    }

    @Test
    void countsRepeatedSameReferenceSignMismatchOnce() {
        List<CheckResult> results = checker.check(new ParagraphBlock(
            1,
            "body",
            "gap detector(100) detects a signal. gap detector(110) detects another signal. "
                + "gap detector(110) detects the signal again."
        ));

        assertEquals(1, results.size());
        assertEquals("gap detector(110)", results.get(0).getOriginal());
        assertEquals("gap detector(100)", results.get(0).getSuggestion());
    }
}
