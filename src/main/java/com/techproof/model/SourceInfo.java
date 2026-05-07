package com.techproof.model;

public class SourceInfo {
    public static final SourceInfo NONE = new SourceInfo("", "", "", "", "");

    private String provider = "";
    private String title = "";
    private String detail = "";
    private String url = "";
    private String retrievedAt = "";

    public SourceInfo() {
    }

    public SourceInfo(String provider, String title, String detail, String url, String retrievedAt) {
        this.provider = provider == null ? "" : provider;
        this.title = title == null ? "" : title;
        this.detail = detail == null ? "" : detail;
        this.url = url == null ? "" : url;
        this.retrievedAt = retrievedAt == null ? "" : retrievedAt;
    }

    public String provider() {
        return provider;
    }

    public String title() {
        return title;
    }

    public String detail() {
        return detail;
    }

    public String url() {
        return url;
    }

    public String retrievedAt() {
        return retrievedAt;
    }

    public void setProvider(String provider) {
        this.provider = provider == null ? "" : provider;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public void setDictionary(String dictionary) {
        setTitle(dictionary);
    }

    public void setDocument(String document) {
        setTitle(document);
    }

    public void setDetail(String detail) {
        this.detail = detail == null ? "" : detail;
    }

    public void setSection(String section) {
        setDetail(section);
    }

    public void setQuery(String query) {
        setDetail(query);
    }

    public void setUrl(String url) {
        this.url = url == null ? "" : url;
    }

    public void setRetrievedAt(String retrievedAt) {
        this.retrievedAt = retrievedAt == null ? "" : retrievedAt;
    }

    public String label() {
        if (isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendPart(sb, provider);
        appendPart(sb, title);
        appendPart(sb, detail);
        return sb.toString();
    }

    public boolean isBlank() {
        return provider.isBlank() && title.isBlank() && detail.isBlank() && url.isBlank() && retrievedAt.isBlank();
    }

    private void appendPart(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(" - ");
        }
        sb.append(value);
    }
}
