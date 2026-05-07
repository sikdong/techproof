package com.techproof.checker;

import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GrammarPatternCheckerTest {
    private final GrammarPatternChecker checker = new GrammarPatternChecker();

    @Test
    void detectsConfiguredGrammarPattern() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문 S1", "결과가 보여집니다."));

        assertTrue(results.stream().anyMatch(r -> "보여집니다".equals(r.getOriginal()) && "보입니다".equals(r.getSuggestion())));
    }
}
