package com.techproof.docx;

import com.techproof.model.ParagraphBlock;
import org.apache.poi.xwpf.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DocxReader {
    public List<ParagraphBlock> read(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new IOException("파일을 찾을 수 없습니다.");
        }
        if (!path.toString().toLowerCase().endsWith(".docx")) {
            throw new IOException(".docx 파일만 지원합니다.");
        }

        List<ParagraphBlock> blocks = new ArrayList<>();
        try (InputStream in = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(in)) {
            int[] paragraphNo = {1};
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    addParagraph(blocks, paragraphNo, "본문", paragraph.getText());
                } else if (element instanceof XWPFTable table) {
                    readTable(blocks, paragraphNo, table, "표");
                }
            }
        }
        return blocks;
    }

    private void readTable(List<ParagraphBlock> blocks, int[] paragraphNo, XWPFTable table, String prefix) {
        int r = 1;
        for (XWPFTableRow row : table.getRows()) {
            int c = 1;
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    addParagraph(blocks, paragraphNo, prefix + " R" + r + "C" + c, paragraph.getText());
                }
                for (XWPFTable nested : cell.getTables()) {
                    readTable(blocks, paragraphNo, nested, prefix + " R" + r + "C" + c + " 내부표");
                }
                c++;
            }
            r++;
        }
    }

    private void addParagraph(List<ParagraphBlock> blocks, int[] paragraphNo, String location, String text) {
        if (text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;
        blocks.add(new ParagraphBlock(paragraphNo[0]++, location, trimmed));
    }
}
