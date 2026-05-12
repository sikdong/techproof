package com.techproof.update;

public record ReleaseInfo(
    String version,
    String title,
    String notes,
    String releaseUrl,
    String downloadUrl
) {
}
