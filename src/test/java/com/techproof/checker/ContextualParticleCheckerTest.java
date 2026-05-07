package com.techproof.checker;

import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextualParticleCheckerTest {
    private final ContextualParticleChecker checker = new ContextualParticleChecker();

    @Test
    void doesNotReviewSyllableInsideSingleNounAsParticle() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문 S1", "전문가 개입이 필요하다."));

        assertTrue(results.stream().noneMatch(r -> "전문가".equals(r.getOriginal())));
    }
}
