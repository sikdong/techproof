package com.techproof.ui;

import com.techproof.checker.RuleEngine;
import com.techproof.docx.DocxReader;
import com.techproof.model.CheckResult;
import com.techproof.model.ParagraphBlock;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class MainView {
    private final Stage stage;
    private final BorderPane root = new BorderPane();
    private final TableView<CheckResult> table = new TableView<>();
    private final TextArea preview = new TextArea();
    private final Label status = new Label("Select a .docx file.");
    private List<ParagraphBlock> loadedBlocks = List.of();
    private List<CheckResult> currentResults = List.of();

    public MainView(Stage stage) {
        this.stage = stage;
        buildUi();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildUi() {
        Button openButton = new Button("Open Word File");
        Button checkButton = new Button("Run Check");
        Button exportButton = new Button("Export CSV");

        openButton.setOnAction(e -> openFile());
        checkButton.setOnAction(e -> runCheck());
        exportButton.setOnAction(e -> exportCsv());

        HBox top = new HBox(10, openButton, checkButton, exportButton, status);
        top.setPadding(new Insets(12));

        buildTable();

        preview.setEditable(false);
        preview.setWrapText(true);
        preview.setPromptText("Paragraph preview will appear after opening a file.");

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(table, preview);
        splitPane.setDividerPositions(0.72);

        Label help = new Label("Particle check is not an auto-fix. Text in parentheses is ignored only for particle rules.");
        help.setPadding(new Insets(8));

        root.setTop(top);
        root.setCenter(splitPane);
        root.setBottom(new VBox(help));
    }

    private void buildTable() {
        TableColumn<CheckResult, String> paragraph = col("Paragraph", r -> String.valueOf(r.getParagraphNo()), 80);
        TableColumn<CheckResult, String> location = col("Location", CheckResult::getLocation, 120);
        TableColumn<CheckResult, String> type = col("Type", CheckResult::getTypeLabel, 100);
        TableColumn<CheckResult, String> original = col("Original", CheckResult::getOriginal, 180);
        TableColumn<CheckResult, String> suggestion = col("Suggestion", CheckResult::getSuggestion, 180);
        TableColumn<CheckResult, String> source = col("Source", CheckResult::getSourceLabel, 260);
        TableColumn<CheckResult, String> sourceUrl = col("Source URL", CheckResult::getSourceUrl, 260);
        TableColumn<CheckResult, String> context = col("Context", CheckResult::getContext, 260);
        table.getColumns().addAll(paragraph, location, type, original, suggestion, source, sourceUrl, context);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null) {
                preview.setText("[Paragraph " + value.getParagraphNo() + "] " + value.getContext());
            }
        });
    }

    private TableColumn<CheckResult, String> col(String title, Function<CheckResult, String> getter, int width) {
        TableColumn<CheckResult, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Word File");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Word Document (*.docx)", "*.docx"));
        var file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            loadedBlocks = new DocxReader().read(file.toPath());
            currentResults = List.of();
            table.setItems(FXCollections.observableArrayList());
            preview.setText(buildPreview(loadedBlocks));
            status.setText("Loaded: " + file.getName() + " / Paragraphs: " + loadedBlocks.size());

            if (loadedBlocks.isEmpty()) {
                showError("No Paragraphs", "The file was opened, but no readable paragraphs were found.");
            }
        } catch (Exception ex) {
            status.setText("Open failed: " + file.getName());
            showException("Failed to open file", ex);
        }
    }

    private String buildPreview(List<ParagraphBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(50, blocks.size());
        for (int i = 0; i < limit; i++) {
            ParagraphBlock block = blocks.get(i);
            sb.append("[Paragraph ")
                .append(block.paragraphNo())
                .append(" / ")
                .append(block.location())
                .append("]\n")
                .append(block.text())
                .append("\n\n");
        }
        if (blocks.size() > limit) {
            sb.append("... omitted ...");
        }
        return sb.toString();
    }

    private void runCheck() {
        if (loadedBlocks.isEmpty()) {
            showError("No Document", "Open a .docx file first.");
            return;
        }

        try {
            currentResults = new RuleEngine().checkAll(loadedBlocks);
            table.setItems(FXCollections.observableArrayList(currentResults));
            status.setText("Check finished: " + currentResults.size() + " issue(s)");
        } catch (Exception ex) {
            status.setText("Check failed");
            showException("Check failed", ex);
        }
    }

    private void exportCsv() {
        if (currentResults.isEmpty()) {
            showError("No Results", "There are no results to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV File", "*.csv"));
        chooser.setInitialFileName("techproof-results.csv");
        var file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            writeCsv(file.toPath(), currentResults);
            status.setText("CSV saved: " + file.getName());
        } catch (Exception ex) {
            status.setText("CSV export failed: " + file.getName());
            showException("CSV export failed", ex);
        }
    }

    private void writeCsv(Path path, List<CheckResult> results) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("Paragraph,Location,Type,Original,Suggestion,Source,SourceUrl,Context,Reason\n");
            for (CheckResult r : results) {
                writer.write(csv(r.getParagraphNo()) + "," + csv(r.getLocation()) + "," + csv(r.getTypeLabel()) + "," +
                    csv(r.getOriginal()) + "," + csv(r.getSuggestion()) + "," + csv(r.getSourceLabel()) + "," +
                    csv(r.getSourceUrl()) + "," + csv(r.getContext()) + "," + csv(r.getReason()) + "\n");
            }
        }
    }

    private String csv(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showException(String title, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "(no message)" : ex.getMessage()));

        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        TextArea details = new TextArea(sw.toString());
        details.setEditable(false);
        details.setWrapText(false);
        details.setPrefColumnCount(100);
        details.setPrefRowCount(20);

        alert.getDialogPane().setExpandableContent(details);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }
}
