package com.techproof.update;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubReleaseCheckerTest {
    private static final String CURRENT_VERSION = "1.2.3";
    private static final String NEWER_VERSION = "1.2.4";

    private final GitHubReleaseChecker checker = new GitHubReleaseChecker(HttpClient.newHttpClient(), new ObjectMapper());

    @Test
    void parsesNewerReleaseWithInstallerAsset() {
        String body = """
            {
              "tag_name": "v1.2.4",
              "name": "TechProof v1.2.4",
              "body": "- 새 버전 알림 추가",
              "html_url": "https://github.com/sikdong/techproof/releases/tag/v1.2.4",
              "assets": [
                {
                  "name": "TechProof-1.2.4.exe",
                  "browser_download_url": "https://github.com/sikdong/techproof/releases/download/v1.2.4/TechProof-1.2.4.exe"
                }
              ]
            }
            """;

        Optional<ReleaseInfo> result = checker.parseResponse(response(200, body), CURRENT_VERSION);

        assertTrue(result.isPresent());
        assertEquals(NEWER_VERSION, result.get().version());
        assertEquals("https://github.com/sikdong/techproof/releases/download/v1.2.4/TechProof-1.2.4.exe",
            result.get().downloadUrl());
    }

    @Test
    void ignoresCurrentOrOlderRelease() {
        String body = """
            {
              "tag_name": "v1.2.3",
              "name": "TechProof v1.2.3",
              "body": "",
              "html_url": "https://github.com/sikdong/techproof/releases/tag/v1.2.3",
              "assets": []
            }
            """;

        Optional<ReleaseInfo> result = checker.parseResponse(response(200, body), CURRENT_VERSION);

        assertTrue(result.isEmpty());
    }

    private HttpResponse<String> response(int statusCode, String body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return HttpRequest.newBuilder(URI.create("https://api.github.com")).build();
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(java.util.Map.of(), (name, value) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return URI.create("https://api.github.com/repos/sikdong/techproof/releases/latest");
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_2;
            }
        };
    }
}
