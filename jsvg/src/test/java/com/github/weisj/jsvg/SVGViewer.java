/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.kitfox.svg.app.beans.SVGIcon;

public class SVGViewer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LafManager.install();
            JFrame frame = new JFrame("SVGViewer");

            JComboBox<String> iconBox = new JComboBox<>(new DefaultComboBoxModel<>(findIcons()));
            iconBox.setSelectedItem("mesh/mesh2.svg");

            SVGPanel svgPanel = new SVGPanel((String) Objects.requireNonNull(iconBox.getSelectedItem()));
            svgPanel.setPreferredSize(new Dimension(1000, 600));
            iconBox.addItemListener(e -> svgPanel.selectIcon((String) iconBox.getSelectedItem()));

            Box box = Box.createHorizontalBox();
            box.add(Box.createHorizontalGlue());
            box.add(iconBox);
            box.add(Box.createHorizontalGlue());

            frame.add(box, BorderLayout.NORTH);
            frame.add(svgPanel, BorderLayout.CENTER);

            Box renderingMode = Box.createHorizontalBox();
            JRadioButton jsvg = new JRadioButton(RenderingMode.JSVG.name());
            jsvg.setSelected(true);
            jsvg.addActionListener(e -> svgPanel.setRenderingMode(RenderingMode.JSVG));
            JRadioButton svgSalamander = new JRadioButton(RenderingMode.SVG_SALAMANDER.name());
            svgSalamander.addActionListener(e -> svgPanel.setRenderingMode(RenderingMode.SVG_SALAMANDER));

            ButtonGroup bg = new ButtonGroup();
            bg.add(jsvg);
            bg.add(svgSalamander);
            renderingMode.add(jsvg);
            renderingMode.add(svgSalamander);
            renderingMode.add(Box.createHorizontalGlue());
            frame.add(renderingMode, BorderLayout.SOUTH);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    private static String[] findIcons() {
        String pack = SVGViewer.class.getPackage().getName();
        try (ResourceWalker walker = ResourceWalker.walkResources(pack)) {
            return walker.stream().filter(p -> p.endsWith("svg"))
                    .map(p -> p.substring(pack.length() + 1))
                    .toArray(String[]::new);
        }
    }

    private enum RenderingMode {
        JSVG,
        SVG_SALAMANDER
    }

    private static class SVGPanel extends JPanel {
        private final Map<String, SVGDocument> iconCache = new HashMap<>();
        private SVGDocument document;
        private RenderingMode mode = RenderingMode.JSVG;
        private final SVGIcon icon = new SVGIcon() {
            @Override
            public int getIconHeightIgnoreAutosize() {
                return SVGPanel.this.getHeight();
            }

            @Override
            public int getIconWidthIgnoreAutosize() {
                return SVGPanel.this.getWidth();
            }
        };

        public SVGPanel(@NotNull String iconName) {
            selectIcon(iconName);
            icon.setAutosize(SVGIcon.AUTOSIZE_BESTFIT);
            icon.setAntiAlias(true);
        }

        private void selectIcon(@NotNull String name) {
            document = iconCache.computeIfAbsent(name, n -> {
                URL url = Objects.requireNonNull(SVGViewer.class.getResource(n));
                SVGLoader loader = new SVGLoader();
                return loader.load(url);
            });
            try {
                icon.setSvgURI(Objects.requireNonNull(SVGViewer.class.getResource(name)).toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            repaint();
        }

        private void setRenderingMode(@NotNull RenderingMode mode) {
            this.mode = mode;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            System.out.println("======");
            if (mode == RenderingMode.JSVG) {
                document.render(this, (Graphics2D) g, new ViewBox(0, 0, getWidth(), getHeight()));
            } else {
                icon.paintIcon(this, g, 0, 0);
            }
        }
    }
}
