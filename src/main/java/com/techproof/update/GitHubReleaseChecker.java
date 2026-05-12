package com.techproof.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GitHubReleaseChecker {
    private static final URI LATEST_RELEASE_URI =
        URI.create("https://api.github.com/repos/sikdong/techproof/releases/latest");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitHubReleaseChecker() {
        this(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build(), new ObjectMapper());
    }

    GitHubReleaseChecker(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<Optional<ReleaseInfo>> findUpdate(String currentVersion) {
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_URI)
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "TechProof")
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> parseResponse(response, currentVersion));
    }

    Optional<ReleaseInfo> parseResponse(HttpResponse<String> response, String currentVersion) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            String tagName = text(root, "tag_name");
            String latestVersion = normalizeVersion(tagName);
            if (latestVersion.isBlank() || VersionComparator.compare(latestVersion, currentVersion) <= 0) {
                return Optional.empty();
            }

            String releaseUrl = text(root, "html_url");
            return Optional.of(new ReleaseInfo(
                latestVersion,
                text(root, "name").isBlank() ? "TechProof v" + latestVersion : text(root, "name"),
                text(root, "body"),
                releaseUrl,
                downloadUrl(root).orElse(releaseUrl)
            ));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private Optional<String> downloadUrl(JsonNode root) {
        JsonNode assets = root.path("assets");
        if (!assets.isArray()) {
            return Optional.empty();
        }

        for (JsonNode asset : assets) {
            String name = text(asset, "name");
            if (name.endsWith(".exe")) {
                String url = text(asset, "browser_download_url");
                if (!url.isBlank()) {
                    return Optional.of(url);
                }
            }
        }
        return Optional.empty();
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            return normalized.substring(1);
        }
        return normalized;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }
}
