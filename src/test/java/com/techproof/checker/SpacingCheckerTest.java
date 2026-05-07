package com.techproof.checker;

import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpacingCheckerTest {
    private final SpacingChecker checker = new SpacingChecker();

    @Test
    void detectsConfiguredSpacingRule() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문 S1", "이 작업은 할수 있다."));

        assertTrue(results.stream().anyMatch(r -> "할수".equals(r.getOriginal()) && "할 수".equals(r.getSuggestion())));
    }
}
