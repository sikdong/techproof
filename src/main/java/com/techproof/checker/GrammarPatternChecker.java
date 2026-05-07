package com.techproof.checker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.techproof.model.CheckResult;
import com.techproof.model.IssueType;
import com.techproof.model.ParagraphBlock;
import com.techproof.model.SourceInfo;

import java.util.ArrayList;
import java.util.List;

public class GrammarPatternChecker {
    private static final List<GrammarRule> DEFAULT_RULES = List.of(
        new GrammarRule("?????", "???", "Prefer concise passive expression."),
        new GrammarRule("?????", "????", "Prefer concise passive expression."),
        new GrammarRule("???", "?", "Avoid redundant 'doe-eojin' pattern."),
        new GrammarRule("????", "???", "Avoid redundant passive progressive pattern.")
    );

    private final List<GrammarRule> rules;

    public GrammarPatternChecker() {
        this.rules = RuleResourceLoader.loadList(
            "/rules/grammar-pattern-rules.json",
            new TypeReference<List<GrammarRule>>() {},
            DEFAULT_RULES
        );
    }

    public List<CheckResult> check(ParagraphBlock block) {
        List<CheckResult> results = new ArrayList<>();
        String text = block.text();

        for (GrammarRule rule : rules) {
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
                results.add(new CheckResult(
                    block.paragraphNo(),
                    block.location(),
                    IssueType.GRAMMAR,
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

    public static class GrammarRule {
        public String wrong;
        public String correct;
        public String reason;
        public SourceInfo source = SourceInfo.NONE;

        public GrammarRule() {
        }

        public GrammarRule(String wrong, String correct, String reason) {
            this.wrong = wrong;
            this.correct = correct;
            this.reason = reason;
            this.source = SourceInfo.NONE;
        }
    }
}
