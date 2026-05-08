package com.techproof.model;

public enum IssueType {
    PARTICLE("Particle"),
    PARTICLE_CONTEXT("Particle Context"),
    TYPO("Typo"),
    SPACING("Spacing"),
    GRAMMAR("Grammar"),
    MORPHOLOGY("Morphology"),
    REFERENCE_SIGN("Reference Sign");

    private final String label;

    IssueType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
