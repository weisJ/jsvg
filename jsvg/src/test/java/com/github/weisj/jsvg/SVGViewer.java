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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
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
            iconBox.setSelectedItem("clipPath/filterAndClipPath2.svg");

            JComponent contentPane = (JComponent) frame.getContentPane();
            contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.META_DOWN_MASK),
                    "selectPreviousIcon");
            contentPane.getActionMap().put("selectPreviousIcon", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int nextIndex = Math.max(0, iconBox.getSelectedIndex() - 1);
                    iconBox.setSelectedIndex(nextIndex);
                }
            });
            contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.META_DOWN_MASK),
                    "selectNextIcon");
            contentPane.getActionMap().put("selectNextIcon", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int nextIndex = Math.min(iconBox.getItemCount() - 1, iconBox.getSelectedIndex() + 1);
                    iconBox.setSelectedIndex(nextIndex);
                }
            });

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
            softClipping.doClick();
            renderingMode.add(softClipping);
            renderingMode.add(Box.createHorizontalStrut(5));

            JCheckBox lowRes = new JCheckBox("Render at intrinsic resolution");
            lowRes.addActionListener(e -> svgPanel.setRenderAtLowResolution(lowRes.isSelected()));
            renderingMode.add(lowRes);
            renderingMode.add(Box.createHorizontalStrut(5));

            JCheckBox strictRendering = new JCheckBox("Strict Mask Rendering");
            strictRendering.addActionListener(e -> svgPanel.setStrictMaskRendering(strictRendering.isSelected()));
            renderingMode.add(strictRendering);
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
        return ResourceWalker.findIcons(SVGViewer.class.getPackage(), "");
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
        private boolean lowResolution;
        private Object maskRenderingValue = SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_DEFAULT;

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

        public void setRenderAtLowResolution(boolean lowResolution) {
            this.lowResolution = lowResolution;
            repaint();
        }

        public void setStrictMaskRendering(boolean strict) {
            this.maskRenderingValue = strict
                    ? SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_ACCURACY
                    : SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_FAST;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            ((Graphics2D) g).setRenderingHint(SVGRenderingHints.KEY_MASK_CLIP_RENDERING,
                    maskRenderingValue);
            System.out.println("======");
            switch (mode) {
                case JSVG:
                    ViewBox viewBox = new ViewBox(0, 0, getWidth(), getHeight());
                    Graphics2D renderGraphics = (Graphics2D) g.create();
                    BufferedImage img = null;
                    if (this.lowResolution) {
                        viewBox = new ViewBox(document.size());
                        img = new BufferedImage((int) viewBox.width, (int) viewBox.height, BufferedImage.TYPE_INT_ARGB);
                        renderGraphics = img.createGraphics();
                    }
                    renderGraphics.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    renderGraphics.setRenderingHint(
                            RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                    renderGraphics.setRenderingHint(
                            SVGRenderingHints.KEY_SOFT_CLIPPING,
                            softClipping ? SVGRenderingHints.VALUE_SOFT_CLIPPING_ON
                                    : SVGRenderingHints.VALUE_SOFT_CLIPPING_OFF);
                    if (paintShape) {
                        Shape shape = document.computeShape(viewBox);
                        renderGraphics.setColor(Color.MAGENTA);
                        renderGraphics.fill(shape);
                    } else {
                        document.render(this, renderGraphics, viewBox);
                    }

                    if (img != null) {
                        int w = img.getWidth();
                        int h = img.getHeight();
                        double scale = getWidth() < getHeight() ? (double) getWidth() / w : (double) getHeight() / h;
                        g.translate(getWidth() / 2, getHeight() / 2);
                        ((Graphics2D) g).scale(scale, scale);
                        g.translate(-w / 2, -h / 2);
                        g.drawImage(img, 0, 0, w, h, this);
                    }
                    renderGraphics.dispose();

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
