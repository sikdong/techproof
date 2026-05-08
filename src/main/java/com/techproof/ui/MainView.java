package com.techproof.ui;

import com.techproof.checker.ReferenceSignChecker;
import com.techproof.checker.RuleEngine;
import com.techproof.docx.DocxReader;
import com.techproof.model.CheckResult;
import com.techproof.model.IssueType;
import com.techproof.model.ParagraphBlock;
import com.techproof.model.ReferenceSignEntry;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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
    private final TableView<CheckResult> typoTable = new TableView<>();
    private final TableView<ReferenceSignEntry> referenceSignTable = new TableView<>();
    private final ComboBox<String> referenceStatusFilter = new ComboBox<>();
    private final TextArea preview = new TextArea();
    private final Label status = new Label("Select a .docx or .doc file.");
    private final Label progressLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar();
    private Button openButton;
    private Button checkButton;
    private Button exportButton;
    private List<ParagraphBlock> loadedBlocks = List.of();
    private List<CheckResult> currentResults = List.of();
    private List<ReferenceSignEntry> currentReferenceSignEntries = List.of();

    public MainView(Stage stage) {
        this.stage = stage;
        buildUi();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildUi() {
        openButton = new Button("Open Word File");
        checkButton = new Button("Run Check");
        exportButton = new Button("Export CSV");

        openButton.setOnAction(e -> openFile());
        checkButton.setOnAction(e -> runCheck());
        exportButton.setOnAction(e -> exportCsv());

        progressBar.setPrefWidth(140);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        progressLabel.setVisible(false);
        progressLabel.setManaged(false);

        HBox top = new HBox(10, openButton, checkButton, exportButton, progressLabel, progressBar, status);
        top.setPadding(new Insets(12));

        buildResultTable(typoTable);
        buildReferenceSignTable();

        preview.setEditable(false);
        preview.setWrapText(true);
        preview.setPromptText("Paragraph preview will appear after opening a file.");

        TabPane resultTabs = new TabPane();
        resultTabs.getTabs().add(resultTab("오타 검사 항목", typoTable));
        resultTabs.getTabs().add(resultTab("도면부호 검사 항목", buildReferenceSignPane()));

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(resultTabs, preview);
        splitPane.setDividerPositions(0.72);

        Label help = new Label("Particle check is not an auto-fix. Text in parentheses is ignored only for particle rules.");
        help.setPadding(new Insets(8));

        root.setTop(top);
        root.setCenter(splitPane);
        root.setBottom(new VBox(help));
    }

    private Tab resultTab(String title, Parent content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    @SuppressWarnings("unchecked")
    private void buildResultTable(TableView<CheckResult> table) {
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

    private Parent buildReferenceSignPane() {
        referenceStatusFilter.setItems(FXCollections.observableArrayList("전체", "일치", "불일치"));
        referenceStatusFilter.setValue("전체");
        referenceStatusFilter.setPrefWidth(120);
        referenceStatusFilter.valueProperty().addListener((obs, old, value) -> applyReferenceSignFilter());

        HBox filterBar = new HBox(8, new Label("상태"), referenceStatusFilter);
        filterBar.setPadding(new Insets(8));

        VBox pane = new VBox(filterBar, referenceSignTable);
        referenceSignTable.prefHeightProperty().bind(pane.heightProperty().subtract(filterBar.heightProperty()));
        return pane;
    }

    @SuppressWarnings("unchecked")
    private void buildReferenceSignTable() {
        TableColumn<ReferenceSignEntry, String> statusColumn = referenceCol("상태", ReferenceSignEntry::getStatus, 80);
        TableColumn<ReferenceSignEntry, String> signColumn =
            referenceCol("도면부호", ReferenceSignEntry::getDisplaySign, 120);
        TableColumn<ReferenceSignEntry, String> nameColumn = referenceCol("명칭", ReferenceSignEntry::getName, 260);
        TableColumn<ReferenceSignEntry, String> expectedColumn =
            referenceCol("기준 도면부호", ReferenceSignEntry::getDisplayExpectedSign, 130);
        TableColumn<ReferenceSignEntry, String> paragraphColumn =
            referenceCol("Paragraph", r -> String.valueOf(r.getParagraphNo()), 90);
        TableColumn<ReferenceSignEntry, String> locationColumn =
            referenceCol("Location", ReferenceSignEntry::getLocation, 120);
        TableColumn<ReferenceSignEntry, String> contextColumn =
            referenceCol("Context", ReferenceSignEntry::getContext, 320);

        referenceSignTable.getColumns().addAll(List.of(
            statusColumn,
            signColumn,
            nameColumn,
            expectedColumn,
            paragraphColumn,
            locationColumn,
            contextColumn
        ));
        referenceSignTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        referenceSignTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(ReferenceSignEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                if (item.isMismatch()) {
                    setStyle(
                        "-fx-background-color: #fff59d;"
                            + "-fx-text-background-color: #111111;"
                            + "-fx-selection-bar-text: #111111;"
                    );
                } else {
                    setStyle("");
                }
            }
        });
        referenceSignTable.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null) {
                preview.setText("[Paragraph " + value.getParagraphNo() + "] " + value.getContext());
            }
        });
    }

    private TableColumn<ReferenceSignEntry, String> referenceCol(
        String title,
        Function<ReferenceSignEntry, String> getter,
        int width
    ) {
        TableColumn<ReferenceSignEntry, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Word File");
        chooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("Word Document (*.docx, *.doc)", "*.docx", "*.doc")
        );
        var file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            loadedBlocks = new DocxReader().read(file.toPath());
            currentResults = List.of();
            setResultTables(currentResults);
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
            showError("No Document", "Open a .docx or .doc file first.");
            return;
        }

        Task<CheckRunResult> task = new Task<>() {
            @Override
            protected CheckRunResult call() {
                updateMessage("Checking...");
                updateProgress(0, loadedBlocks.size() + 1);
                List<CheckResult> results = new RuleEngine().checkAll(loadedBlocks, (completedSteps, totalSteps) -> {
                    updateMessage("Checking... " + completedSteps + "/" + totalSteps);
                    updateProgress(completedSteps, totalSteps);
                });
                List<ReferenceSignEntry> referenceEntries = new ReferenceSignChecker().entries(loadedBlocks);
                return new CheckRunResult(results, referenceEntries);
            }
        };

        setChecking(true);
        progressLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            CheckRunResult result = task.getValue();
            currentResults = result.issues();
            setResultTables(currentResults, result.referenceEntries());
            status.setText("Check finished: " + currentResults.size() + " issue(s)");
            setChecking(false);
            showInfo(
                "TechProof 검사 완료",
                "검사가 완료되었습니다.\n발견된 오류 후보: " + currentResults.size() + "건"
            );
        });

        task.setOnFailed(e -> {
            status.setText("Check failed");
            setChecking(false);
            Throwable error = task.getException();
            if (error instanceof Exception ex) {
                showException("Check failed", ex);
            } else {
                showException("Check failed", new RuntimeException(error));
            }
        });

        Thread worker = new Thread(task, "techproof-check");
        worker.setDaemon(true);
        worker.start();
    }

    private void setResultTables(List<CheckResult> results) {
        setResultTables(results, List.of());
    }

    private void setResultTables(List<CheckResult> results, List<ReferenceSignEntry> referenceEntries) {
        List<CheckResult> typoResults = results.stream()
            .filter(result -> !isReferenceSignResult(result))
            .toList();

        currentReferenceSignEntries = referenceEntries;
        typoTable.setItems(FXCollections.observableArrayList(typoResults));
        applyReferenceSignFilter();
    }

    private void applyReferenceSignFilter() {
        String selectedStatus = referenceStatusFilter.getValue();
        List<ReferenceSignEntry> filteredEntries = currentReferenceSignEntries.stream()
            .filter(entry -> selectedStatus == null
                || selectedStatus.equals("전체")
                || selectedStatus.equals(entry.getStatus()))
            .toList();
        referenceSignTable.setItems(FXCollections.observableArrayList(filteredEntries));
    }

    private boolean isReferenceSignResult(CheckResult result) {
        return result.getType() == IssueType.REFERENCE_SIGN;
    }

    private void setChecking(boolean checking) {
        openButton.setDisable(checking);
        checkButton.setDisable(checking);
        exportButton.setDisable(checking);

        if (!checking) {
            progressLabel.textProperty().unbind();
            progressBar.progressProperty().unbind();
            progressLabel.setText("");
            progressBar.setProgress(0);
        }
        progressLabel.setVisible(checking);
        progressLabel.setManaged(checking);
        progressBar.setVisible(checking);
        progressBar.setManaged(checking);
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

    private record CheckRunResult(List<CheckResult> issues, List<ReferenceSignEntry> referenceEntries) {
    }
}
