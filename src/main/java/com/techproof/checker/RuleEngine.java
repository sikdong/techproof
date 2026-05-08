package com.techproof.checker;

import com.techproof.dictionary.TypoDictionary;
import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RuleEngine {
    private final SentenceSplitter sentenceSplitter = new SentenceSplitter();
    private final ParticleChecker particleChecker = new ParticleChecker();
    private final ContextualParticleChecker contextualParticleChecker = new ContextualParticleChecker();
    private final TypoChecker typoChecker = new TypoChecker(TypoDictionary.loadDefault());
    private final SpacingChecker spacingChecker = new SpacingChecker();
    private final GrammarPatternChecker grammarPatternChecker = new GrammarPatternChecker();
    private final MorphologyChecker morphologyChecker = new MorphologyChecker();
    private final ReferenceSignChecker referenceSignChecker = new ReferenceSignChecker();

    public List<CheckResult> checkAll(List<ParagraphBlock> blocks) {
        return checkAll(blocks, (completed, total) -> {
        });
    }

    public List<CheckResult> checkAll(List<ParagraphBlock> blocks, ProgressListener progressListener) {
        List<CheckResult> results = new ArrayList<>();
        int totalSteps = blocks.size() + 1;
        int completedSteps = 0;

        results.addAll(referenceSignChecker.check(blocks));
        progressListener.onProgress(++completedSteps, totalSteps);

        for (ParagraphBlock block : blocks) {
            for (ParagraphBlock sentenceBlock : sentenceSplitter.splitAsParagraphBlocks(block)) {
                results.addAll(particleChecker.check(sentenceBlock));
                results.addAll(typoChecker.check(sentenceBlock));
                results.addAll(spacingChecker.check(sentenceBlock));
                results.addAll(grammarPatternChecker.check(sentenceBlock));
            }
            progressListener.onProgress(++completedSteps, totalSteps);
        }
        results.sort(Comparator.comparingInt(CheckResult::getParagraphNo).thenComparing(CheckResult::getOriginal));
        return results;
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int completedSteps, int totalSteps);
    }
}
