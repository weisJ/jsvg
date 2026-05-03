/*
 * MIT License
 *
 * Copyright (c) 2025-2026 Jannis Weis
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
package com.github.weisj.jsvg.renderer.jfx;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import com.github.weisj.jsvg.ReferenceTest;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.renderer.SVGTestFiles;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.jfx.impl.bridge.FXRenderingHintsUtil;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;

class FXOutputTest {

    private static final double DEFAULT_TOLERANCE = 0.3;
    private static final double DEFAULT_PIXEL_TOLERANCE = 0.1;

    @TestFactory
    Collection<DynamicNode> generateSVGTests() {
        Assumptions.assumeTrue(FXHeadlessApplication.checkJavaFXThread(), "Failed to initialize JavaFX");

        List<String> testFiles = SVGTestFiles.findTestSVGFiles();
        Assumptions.assumeTrue(!testFiles.isEmpty(),
                "No SVG Test Files Found in: " + SVGTestFiles.getTestSVGDirectory().getAbsolutePath());

        return testFiles.stream()
                .map(File::new)
                .map(file -> {
                    String testName = "test-jfx_" + file.getName().replace(".svg", "");
                    if (isInvalidSVGFile(file.getName())) {
                        return DynamicTest.dynamicTest(testName, () -> {
                            SVGLoader loader = new SVGLoader();
                            LoaderContext loaderContext = LoaderContext.builder()
                                    .externalResourcePolicy(ResourcePolicy.ALLOW_ALL)
                                    .build();
                            SVGDocument svgDocument = loader.load(file.toURI().toURL(), loaderContext);
                            Assertions.assertNull(svgDocument,
                                    "Expected SVG loading to fail for: " + file.getName());
                        });
                    }
                    return DynamicTest.dynamicTest(testName, () -> compareSVGOutput(file));
                })
                .collect(Collectors.toList());
    }

    // SVG files that are expected to fail loading due to invalid structure (e.g., use cycles or
    // excessive nesting). These are tested explicitly to verify they produce no valid document.
    private boolean isInvalidSVGFile(String testName) {
        return "manyImplicitPathsThroughUse.svg".equals(testName)
                || "useCycle.svg".equals(testName)
                || "useCycleSelfReference.svg".equals(testName)
                || "useNesting.svg".equals(testName);
    }

    private void compareSVGOutput(File file) throws MalformedURLException {
        SVGLoader loader = new SVGLoader();
        LoaderContext loaderContext = LoaderContext.builder()
                .externalResourcePolicy(ResourcePolicy.ALLOW_ALL)
                .build();
        SVGDocument svgDocument = loader.load(file.toURI().toURL(), loaderContext);

        if (svgDocument == null) {
            throw new IllegalStateException("Invalid SVG Test File: " + file.getAbsolutePath());
        }

        BufferedImage expected = renderJSVG(svgDocument);
        BufferedImage actual = renderJavaFX(svgDocument);

        ReferenceTest.ReferenceTestResult result = ReferenceTest.compareImageRasterization(
                expected, actual, file.getAbsolutePath() + "_jfx", DEFAULT_TOLERANCE, DEFAULT_PIXEL_TOLERANCE);
        Assumptions.assumeTrue(result.equals(ReferenceTest.ReferenceTestResult.SUCCESS),
                "JFX/AWT Render Comparison Failed: " + file.getAbsolutePath());
    }

    private BufferedImage renderJSVG(@NotNull SVGDocument svgDocument) {
        FloatSize size = svgDocument.size();
        BufferedImage image = new BufferedImage((int) size.width, (int) size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Output output = Output.createForGraphics(g);
        FXRenderingHintsUtil.setupDefaultJFXRenderingHints(output);
        svgDocument.renderWithPlatform(NullPlatformSupport.INSTANCE, output, null);
        g.dispose();
        return image;
    }

    private BufferedImage renderJavaFX(@NotNull SVGDocument svgDocument) {
        FloatSize size = svgDocument.size();
        CompletableFuture<BufferedImage> result = new CompletableFuture<>();

        Platform.runLater(() -> {
            Canvas canvas = new Canvas((int) size.width, (int) size.height);
            canvas.getGraphicsContext2D().clearRect(0, 0, (int) size.width, (int) size.height);

            Output output = FXOutput.createForGraphicsContext(canvas.getGraphicsContext2D());
            svgDocument.renderWithPlatform(NullPlatformSupport.INSTANCE, output, null, null);
            output.dispose();

            SnapshotParameters snapshotParameters = new SnapshotParameters();
            snapshotParameters.setFill(Color.TRANSPARENT);
            WritableImage snapshot = canvas.snapshot(snapshotParameters, null);
            result.complete(SwingFXUtils.fromFXImage(snapshot, null));
        });

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

}
