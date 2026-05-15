package com.techproof;

import com.techproof.ui.MainView;
import com.techproof.update.UpdateNotifier;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TechProofApp extends Application {
    static final String FALLBACK_VERSION = "0.2.5";

    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView(stage);
        Scene scene = new Scene(mainView.getRoot(), 1100, 720);
        var stylesheet = TechProofApp.class.getResource("/styles/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        stage.setTitle("TechProof - 기술 문서 오타/도면부호 검사기");
        stage.setScene(scene);
        stage.show();
        UpdateNotifier.checkForUpdates(stage, getHostServices(), currentVersion());
    }

    public static void main(String[] args) {
        launch(args);
    }

    private String currentVersion() {
        String version = TechProofApp.class.getPackage().getImplementationVersion();
        return resolveCurrentVersion(version);
    }

    static String resolveCurrentVersion(String implementationVersion) {
        return implementationVersion == null || implementationVersion.isBlank()
            ? FALLBACK_VERSION
            : implementationVersion;
    }
}
