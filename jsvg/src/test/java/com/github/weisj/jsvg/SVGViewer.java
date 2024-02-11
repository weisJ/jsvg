/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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

import org.apache.batik.swing.JSVGCanvas;
import org.ehcache.sizeof.SizeOf;
import org.jetbrains.annotations.NotNull;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.kitfox.svg.app.beans.SVGIcon;

public final class SVGViewer {

    private SVGViewer() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LafManager.installTheme(LafManager.getPreferredThemeStyle());
            JFrame frame = new JFrame("SVGViewer");

            JComboBox<String> iconBox = new JComboBox<>(new DefaultComboBoxModel<>(findIcons()));
            iconBox.setSelectedItem("tmp.svg");

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
            JRadioButton batik = new JRadioButton(RenderingMode.BATIK.name());
            batik.addActionListener(e -> svgPanel.setRenderingMode(RenderingMode.BATIK));


            ButtonGroup bg = new ButtonGroup();
            bg.add(jsvg);
            bg.add(svgSalamander);
            bg.add(batik);
            renderingMode.add(jsvg);
            renderingMode.add(svgSalamander);
            renderingMode.add(batik);
            renderingMode.add(Box.createHorizontalStrut(5));

            JCheckBox paintShape = new JCheckBox("Paint SVG shape");
            paintShape.addActionListener(e -> svgPanel.setPaintSVGShape(paintShape.isSelected()));
            renderingMode.add(paintShape);
            renderingMode.add(Box.createHorizontalStrut(5));

            JCheckBox softClipping = new JCheckBox("Soft clipping");
            softClipping.addActionListener(e -> svgPanel.setSoftClipping(softClipping.isSelected()));
            renderingMode.add(softClipping);
            renderingMode.add(Box.createHorizontalGlue());

            JButton resourceInfo = new JButton("Print Memory");
            resourceInfo.addActionListener(e -> svgPanel.printMemory());
            renderingMode.add(resourceInfo);


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
                    .sorted()
                    .toArray(String[]::new);
        }
    }

    private enum RenderingMode {
        JSVG,
        SVG_SALAMANDER,
        BATIK
    }

    private static final class SVGPanel extends JPanel {
        private final Map<String, SVGDocument> iconCache = new HashMap<>();
        private SVGDocument document;
        private String selectedIconName;
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
        private final JSVGCanvas jsvgCanvas = new JSVGCanvas();
        private boolean paintShape;
        private boolean softClipping;

        public SVGPanel(@NotNull String iconName) {
            selectIcon(iconName);
            setBackground(Color.WHITE);
            setOpaque(true);
            icon.setAutosize(SVGIcon.AUTOSIZE_BESTFIT);
            icon.setAntiAlias(true);
        }

        private void printMemory() {
            switch (mode) {
                case JSVG -> System.out.println(mode + " Memory: "
                        + SizeOf.newInstance().deepSizeOf(document));
                case SVG_SALAMANDER -> System.out.println(mode + " Memory: "
                        + SizeOf.newInstance().deepSizeOf(icon.getSvgUniverse().getDiagram(icon.getSvgURI())));
                case BATIK -> System.out.println(mode + " Memory: "
                        + SizeOf.newInstance().deepSizeOf(jsvgCanvas.getSVGDocument()));
            }
        }

        private void selectIcon(@NotNull String name) {
            remove(jsvgCanvas);
            switch (mode) {
                case JSVG -> document = iconCache.computeIfAbsent(name, n -> {
                    URL url = Objects.requireNonNull(SVGViewer.class.getResource(n));
                    SVGLoader loader = new SVGLoader();
                    return loader.load(url);
                });
                case SVG_SALAMANDER -> {
                    try {
                        icon.setSvgURI(Objects.requireNonNull(SVGViewer.class.getResource(name)).toURI());
                    } catch (URISyntaxException e) {
                        throw new IllegalStateException(e);
                    }
                }
                case BATIK -> {
                    add(jsvgCanvas);
                    try {
                        jsvgCanvas.setURI(
                                Objects.requireNonNull(SVGViewer.class.getResource(name)).toURI().toASCIIString());
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            this.selectedIconName = name;
            repaint();
        }

        @Override
        public void doLayout() {
            super.doLayout();
            jsvgCanvas.setBounds(0, 0, getWidth(), getHeight());
        }

        private void setRenderingMode(@NotNull RenderingMode mode) {
            this.mode = mode;
            selectIcon(selectedIconName);
        }

        public void setPaintSVGShape(boolean paintShape) {
            this.paintShape = paintShape;
            repaint();
        }

        public void setSoftClipping(boolean softClipping) {
            this.softClipping = softClipping;
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
            switch (mode) {
                case JSVG:
                    ViewBox viewBox = new ViewBox(0, 0, getWidth(), getHeight());
                    ((Graphics2D) g).setRenderingHint(
                            SVGRenderingHints.KEY_SOFT_CLIPPING,
                            softClipping ? SVGRenderingHints.VALUE_SOFT_CLIPPING_ON
                                    : SVGRenderingHints.VALUE_SOFT_CLIPPING_OFF);
                    if (paintShape) {
                        Shape shape = document.computeShape(viewBox);
                        g.setColor(Color.MAGENTA);
                        ((Graphics2D) g).fill(shape);
                    } else {
                        document.render(this, (Graphics2D) g, viewBox);
                    }
                    break;
                case SVG_SALAMANDER:
                    icon.paintIcon(this, g, 0, 0);
                    break;
                case BATIK:
                    break;
            }
        }
    }
}
