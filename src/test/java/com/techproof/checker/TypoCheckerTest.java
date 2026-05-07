package com.techproof.checker;

import com.techproof.dictionary.TypoDictionary;
import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypoCheckerTest {
    private final TypoChecker checker = new TypoChecker(TypoDictionary.loadDefault());

    @Test
    void detectsDictionaryTypoWithoutDuplicateShorterRule() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문", "장치가 됬다."));

        assertEquals(1, results.size());
        assertEquals("됬다", results.get(0).getOriginal());
        assertEquals("됐다", results.get(0).getSuggestion());
    }

    @Test
    void preservesTrailingParticleWhenNormalizedDictionaryEntryMatches() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문", "장치가 됬다는 점을 확인한다."));

        assertEquals(1, results.size());
        assertEquals("됬다는", results.get(0).getOriginal());
        assertEquals("됐다는", results.get(0).getSuggestion());
    }

    @Test
    void detectsCommonDoeDwaeSpellingPatterns() {
        List<CheckResult> results = checker.check(new ParagraphBlock(1, "본문", "처리가 되요. 실행이 돼고 있습니다."));

        assertTrue(results.stream().anyMatch(r -> "되요".equals(r.getOriginal()) && "돼요".equals(r.getSuggestion())));
        assertTrue(results.stream().anyMatch(r -> "돼고".equals(r.getOriginal()) && "되고".equals(r.getSuggestion())));
    }
}
