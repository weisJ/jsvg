/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
import org.jetbrains.annotations.Nullable;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.jsvg.parser.*;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.renderer.RenderConfig;
import com.github.weisj.jsvg.renderer.SVGRenderingHints;
import com.github.weisj.jsvg.renderer.awt.AwtComponentPlatformSupport;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.ui.AnimationPlayer;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import com.kitfox.svg.app.beans.SVGIcon;

public final class SVGViewer {
    private static final String NO_VIEW = "<None>";

    private SVGViewer() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            JFrame frame = createFrame();
            frame.setVisible(true);
        });
    }

    private static void installLookAndFeel() {
        try {
            LafManager.installTheme(LafManager.getPreferredThemeStyle());
        } catch (LinkageError e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                // Keep Swing's cross-platform look and feel.
            }
        }
    }

    private static @NotNull JFrame createFrame() {
        JFrame frame = new JFrame("SVGViewer");

        JComboBox<String> iconBox = new JComboBox<>(new DefaultComboBoxModel<>(findIcons()));
        iconBox.setSelectedItem("tmp.svg");
        iconBox.setToolTipText("Select an SVG document (⌘← / ⌘→)");
        Dimension selectorSize = iconBox.getPreferredSize();
        selectorSize.width = 420;
        iconBox.setPreferredSize(selectorSize);

        JComponent contentPane = (JComponent) frame.getContentPane();
        installIconSelectionShortcuts(contentPane, iconBox);

        SVGPanel svgPanel = new SVGPanel((String) Objects.requireNonNull(iconBox.getSelectedItem()));
        svgPanel.setPreferredSize(new Dimension(1000, 600));
        iconBox.addActionListener(e -> svgPanel.selectIcon((String) Objects.requireNonNull(iconBox.getSelectedItem())));

        frame.add(createDocumentSelector(iconBox), BorderLayout.NORTH);
        frame.add(svgPanel, BorderLayout.CENTER);
        frame.add(createControls(svgPanel), BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }

    private static @NotNull JComponent createDocumentSelector(@NotNull JComboBox<String> iconBox) {
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        selector.add(createSectionLabel("Document"));
        selector.add(iconBox);
        return selector;
    }

    private static @NotNull JComponent createControls(@NotNull SVGPanel svgPanel) {
        Box controls = Box.createVerticalBox();
        controls.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));

        Box renderingControls = createRenderingControls(svgPanel);
        renderingControls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(renderingControls);
        controls.add(Box.createVerticalStrut(4));

        Box displayControls = createDisplayControls(svgPanel);
        displayControls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(displayControls);
        controls.add(Box.createVerticalStrut(4));

        DocumentControls documentControls = new DocumentControls(svgPanel);
        documentControls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(documentControls);
        return controls;
    }

    private static void installIconSelectionShortcuts(
            @NotNull JComponent contentPane,
            @NotNull JComboBox<String> iconBox) {
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
    }

    private static @NotNull Box createRenderingControls(@NotNull SVGPanel svgPanel) {
        Box controls = Box.createHorizontalBox();
        controls.add(createSectionLabel("Renderer"));
        controls.add(Box.createHorizontalStrut(6));

        JRadioButton jsvg = new JRadioButton("JSVG");
        jsvg.setSelected(true);
        jsvg.addActionListener(e -> svgPanel.setRenderingMode(RenderingMode.JSVG));
        JRadioButton svgSalamander = new JRadioButton("SVG Salamander");
        svgSalamander.addActionListener(e -> svgPanel.setRenderingMode(RenderingMode.SVG_SALAMANDER));
        JRadioButton batik = new JRadioButton("Batik");
        batik.addActionListener(e -> svgPanel.setRenderingMode(RenderingMode.BATIK));

        ButtonGroup bg = new ButtonGroup();
        bg.add(jsvg);
        bg.add(svgSalamander);
        bg.add(batik);
        controls.add(jsvg);
        controls.add(svgSalamander);
        controls.add(batik);
        controls.add(Box.createHorizontalGlue());

        JButton resourceInfo = new JButton("Print memory usage");
        resourceInfo.setToolTipText("Print estimated renderer memory usage to the console");
        resourceInfo.addActionListener(e -> svgPanel.printMemory());
        controls.add(resourceInfo);
        return controls;
    }

    private static @NotNull Box createDisplayControls(@NotNull SVGPanel svgPanel) {
        Box controls = Box.createHorizontalBox();
        controls.add(createSectionLabel("Display"));
        controls.add(Box.createHorizontalStrut(6));

        JCheckBox paintShape = new JCheckBox("Shape preview");
        paintShape.setToolTipText("Render the computed SVG shape in magenta instead of its normal paint");
        paintShape.addActionListener(e -> svgPanel.setPaintSVGShape(paintShape.isSelected()));
        controls.add(paintShape);

        JCheckBox softClipping = new JCheckBox("Soft clipping");
        softClipping.setToolTipText("Use antialiased clipping for SVG clip paths");
        softClipping.addActionListener(e -> svgPanel.setSoftClipping(softClipping.isSelected()));
        softClipping.doClick();
        controls.add(softClipping);

        JCheckBox lowRes = new JCheckBox("Intrinsic resolution");
        lowRes.setToolTipText("Render at the SVG's intrinsic resolution, then scale the result");
        lowRes.addActionListener(e -> svgPanel.setRenderAtLowResolution(lowRes.isSelected()));
        controls.add(lowRes);

        JCheckBox intrinsicSize = new JCheckBox("Intrinsic size");
        intrinsicSize.setToolTipText("Center the SVG at its intrinsic size instead of filling the viewer");
        intrinsicSize.addActionListener(e -> svgPanel.setRenderAtIntrinsicSize(intrinsicSize.isSelected()));
        controls.add(intrinsicSize);

        JCheckBox strictRendering = new JCheckBox("Accurate masks");
        strictRendering.setToolTipText("Prefer mask rendering accuracy over speed");
        strictRendering.addActionListener(e -> svgPanel.setStrictMaskRendering(strictRendering.isSelected()));
        controls.add(strictRendering);
        controls.add(Box.createHorizontalGlue());
        return controls;
    }

    private static @NotNull JLabel createSectionLabel(@NotNull String text) {
        JLabel label = new JLabel(text + ":");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private static void addSectionSeparator(@NotNull Container controls) {
        controls.add(Box.createHorizontalStrut(8));
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setMaximumSize(new Dimension(1, 22));
        separator.setPreferredSize(new Dimension(1, 22));
        controls.add(separator);
        controls.add(Box.createHorizontalStrut(8));
    }

    private static final class DocumentControls extends JPanel {
        private final @NotNull JLabel animationLabel = createSectionLabel("Animation");
        private final @NotNull JButton restartAnimation = new JButton("Restart");
        private final @NotNull JToggleButton pauseAnimation = new JToggleButton("Pause");
        private final @NotNull JLabel viewLabel = createSectionLabel("SVG view");
        private final @NotNull JComboBox<String> viewSelection = new JComboBox<>();

        private DocumentControls(@NotNull SVGPanel svgPanel) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(false);

            restartAnimation.setToolTipText("Restart the animation from the beginning");
            restartAnimation.addActionListener(e -> {
                svgPanel.restartAnimation();
                pauseAnimation.setSelected(false);
            });

            pauseAnimation.setToolTipText("Pause the animation");
            pauseAnimation.addItemListener(e -> updatePauseButton());
            pauseAnimation.addActionListener(e -> svgPanel.setAnimationState(!pauseAnimation.isSelected()));

            Dimension viewSize = viewSelection.getPreferredSize();
            viewSize.width = 220;
            viewSelection.setPreferredSize(viewSize);
            viewSelection.setMaximumSize(viewSize);
            viewSelection.setToolTipText("Select a named <view> declared by this SVG");
            viewSelection.addActionListener(e -> {
                String selectedView = (String) viewSelection.getSelectedItem();
                svgPanel.setViewName(NO_VIEW.equals(selectedView) ? null : selectedView);
            });

            add(animationLabel);
            add(Box.createHorizontalStrut(6));
            add(restartAnimation);
            add(Box.createHorizontalStrut(4));
            add(pauseAnimation);
            addSectionSeparator(this);
            add(viewLabel);
            add(Box.createHorizontalStrut(6));
            add(viewSelection);
            add(Box.createHorizontalGlue());

            svgPanel.addPropertyChangeListener(
                    SVGPanel.DOCUMENT_PROPERTY, e -> updateDocument((SVGDocument) e.getNewValue()));
            updateDocument(svgPanel.document());
        }

        private void updatePauseButton() {
            boolean paused = pauseAnimation.isSelected();
            pauseAnimation.setText(paused ? "Resume" : "Pause");
            pauseAnimation.setToolTipText(paused ? "Resume the animation" : "Pause the animation");
        }

        private void updateDocument(@Nullable SVGDocument document) {
            boolean animated = document != null && document.isAnimated();
            animationLabel.setEnabled(animated);
            restartAnimation.setEnabled(animated);
            pauseAnimation.setEnabled(animated);
            pauseAnimation.setSelected(false);

            DefaultComboBoxModel<String> views = new DefaultComboBoxModel<>();
            views.addElement(NO_VIEW);
            if (document != null) document.viewNames().forEach(views::addElement);
            viewSelection.setModel(views);

            boolean hasViews = views.getSize() > 1;
            viewLabel.setEnabled(hasViews);
            viewSelection.setEnabled(hasViews);
        }
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
        private static final String DOCUMENT_PROPERTY = "document";

        private final Map<String, SVGDocument> iconCache = new HashMap<>();
        private @Nullable SVGDocument document;
        private @Nullable String selectedIconName;
        private @Nullable com.github.weisj.jsvg.view.View selectedView;
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
        private boolean intrinsicSize;
        private Object maskRenderingValue = SVGRenderingHints.VALUE_MASK_CLIP_RENDERING_DEFAULT;

        private final @NotNull AnimationPlayer animationPlayer = new AnimationPlayer(e -> repaint());


        public SVGPanel(@NotNull String iconName) {
            setBackground(Color.WHITE);
            setOpaque(true);
            icon.setAutosize(SVGIcon.AUTOSIZE_BESTFIT);
            icon.setAntiAlias(true);
            selectIcon(iconName);
        }

        private @Nullable SVGDocument document() {
            return document;
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

        private void restartAnimation() {
            if (document == null || !document.isAnimated()) return;
            animationPlayer.stop();
            animationPlayer.start();
        }

        public void setAnimationState(boolean playing) {
            if (playing == animationPlayer.isRunning()) return;
            if (playing) {
                animationPlayer.resume();
            } else {
                animationPlayer.pause();
            }
        }

        private void selectIcon(@NotNull String name) {
            if (name.equals(selectedIconName)) return;

            animationPlayer.stop();
            SVGDocument oldDocument = document;
            document = iconCache.computeIfAbsent(name, this::loadDocument);
            selectedIconName = name;
            selectedView = null;
            animationPlayer.setAnimation(document != null ? document.animation() : null);
            if (document != null && document.isAnimated()) restartAnimation();

            configureRenderer();
            firePropertyChange(DOCUMENT_PROPERTY, oldDocument, document);
        }

        private @Nullable SVGDocument loadDocument(@NotNull String name) {
            SVGLoader loader = new SVGLoader();
            LoaderContext loaderContext = LoaderContext.builder()
                    .externalResourcePolicy(ResourcePolicy.ALLOW_ALL)
                    .build();
            return loader.load(iconUrl(name), loaderContext);
        }

        private void configureRenderer() {
            String iconName = Objects.requireNonNull(selectedIconName);
            remove(jsvgCanvas);
            switch (mode) {
                case JSVG -> {
                }
                case SVG_SALAMANDER -> {
                    try {
                        icon.setSvgURI(iconUrl(iconName).toURI());
                    } catch (URISyntaxException e) {
                        throw new IllegalStateException(e);
                    }
                }
                case BATIK -> {
                    add(jsvgCanvas);
                    try {
                        jsvgCanvas.setURI(iconUrl(iconName).toURI().toASCIIString());
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            revalidate();
            repaint();
        }

        private static @NotNull URL iconUrl(@NotNull String name) {
            return Objects.requireNonNull(SVGViewer.class.getResource(name));
        }

        @Override
        public void doLayout() {
            super.doLayout();
            jsvgCanvas.setBounds(0, 0, getWidth(), getHeight());
        }

        private void setRenderingMode(@NotNull RenderingMode mode) {
            if (this.mode == mode) return;
            this.mode = mode;
            configureRenderer();
        }

        private void setViewName(@Nullable String viewName) {
            selectedView = viewName != null ? com.github.weisj.jsvg.view.View.named(viewName) : null;
            repaint();
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

        public void setRenderAtIntrinsicSize(boolean intrinsicSize) {
            this.intrinsicSize = intrinsicSize;
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
            ((Graphics2D) g).setRenderingHint(
                    SVGRenderingHints.KEY_SOFT_CLIPPING,
                    softClipping ? SVGRenderingHints.VALUE_SOFT_CLIPPING_ON
                            : SVGRenderingHints.VALUE_SOFT_CLIPPING_OFF);
            ((Graphics2D) g).setRenderingHint(SVGRenderingHints.KEY_MASK_CLIP_RENDERING,
                    maskRenderingValue);
            System.out.println("======");
            switch (mode) {
                case JSVG:
                    SVGDocument currentDocument = document;
                    if (currentDocument == null) return;
                    ViewBox viewport = new ViewBox(0, 0, getWidth(), getHeight());
                    Graphics2D renderGraphics = (Graphics2D) g.create();
                    BufferedImage img = null;
                    if (this.lowResolution) {
                        viewport = new ViewBox(currentDocument.size());
                        img = new BufferedImage((int) viewport.width, (int) viewport.height,
                                BufferedImage.TYPE_INT_ARGB);
                        Graphics2D imgGraphics = img.createGraphics();
                        imgGraphics.setRenderingHints(renderGraphics.getRenderingHints());
                        renderGraphics = imgGraphics;
                    }
                    if (paintShape) {
                        Shape shape = currentDocument.computeShape(viewport);
                        renderGraphics.setColor(Color.MAGENTA);
                        renderGraphics.fill(shape);
                    } else {
                        Output output = Output.createForGraphics(renderGraphics);
                        FloatSize floatSize = intrinsicSize
                                ? currentDocument.sizeForViewport(viewport)
                                : new FloatSize(getWidth(), getHeight());
                        ViewBox vb = new ViewBox(
                                viewport.width / 2 - floatSize.width / 2,
                                viewport.height / 2 - floatSize.height / 2,
                                floatSize.width,
                                floatSize.height);
                        currentDocument.render(output, RenderConfig.builder()
                                .platformSupport(new AwtComponentPlatformSupport(this))
                                .viewBox(vb)
                                .view(selectedView)
                                .animationState(animationPlayer.animationState())
                                .build());
                        output.dispose();
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
