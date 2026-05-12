package com.techproof.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionComparatorTest {
    @Test
    void comparesSemanticVersionsNumerically() {
        assertTrue(VersionComparator.compare("0.2.10", "0.2.3") > 0);
        assertTrue(VersionComparator.compare("0.3.0", "0.2.99") > 0);
        assertTrue(VersionComparator.compare("1.0.0", "0.9.9") > 0);
    }

    @Test
    void treatsMissingPatchAsZero() {
        assertEquals(0, VersionComparator.compare("v0.2", "0.2.0"));
    }

    @Test
    void ignoresCommonVersionPrefixesAndSuffixes() {
        assertEquals(0, VersionComparator.compare("v0.2.3", "0.2.3"));
        assertEquals(0, VersionComparator.compare("0.2.3+build.1", "0.2.3"));
        assertEquals(0, VersionComparator.compare("0.2.3-rc.1", "0.2.3"));
    }
}
