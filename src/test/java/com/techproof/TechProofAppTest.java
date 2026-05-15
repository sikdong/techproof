package com.techproof;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TechProofAppTest {
    @Test
    void fallsBackToCurrentReleaseVersionWhenManifestVersionIsUnavailable() {
        assertEquals("0.2.5", TechProofApp.resolveCurrentVersion(null));
        assertEquals("0.2.5", TechProofApp.resolveCurrentVersion(" "));
    }

    @Test
    void usesManifestVersionWhenAvailable() {
        assertEquals("1.2.3", TechProofApp.resolveCurrentVersion("1.2.3"));
    }
}
