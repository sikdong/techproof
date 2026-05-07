package com.techproof.checker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.techproof.model.CheckResult;
import com.techproof.model.IssueType;
import com.techproof.model.ParagraphBlock;
import com.techproof.model.SourceInfo;

import java.util.ArrayList;
import java.util.List;

public class SpacingChecker {
    private static final List<SpacingRule> DEFAULT_RULES = List.of(
        new SpacingRule("??", "? ?", "Bound noun 'su' should be separated."),
        new SpacingRule("??", "? ?", "Bound noun 'su' should be separated."),
        new SpacingRule("??", "? ?", "Bound noun 'geot' should be separated."),
        new SpacingRule("??", "? ?", "Bound noun 'geot' should be separated.")
    );

    private final List<SpacingRule> rules;

    public SpacingChecker() {
        this.rules = RuleResourceLoader.loadList(
            "/rules/spacing-rules.json",
            new TypeReference<List<SpacingRule>>() {},
            DEFAULT_RULES
        );
    }

    public List<CheckResult> check(ParagraphBlock block) {
        List<CheckResult> results = new ArrayList<>();
        String text = block.text();

        for (SpacingRule rule : rules) {
            if (rule.wrong == null || rule.wrong.isBlank()) {
                continue;
            }

            int from = 0;
            while (from < text.length()) {
                int idx = text.indexOf(rule.wrong, from);
                if (idx < 0) {
                    break;
                }

                int end = idx + rule.wrong.length();
                if (!isWordBoundary(text, idx, end)) {
                    from = end;
                    continue;
                }

                results.add(new CheckResult(
                    block.paragraphNo(),
                    block.location(),
                    IssueType.SPACING,
                    rule.wrong,
                    rule.correct,
                    rule.reason,
                    TextUtil.context(text, idx, end),
                    rule.source == null ? SourceInfo.NONE : rule.source
                ));
                from = end;
            }
        }

        return results;
    }

    private boolean isWordBoundary(String text, int start, int end) {
        boolean leftWord = start > 0 && TextUtil.isKoreanWordChar(text.charAt(start - 1));
        boolean rightWord = end < text.length() && TextUtil.isKoreanWordChar(text.charAt(end));
        return !(leftWord || rightWord);
    }

    public static class SpacingRule {
        public String wrong;
        public String correct;
        public String reason;
        public SourceInfo source = SourceInfo.NONE;

        public SpacingRule() {
        }

        public SpacingRule(String wrong, String correct, String reason) {
            this.wrong = wrong;
            this.correct = correct;
            this.reason = reason;
            this.source = SourceInfo.NONE;
        }
    }
}
