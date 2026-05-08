package com.techproof.docx;

import com.techproof.model.ParagraphBlock;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DocxReader {
    public List<ParagraphBlock> read(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new IOException("File not found.");
        }

        String fileName = path.toString().toLowerCase();
        if (fileName.endsWith(".docx")) {
            return readDocx(path);
        }
        if (fileName.endsWith(".doc")) {
            return readDoc(path);
        }
        throw new IOException("Only .docx and .doc files are supported.");
    }

    private List<ParagraphBlock> readDocx(Path path) throws IOException {
        List<ParagraphBlock> blocks = new ArrayList<>();
        try (InputStream in = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(in)) {
            int[] paragraphNo = {1};
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    addParagraph(blocks, paragraphNo, "Body", paragraph.getText());
                } else if (element instanceof XWPFTable table) {
                    readTable(blocks, paragraphNo, table, "Table");
                }
            }
        }
        return blocks;
    }

    private List<ParagraphBlock> readDoc(Path path) throws IOException {
        List<ParagraphBlock> blocks = new ArrayList<>();
        try (
            InputStream in = Files.newInputStream(path);
            HWPFDocument document = new HWPFDocument(in);
            WordExtractor extractor = new WordExtractor(document)
        ) {
            int[] paragraphNo = {1};
            for (String paragraph : extractor.getParagraphText()) {
                addParagraph(blocks, paragraphNo, "Body", paragraph);
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
                    readTable(blocks, paragraphNo, nested, prefix + " R" + r + "C" + c + " Nested");
                }
                c++;
            }
            r++;
        }
    }

    private void addParagraph(List<ParagraphBlock> blocks, int[] paragraphNo, String location, String text) {
        if (text == null) {
            return;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        blocks.add(new ParagraphBlock(paragraphNo[0]++, location, trimmed));
    }
}
