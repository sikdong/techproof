package com.techproof.update;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public final class UpdateNotifier {
    private UpdateNotifier() {
    }

    public static void checkForUpdates(Stage owner, HostServices hostServices, String currentVersion) {
        new GitHubReleaseChecker().findUpdate(currentVersion)
            .thenAccept(update -> update.ifPresent(release ->
                Platform.runLater(() -> showUpdateDialog(owner, hostServices, release))))
            .exceptionally(error -> null);
    }

    private static void showUpdateDialog(Stage owner, HostServices hostServices, ReleaseInfo release) {
        ButtonType download = new ButtonType("다운로드", ButtonBar.ButtonData.OK_DONE);
        ButtonType later = new ButtonType("나중에", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", download, later);
        alert.initOwner(owner);
        alert.setTitle("TechProof 업데이트");
        alert.setHeaderText("TechProof v" + release.version() + " 새 버전이 있습니다.");
        alert.setContentText("새 기능과 수정 사항을 확인하고 설치 파일을 받을 수 있습니다.");
        alert.getDialogPane().setExpandableContent(releaseNotes(release.notes()));
        alert.getDialogPane().setExpanded(true);

        alert.showAndWait()
            .filter(download::equals)
            .ifPresent(button -> hostServices.showDocument(release.downloadUrl()));
    }

    private static TextArea releaseNotes(String notes) {
        TextArea textArea = new TextArea(notes == null || notes.isBlank() ? "릴리스 설명이 없습니다." : notes);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefColumnCount(70);
        textArea.setPrefRowCount(10);
        return textArea;
    }
}
