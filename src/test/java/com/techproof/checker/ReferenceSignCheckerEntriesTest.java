package com.techproof.checker;

import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceSignCheckerEntriesTest {
    private final ReferenceSignChecker checker = new ReferenceSignChecker();

    @Test
    void keepsTwoWordModuleNamesAfterOtherModuleReferences() {
        var entries = checker.entries(new ParagraphBlock(
            1,
            "body",
            "뉴런 모듈(110)은 동작한다. 센서 모듈(400)과 통신 모듈(410)"
        ));

        assertEquals(3, entries.size());
        assertEquals("뉴런 모듈", entries.get(0).getName());
        assertEquals("센서 모듈", entries.get(1).getName());
        assertEquals("400", entries.get(1).getSign());
        assertEquals("통신 모듈", entries.get(2).getName());
        assertEquals("410", entries.get(2).getSign());
        assertTrue(entries.stream().allMatch(entry -> !entry.isMismatch()));
    }

    @Test
    void listsAlphabeticReferenceSignsAndMarksMismatches() {
        var entries = checker.entries(new ParagraphBlock(
            1,
            "body",
            "센서 모듈(10A)은 신호를 감지한다. 센서 모듈(10B)은 감지 결과를 출력한다."
        ));

        assertEquals(2, entries.size());
        assertEquals("센서 모듈", entries.get(0).getName());
        assertEquals("10A", entries.get(0).getSign());
        assertTrue(!entries.get(0).isMismatch());
        assertEquals("센서 모듈", entries.get(1).getName());
        assertEquals("10B", entries.get(1).getSign());
        assertEquals("10A", entries.get(1).getExpectedSign());
        assertTrue(entries.get(1).isMismatch());
    }

    @Test
    void listsLetterFirstReferenceSignsAndMarksMismatches() {
        var entries = checker.entries(new ParagraphBlock(
            1,
            "body",
            "via plug(VIA1) contacts the lower wiring. via plug(VIA2) contacts the upper wiring."
        ));

        assertEquals(2, entries.size());
        assertEquals("via plug", entries.get(0).getName());
        assertEquals("VIA1", entries.get(0).getSign());
        assertTrue(!entries.get(0).isMismatch());
        assertEquals("via plug", entries.get(1).getName());
        assertEquals("VIA2", entries.get(1).getSign());
        assertEquals("VIA1", entries.get(1).getExpectedSign());
        assertTrue(entries.get(1).isMismatch());
    }

    @Test
    void listsUppercaseLetterOnlyReferenceSignsAndMarksMismatches() {
        var entries = checker.entries(new ParagraphBlock(
            1,
            "body",
            "gate line(GL) extends in a first direction. gate line(DL) crosses the pixel area."
        ));

        assertEquals(2, entries.size());
        assertEquals("gate line", entries.get(0).getName());
        assertEquals("GL", entries.get(0).getSign());
        assertTrue(!entries.get(0).isMismatch());
        assertEquals("gate line", entries.get(1).getName());
        assertEquals("DL", entries.get(1).getSign());
        assertEquals("GL", entries.get(1).getExpectedSign());
        assertTrue(entries.get(1).isMismatch());
    }

    @Test
    void listsLowercaseLetterOnlyReferenceSignsAndMarksMismatches() {
        var entries = checker.entries(new ParagraphBlock(
            1,
            "body",
            "gate line(gl) extends in a first direction. gate line(dl) crosses the pixel area."
        ));

        assertEquals(2, entries.size());
        assertEquals("gate line", entries.get(0).getName());
        assertEquals("gl", entries.get(0).getSign());
        assertTrue(!entries.get(0).isMismatch());
        assertEquals("gate line", entries.get(1).getName());
        assertEquals("dl", entries.get(1).getSign());
        assertEquals("gl", entries.get(1).getExpectedSign());
        assertTrue(entries.get(1).isMismatch());
    }

    @Test
    void listsEveryReferenceSignAndMarksMismatches() {
        var entries = checker.entries(new ParagraphBlock(
            1,
            "body",
            "이미지 인코더(112)는 영상을 처리한다. 이미지 인코더(132)는 결과를 출력한다."
        ));

        assertEquals(2, entries.size());
        assertEquals("이미지 인코더", entries.get(0).getName());
        assertEquals("112", entries.get(0).getSign());
        assertTrue(!entries.get(0).isMismatch());
        assertEquals("이미지 인코더", entries.get(1).getName());
        assertEquals("132", entries.get(1).getSign());
        assertEquals("112", entries.get(1).getExpectedSign());
        assertTrue(entries.get(1).isMismatch());
    }

    @Test
    void removesDuplicateReferenceSignEntries() {
        var entries = checker.entries(new ParagraphBlock(
            1,
            "body",
            "유간 검출 장치(100)는 신호를 검출한다. 유간 검출 장치(100)는 다시 신호를 검출한다. "
                + "유간 검출 장치(110)는 결과를 출력한다. 유간 검출 장치(110)는 다시 결과를 출력한다."
        ));

        assertEquals(2, entries.size());
        assertEquals("100", entries.get(0).getSign());
        assertTrue(!entries.get(0).isMismatch());
        assertEquals("110", entries.get(1).getSign());
        assertTrue(entries.get(1).isMismatch());
    }
}
