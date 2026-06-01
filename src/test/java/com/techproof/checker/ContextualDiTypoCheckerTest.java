package com.techproof.checker;

import com.techproof.dictionary.TypoDictionary;
import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextualDiTypoCheckerTest {
    private final TypoChecker checker = new TypoChecker(TypoDictionary.loadDefault());

    @Test
    void suggestsSubjectParticleForDiTypoBeforePassivePredicateWithBatchim() {
        List<CheckResult> results = checker.check(new ParagraphBlock(
            1,
            "본문",
            "제3 연산단계에서는 갱신된 모드 선택신호에 기초하여 제2 결과에 대해 단일 큐비트 게이트 연산디 수행되고 제3 결과가 생성된다."
        ));

        assertTrue(results.stream().anyMatch(r ->
            "연산디".equals(r.getOriginal())
                && "연산이".equals(r.getSuggestion())
                && r.getReason().contains("Contextual typo candidate")
        ));
    }

    @Test
    void suggestsSubjectParticleForDiTypoBeforePassivePredicateWithoutBatchim() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문", "제2 결과디 생성된다."));

        assertTrue(results.stream().anyMatch(r ->
            "결과디".equals(r.getOriginal())
                && "결과가".equals(r.getSuggestion())
        ));
    }

    @Test
    void suggestsObjectParticleForDiTypoBeforeActivePredicateWithBatchim() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문", "처리부는 연산디 수행하고 결과를 출력한다."));

        assertTrue(results.stream().anyMatch(r ->
            "연산디".equals(r.getOriginal())
                && "연산을".equals(r.getSuggestion())
        ));
    }

    @Test
    void suggestsObjectParticleForDiTypoBeforeActivePredicateWithoutBatchim() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문", "장치는 결과디 포함하고 신호를 전송한다."));

        assertTrue(results.stream().anyMatch(r ->
            "결과디".equals(r.getOriginal())
                && "결과를".equals(r.getSuggestion())
        ));
    }

    @Test
    void doesNotSuggestDiTypoWhenFollowingContextIsAmbiguous() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문", "가위디 절단한다."));

        assertTrue(results.stream().noneMatch(r -> "가위디".equals(r.getOriginal())));
    }

    @Test
    void doesNotSuggestDiTypoForKnownExceptionWord() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문", "어디 수행되는지 확인한다."));

        assertTrue(results.stream().noneMatch(r -> "어디".equals(r.getOriginal())));
    }
}
