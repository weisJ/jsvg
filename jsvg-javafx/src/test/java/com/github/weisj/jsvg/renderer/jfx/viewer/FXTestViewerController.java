/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.github.weisj.jsvg.renderer.jfx.viewer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.util.Callback;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.renderer.FXTestSVGFiles;
import com.github.weisj.jsvg.ui.jfx.FXSVGCanvas;


public class FXTestViewerController {

    private final ObjectProperty<SVGDocument> currentSVG = new SimpleObjectProperty<>();

    public ComboBox<String> comboBoxSVGDocument;
    public ScrollPane scrollPaneJFX;
    public FXSVGCanvas svgCanvasJFX;

    public ScrollPane scrollPaneAWT;
    public FXSVGCanvas svgCanvasAWT;
    public CheckBox checkBoxShowTransparentPattern;

    public void initialize() {
        String svgDirectoryPrefix = FXTestSVGFiles.getTestSVGDirectory().toPath().toString();
        List<String> testSVGFiles = FXTestSVGFiles.findTestSVGFiles();
        comboBoxSVGDocument.getItems().addAll(testSVGFiles);
        if (!testSVGFiles.isEmpty()) {
            comboBoxSVGDocument.setValue(testSVGFiles.getFirst());
            Callback<ListView<String>, ListCell<String>> cellFactory = view -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && item.startsWith(svgDirectoryPrefix)) {
                        setText(item.substring(svgDirectoryPrefix.length() + 1));
                    }
                }
            };
            comboBoxSVGDocument.setCellFactory(cellFactory);
            comboBoxSVGDocument.setButtonCell(cellFactory.call(null));
        }

        svgCanvasJFX.setRenderBackend(FXSVGCanvas.RenderBackend.JavaFX);
        svgCanvasJFX.documentProperty().bind(currentSVG);
        svgCanvasJFX.showTransparentPatternProperty().bind(checkBoxShowTransparentPattern.selectedProperty());
        scrollPaneJFX.setPannable(true);

        svgCanvasAWT.setRenderBackend(FXSVGCanvas.RenderBackend.AWT);
        svgCanvasAWT.documentProperty().bind(currentSVG);
        svgCanvasAWT.showTransparentPatternProperty().bind(checkBoxShowTransparentPattern.selectedProperty());
        scrollPaneAWT.setPannable(true);

        // Bind viewport positions
        scrollPaneJFX.hvalueProperty().bindBidirectional(scrollPaneAWT.hvalueProperty());
        scrollPaneJFX.vvalueProperty().bindBidirectional(scrollPaneAWT.vvalueProperty());

        currentSVG.bind(Bindings.createObjectBinding(() -> {
            String file = comboBoxSVGDocument.getValue();
            if (file == null || !file.endsWith("svg")) {
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
        }, comboBoxSVGDocument.valueProperty()));
    }

    public void refreshCanvas() {
        svgCanvasAWT.repaint();
        svgCanvasJFX.repaint();
    }

    public void previousSVG() {
        int index = comboBoxSVGDocument.getSelectionModel().getSelectedIndex();
        if (index > 0) {
            comboBoxSVGDocument.getSelectionModel().select(index - 1);
        }
    }

    public void nextSVG(ActionEvent actionEvent) {
        int index = comboBoxSVGDocument.getSelectionModel().getSelectedIndex();
        if (index < comboBoxSVGDocument.getItems().size() - 1) {
            comboBoxSVGDocument.getSelectionModel().select(index + 1);
        }
    }
}
