package com.techproof.model;

public class ReferenceSignEntry {
    private final int paragraphNo;
    private final String location;
    private final String name;
    private final String sign;
    private final String expectedSign;
    private final String context;

    public ReferenceSignEntry(
        int paragraphNo,
        String location,
        String name,
        String sign,
        String expectedSign,
        String context
    ) {
        this.paragraphNo = paragraphNo;
        this.location = location;
        this.name = name;
        this.sign = sign;
        this.expectedSign = expectedSign;
        this.context = context;
    }

    public int getParagraphNo() {
        return paragraphNo;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getSign() {
        return sign;
    }

    public String getExpectedSign() {
        return expectedSign;
    }

    public String getContext() {
        return context;
    }

    public boolean isMismatch() {
        return expectedSign != null && !expectedSign.isBlank() && !expectedSign.equals(sign);
    }

    public String getStatus() {
        return isMismatch() ? "불일치" : "일치";
    }

    public String getDisplaySign() {
        return sign == null || sign.isBlank() ? "" : sign;
    }

    public String getDisplayExpectedSign() {
        return isMismatch() ? expectedSign : "";
    }
}
