package com.github.weisj.jsvg.renderer.jfx.viewer;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.renderer.FXTestSVGFiles;
import com.github.weisj.jsvg.ui.jfx.FXSVGCanvas;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class FXTestViewerController {

    private final ObjectProperty<SVGDocument> currentSVG = new SimpleObjectProperty<>();

    public ComboBox<String> comboBoxSVGDocument;
    public ScrollPane scrollPaneJFX;
    public FXSVGCanvas svgCanvasJFX;

    public ScrollPane scrollPaneAWT;
    public FXSVGCanvas svgCanvasAWT;

    public void initialize() {
        List<String> testSVGFiles = FXTestSVGFiles.findTestSVGFiles();
        comboBoxSVGDocument.getItems().addAll(testSVGFiles);
        if(!testSVGFiles.isEmpty()) {
            comboBoxSVGDocument.setValue(testSVGFiles.getFirst());
        }

        svgCanvasJFX.setRenderBackend(FXSVGCanvas.RenderBackend.JavaFX);
        svgCanvasJFX.documentProperty().bind(currentSVG);
        scrollPaneJFX.setPannable(true);

        svgCanvasAWT.setRenderBackend(FXSVGCanvas.RenderBackend.AWT);
        svgCanvasAWT.documentProperty().bind(currentSVG);
        scrollPaneAWT.setPannable(true);

        // Bind viewport positions
        scrollPaneJFX.hvalueProperty().bindBidirectional(scrollPaneAWT.hvalueProperty());
        scrollPaneJFX.vvalueProperty().bindBidirectional(scrollPaneAWT.vvalueProperty());

        currentSVG.bind(comboBoxSVGDocument.valueProperty().map(file -> {
            if(!file.endsWith("svg")){
                return null;
            }
            URL url;
            try {
                url = new File(file).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            SVGLoader loader = new SVGLoader();
            LoaderContext loaderContext = LoaderContext.builder()
                .externalResourcePolicy(ResourcePolicy.ALLOW_ALL)
                .build();
            return loader.load(url, loaderContext);
        }));
    }

    public void refreshCanvas() {
        svgCanvasAWT.repaint();
        svgCanvasJFX.repaint();
    }
}
