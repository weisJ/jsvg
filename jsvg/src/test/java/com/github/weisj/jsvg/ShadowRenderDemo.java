/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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
package com.github.weisj.jsvg;

import java.awt.*;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.jsvg.parser.SVGLoader;

public final class ShadowRenderDemo {

    private ShadowRenderDemo() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LafManager.installTheme(LafManager.getPreferredThemeStyle());
            JFrame frame = new JFrame("ShadowTest");

            SVGLoader loader = new SVGLoader();
            String folder = "shadow";
            Function<@NotNull String, @NotNull SVGDocument> loadShadow = name -> Objects.requireNonNull(
                    loader.load(Objects.requireNonNull(ShadowRenderDemo.class.getResource(folder + "/" + name))));
            SVGDocument bottom = loadShadow.apply("bottom.svg");
            SVGDocument bottomLeft = loadShadow.apply("bottomLeft.svg");
            SVGDocument bottomRight = loadShadow.apply("bottomRight.svg");
            SVGDocument left = loadShadow.apply("left.svg");
            SVGDocument right = loadShadow.apply("right.svg");
            SVGDocument top = loadShadow.apply("top.svg");
            SVGDocument topLeft = loadShadow.apply("topLeft.svg");
            SVGDocument topRight = loadShadow.apply("topRight.svg");

            JPanel svgPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int scale = 1;
                    int repeat = 4;

                    int totalHeight =
                            (int) (topLeft.size().height + bottomLeft.size().height + left.size().height * repeat);
                    int totalWidth =
                            (int) (bottomLeft.size().width + bottomRight.size().width + bottom.size().width * repeat);

                    int x = (getWidth() - totalWidth * scale) / 2;
                    int y = (getHeight() - totalHeight) / 2;

                    g.translate(x, y);
                    Graphics2D g2 = (Graphics2D) g;

                    g2.scale(scale, scale);

                    Graphics2D rowG = (Graphics2D) g2.create();

                    topLeft.render(this, rowG);
                    rowG.translate(topLeft.size().width, 0);
                    for (int i = 0; i < repeat; i++) {
                        top.render(this, rowG);
                        rowG.translate(top.size().width, 0);
                    }
                    topRight.render(this, rowG);
                    rowG.translate(8, topRight.size().height);
                    for (int i = 0; i < repeat; i++) {
                        right.render(this, rowG);
                        rowG.translate(0, right.size().height);
                    }


                    rowG.dispose();
                    rowG = (Graphics2D) g2.create();
                    rowG.translate(0, topLeft.size().height);

                    for (int i = 0; i < repeat; i++) {
                        left.render(this, rowG);
                        rowG.translate(0, left.size().height);
                    }
                    bottomLeft.render(this, rowG);
                    rowG.translate(bottomLeft.size().width, 8);
                    for (int i = 0; i < repeat; i++) {
                        bottom.render(this, rowG);
                        rowG.translate(bottom.size().width, 0);
                    }

                    rowG.translate(0, -8);
                    bottomRight.render(this, rowG);

                    rowG.dispose();
                }
            };

            svgPanel.setPreferredSize(new Dimension(1000, 600));


            frame.add(svgPanel, BorderLayout.CENTER);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

}
