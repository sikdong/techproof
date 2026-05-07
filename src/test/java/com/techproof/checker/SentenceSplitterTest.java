package com.techproof.checker;

import com.techproof.model.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SentenceSplitterTest {
    private final SentenceSplitter splitter = new SentenceSplitter();

    @Test
    void splitsParagraphIntoSentenceBlocksWithSentenceLocation() {
        ParagraphBlock paragraph = new ParagraphBlock(3, "본문", "장치은 동작한다. 센서가 고장났다.");

        List<ParagraphBlock> sentences = splitter.splitAsParagraphBlocks(paragraph);

        assertEquals(2, sentences.size());
        assertEquals(3, sentences.get(0).paragraphNo());
        assertEquals("본문 S1", sentences.get(0).location());
        assertEquals("장치은 동작한다.", sentences.get(0).text());
        assertEquals("본문 S2", sentences.get(1).location());
        assertEquals("센서가 고장났다.", sentences.get(1).text());
    }

    @Test
    void doesNotSplitDecimalNumber() {
        ParagraphBlock paragraph = new ParagraphBlock(1, "본문", "버전 1.2.3을 확인한다. 결과를 저장한다.");

        List<ParagraphBlock> sentences = splitter.splitAsParagraphBlocks(paragraph);

        assertEquals(2, sentences.size());
        assertEquals("버전 1.2.3을 확인한다.", sentences.get(0).text());
    }
}
