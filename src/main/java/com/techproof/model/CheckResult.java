package com.techproof.model;

public class CheckResult {
    private final int paragraphNo;
    private final String location;
    private final IssueType type;
    private final String original;
    private final String suggestion;
    private final String reason;
    private final String context;
    private final SourceInfo source;

    public CheckResult(int paragraphNo, String location, IssueType type, String original, String suggestion, String reason, String context) {
        this(paragraphNo, location, type, original, suggestion, reason, context, SourceInfo.NONE);
    }

    public CheckResult(
        int paragraphNo,
        String location,
        IssueType type,
        String original,
        String suggestion,
        String reason,
        String context,
        SourceInfo source
    ) {
        this.paragraphNo = paragraphNo;
        this.location = location;
        this.type = type;
        this.original = original;
        this.suggestion = suggestion;
        this.reason = reason;
        this.context = context;
        this.source = source == null ? SourceInfo.NONE : source;
    }

    public int getParagraphNo() { return paragraphNo; }
    public String getLocation() { return location; }
    public IssueType getType() { return type; }
    public String getTypeLabel() { return type.label(); }
    public String getOriginal() { return original; }
    public String getSuggestion() { return suggestion; }
    public String getReason() { return reason; }
    public String getContext() { return context; }
    public SourceInfo getSource() { return source; }
    public String getSourceLabel() { return source.label(); }
    public String getSourceUrl() { return source.url(); }
}
