package com.github.weisj.jsvg.renderer.jfx.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Start with {@link FXTestViewerLauncher}
 */
public class FXTestViewerApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace(System.err));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("svg-viewer.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
