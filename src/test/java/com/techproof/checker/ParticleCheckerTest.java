package com.techproof.checker;

import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleCheckerTest {
    private final ParticleChecker checker = new ParticleChecker();

    @Test
    void detectsBatchimBasedParticleMismatch() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문 S1", "값는 증가했다."));

        assertEquals(1, results.size());
        assertEquals("값는", results.get(0).getOriginal());
        assertEquals("값은", results.get(0).getSuggestion());
    }

    @Test
    void keepsParenthesizedDrawingMarkerInSuggestion() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문 S1", "센서(10)은 동작한다."));

        assertEquals(1, results.size());
        assertEquals("센서(10)은", results.get(0).getOriginal());
        assertEquals("센서(10)는", results.get(0).getSuggestion());
    }

    @Test
    void doesNotTreatSyllableInsideSingleNounAsParticle() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문 S1", "전문가가 개입이 필요하다."));

        assertTrue(results.stream().noneMatch(r -> "전문가".equals(r.getOriginal())));
    }

    @Test
    void containedNumberTest() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문 S1", "도1는 발명이다."));

        assertEquals(1, results.size());
        assertEquals("도1는", results.get(0).getOriginal());
        assertEquals("도1은", results.get(0).getSuggestion());
    }
}
