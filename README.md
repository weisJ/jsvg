[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.weisj%3Ajsvg&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=com.github.weisj%3Ajsvg)
[![Code Style](https://github.com/weisJ/jsvg/actions/workflows/spotless.yml/badge.svg)](https://github.com/weisJ/jsvg/actions/workflows/spotless.yml)
[![CI](https://github.com/weisJ/jsvg/actions/workflows/gradle.yml/badge.svg)](https://github.com/weisJ/jsvg/actions/workflows/gradle.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.weisj/jsvg?label=Maven%20Central)](https://search.maven.org/artifact/com.github.weisj/jsvg)

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/weisj)

# JSVG - A Java SVG implementation

<p align="center">
    <img src="https://raw.githubusercontent.com/weisJ/jsvg/master/images/svg_logo.png"
         alt="The SVG logo rendered by JSVG"
         align="center" width="150" height="150">
    <br>
    <em>The SVG logo rendered using JSVG</em>
</p>

JSVG is an SVG user agent using AWT graphics. Its aim is to provide a small and fast implementation.
This library is under active development and doesn't yet support all features of the SVG specification, some of which
it decidedly won't support at all. This implementation only tries to be a static user agent meaning it won't support any
scripting languages or interaction. Animations aren't currently implemented but are planned to be supported.

This library aims to be as lightweight as possible. Generally JSVG uses ~50% less memory than svgSalamander and
~98% less than Batik.

## Projects using JSVG
* The [Jetbrains IDEA IDE](https://github.com/JetBrains/intellij-community) suite ([YouTrack Ticket](https://youtrack.jetbrains.com/issue/IJPL-81/Switch-to-JSVG-for-SVG-icon-rendering?reloaded=true)).
* [Apache NetBeans](https://netbeans.apache.org/front/main/index.html) ([PR #7941](https://github.com/apache/netbeans/pull/7941))
* [Eclipse SWT](https://github.com/eclipse-platform/eclipse.platform.swt) (Upcoming [PR #1638](https://github.com/eclipse-platform/eclipse.platform.swt/pull/1638))
* [FlatLaf](https://github.com/JFormDesigner/FlatLaf) for [FlatSVGIcon](https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-extras) ([PR #684](https://github.com/JFormDesigner/FlatLaf/pull/684))

## How to use

The library is available on maven central:

````kotlin
dependencies {
    implementation("com.github.weisj:jsvg:1.7.0")
}
````

Also, nightly snapshot builds will be released to maven:

````kotlin
repositories {
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

// Optional:
configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation("com.github.weisj:jsvg:latest.integration")
}
````
JSVG provides OSGi metadata in the manifest file.

### Loading

To load an svg icon you can use
the [`SVGLoader`](https://github.com/weisJ/jsvg/blob/master/jsvg/src/main/java/com/github/weisj/jsvg/parser/SVGLoader.java)
class. It will produce
an [`SVGDocument`](https://github.com/weisJ/jsvg/blob/master/jsvg/src/main/java/com/github/weisj/jsvg/SVGDocument.java)

````java
SVGLoader loader = new SVGLoader();
URL svgUrl = MyClass.class.getResource("mySvgFile.svg");
SVGDocument svgDocument = loader.load(svgUrl);
````

If you need more control over the loading process you can pass a `LoaderContext` for configuration purposes.

````java
SVGDocument svgDocument = loader.load(svgUrl,
    LoaderContext.builder()
                 // configure the context
                 // ...
                 .build());
````

Note that `SVGLoader` is not guaranteed to be thread safe, hence shouldn't be used across multiple threads.

### Rendering

An `SVGDocument` can be rendered to any `Graphics2D` object you like e.g. a `BufferedImage`

````java
FloatSize size = svgDocument.size();
BufferedImage image = new BufferedImage((int) size.width,(int) size.height);
Graphics2D g = image.createGraphics();
svgDocument.render(null,g);
g.dispose();
````

or a swing component

````java
class MyComponent extends JComponent {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        svgDocument.render(this, (Graphics2D) g, new ViewBox(0, 0, getWidth(), getHeight()));
    }
}
````

For more in-depth examples see #Usage examples below.

#### Rendering Quality

The rendering quality can be adjusted by setting the `RenderingHints` of the `Graphics2D` object. The following
properties are recommended:

````java
g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
````

If either of these values are not set or have their respective default values (`VALUE_ANTIALIAS_DEFAULT` and `VALUE_STROKE_DEFAULT`)
JSVG will automatically set them to the recommended values above.

JSVG also supports custom SVG specific rendering hints. These can be set using the `SVGRenderingHints` class. For example:

````java
// Will use the value of RenderingHints.KEY_ANTIALIASING by default
g.setRenderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING, SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_ON);
````

By default clipping with a `<clipPath>` element does not use soft-clipping (i.e. anti-aliasing along the edges of the clip shape).
This can be enabled by setting

````java
g.setRenderingHint(SVGRenderingHints.KEY_SOFT_CLIPPING, SVGRenderingHints.VALUE_SOFT_CLIPPING_ON);
````

In the future this will get stabilized and be enabled by default.

Supported custom rendering hints are:

| Key                         | Values                                                                                                          | Default                                                              | Description                                                                                                                                                                   |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `KEY_IMAGE_ANTIALIASING`    | `VALUE_IMAGE_ANTIALIAS_ON`<br>`VALUE_IMAGE_ANTIALIAS_OFF`                                                       | Value of `RenderingHints.KEY_ANTIALIASING`                           | Enables anti-aliasing for images                                                                                                                                              |
| `KEY_SOFT_CLIPPING`         | `VALUE_SOFT_CLIPPING_ON`<br>`VALUE_SOFT_CLIPPING_OFF`                                                           | `VALUE_SOFT_CLIPPING_OFF`                                            | Enables soft (anti-aliased) clipping for `clipPath`                                                                                                                           |
| `KEY_MASK_CLIP_RENDERING`   | `VALUE_MASK_CLIP_RENDERING_FAST`<br>`VALUE_MASK_CLIP_RENDERING_ACCURACY`<br>`VALUE_MASK_CLIP_RENDERING_DEFAULT` | `VALUE_MASK_CLIP_RENDERING_DEFAULT = VALUE_MASK_CLIP_RENDERING_FAST` | Changes how masks and clip paths are rendered. Accurate rendering enforces the sub-image to which the mask/clip is applied to be rendered on its own isolated offscreen image |
| `KEY_CACHE_OFFSCREEN_IMAGE` | `VALUE_USE_CACHE`<br>`VALUE_NO_CACHE`                                                                           | `VALUE_USE_CACHE`                                                    | Whether to cache offscreen images. This can be useful for performance reasons, but can also lead to increased memory usage.                                                   |

All are exposed through the `SVGRenderingHints`class.

### Animations

The current support for animations is limited and in an experimental state.
Only basic timing mechanisms and interpolation methods are supported.
Moreover most animatable properties aren't yet supported.
Please beware that the API for animations is subject to change.

Animations can be controlled on a per frame basis by supplying an `AnimationState` to `SVGDocument#renderWithPlatform`.
In particular this means that animations need to be driven by the user code.
See below for examples on how to do this.


## Supported features

For supported elements most of the attributes which apply to them are implemented.

- :white_check_mark:: The element is supported. Note that this doesn't mean that every attribute is supported.
- :white_check_mark:*: The element is supported, but won't have any effect (e.g. it's currently not possible to query
  the content of a `<desc>` element)
- :ballot_box_with_check:: The element is partially implemented and might not support most basic features of the
  element.
- :x:: The element is currently not supported
- :warning:: The element is deprecated in the spec and has a low priority of getting implemented.
- :test_tube:: The element is an experimental part of the svg 2.* spec. It may not fully behave as expected.

### Shape and container elements

| Element       | Status              |
|---------------|---------------------|
| a             | :white_check_mark:  |
| circle        | :white_check_mark:  |
| clipPath      | :white_check_mark:  |
| defs          | :white_check_mark:  |
| ellipse       | :white_check_mark:  |
| foreignObject | :x:                 |
| g             | :white_check_mark:  |
| image         | :white_check_mark:  |
| line          | :white_check_mark:  |
| marker        | :white_check_mark:  |
| mask          | :white_check_mark:  |
| path          | :white_check_mark:  |
| polygon       | :white_check_mark:  |
| polyline      | :white_check_mark:  |
| rect          | :white_check_mark:  |
| svg           | :white_check_mark:  |
| symbol        | :white_check_mark:  |
| use           | :white_check_mark:  |
| view          | :white_check_mark:* |

### Paint server elements

| Element                 | Status             |
|-------------------------|--------------------|
| linearGradient          | :white_check_mark: |
| :test_tube:meshgradient | :white_check_mark: |
| :test_tube:meshrow      | :white_check_mark: |
| :test_tube:meshpatch    | :white_check_mark: |
| pattern                 | :white_check_mark: |
| radialGradient          | :white_check_mark: |
| solidColor              | :white_check_mark: |
| stop                    | :white_check_mark: |

### Text elements

| Element       | Status             |
|---------------|--------------------|
| text          | :white_check_mark: |
| textPath      | :white_check_mark: |
| :warning:tref | :x:                |
| tspan         | :white_check_mark: |

### Animation elements

| Element               | Status                  |
|-----------------------|-------------------------|
| animate               | :ballot_box_with_check: |
| :warning:animateColor | :x:                     |
| animateMotion         | :x:                     |
| animateTransform      | :ballot_box_with_check: |
| mpath                 | :x:                     |
| set                   | :x:                     |
| switch                | :x:                     |

### Filter elements

| Element             | Status                  |
|---------------------|-------------------------|
| feBlend             | :white_check_mark:      |
| feColorMatrix       | :white_check_mark:      |
| feComponentTransfer | :white_check_mark:      |
| feComposite         | :white_check_mark:      |
| feConvolveMatrix    | :x:                     |
| feDiffuseLighting   | :white_check_mark:      |
| feDisplacementMap   | :white_check_mark:      |
| feDistantLight      | :x:                     |
| feDropShadow        | :white_check_mark:      |
| feFlood             | :white_check_mark:      |
| feFuncA             | :white_check_mark:      |
| feFuncB             | :white_check_mark:      |
| feFuncG             | :white_check_mark:      |
| feFuncR             | :white_check_mark:      |
| feGaussianBlur      | :white_check_mark:      |
| feImage             | :x:                     |
| feMerge             | :white_check_mark:      |
| feMergeNode         | :white_check_mark:      |
| feMorphology        | :x:                     |
| feOffset            | :white_check_mark:      |
| fePointLight        | :x:                     |
| feSpecularLighting  | :x:                     |
| feSpotLight         | :x:                     |
| feTile              | :x:                     |
| feTurbulence        | :white_check_mark:      |
| filter              | :ballot_box_with_check: |

### Font elements

| Element                   | Status |
|---------------------------|--------|
| :warning:altGlyph         | :x:    |
| :warning:altGlyphDef      | :x:    |
| :warning:altGlyphItem     | :x:    |
| :warning:font             | :x:    |
| :warning:font-face        | :x:    |
| :warning:font-face-format | :x:    |
| :warning:font-face-name   | :x:    |
| :warning:font-face-src    | :x:    |
| :warning:font-face-uri    | :x:    |
| :warning:glyph            | :x:    |
| :warning:glyphRef         | :x:    |
| :warning:hkern            | :x:    |
| :warning:missing-glyph    | :x:    |
| :warning:vkern            | :x:    |

### Other elements

| Element         | Status                  |
|-----------------|-------------------------|
| desc            | (:white_check_mark:)    |
| title           | (:white_check_mark:)    |
| metadata        | (:white_check_mark:)    |
| color-profile   | :x:                     |
| :warning:cursor | :x:                     |
| script          | :x:                     |
| style           | :ballot_box_with_check: |


## Usage examples

### Basic
To render an SVG to a swing component you can start from the following example:

````java
import javax.swing.*;
import java.awt.*;
import java.net.URL;

import com.github.weisj.jsvg.*;
import com.github.weisj.jsvg.attributes.*;
import com.github.weisj.jsvg.parser.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RenderExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SVGLoader loader = new SVGLoader();

            URL svgUrl = RenderExample.class.getResource("path/to/image.svg");
            SVGDocument document = loader.load(Objects.requireNonNull(svgUrl, "SVG file not found"));

            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 400));
            frame.setContentPane(new SVGPanel(Objects.requireNonNull(document)));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static class SVGPanel extends JPanel {
        private @NotNull final SVGDocument document;

        SVGPanel(@NotNull SVGDocument document) {
            this.document = document;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ((Graphics2D) g).setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D) g).setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

            document.render(this, (Graphics2D) g, new ViewBox(0, 0, getWidth(), getHeight()));
        }
    }
}
````

### DOM manipulation
You can even change the color of svg elements by using a suitable `DomProcessor` together with a custom implementation
of `SVGPaint`. Lets take the following SVG as an example:

````svg
<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100">
    <rect x="0" y="0" width="100%" height="40%" id="myRect"></rect>
    <rect x="0" y="60" width="100%" height="40%"></rect>
</svg>
````

We want to change the color if the first rectangle at runtime. We start by loading the SVG using a custom `ParserProvider`
which returns a `DomProcessor`for the pre-processing step. The `DomProcessor` will allow us to change attributes
of the SVG elements before they are fully parsed.

````java
CustomColorsProcessor processor = new CustomColorsProcessor(List.of("myRect"));
document = loader.load(svgUrl, new DefaultParserProvider() {
    @Override
    public DomProcessor createPreProcessor() {
        return processor;
    }
});
````

The heavy lifting is done by the `CustomColorsProcessor` class which looks like this:

````java
class CustomColorsProcessor implements DomProcessor {

    private final Map<String, DynamicAWTSvgPaint> customColors = new HashMap<>();

    public CustomColorsProcessor(@NotNull List<String> elementIds) {
        for (String elementId : elementIds) {
            customColors.put(elementId, new DynamicAWTSvgPaint(Color.BLACK));
        }
    }

    @Nullable DynamicAWTSvgPaint customColorForId(@NotNull String id) {
        return customColors.get(id);
    }

    @Override
    public void process(@NotNull ParsedElement root) {
        processImpl(root);
        root.children().forEach(this::process);
    }

    private void processImpl(ParsedElement element) {
        // Obtain the id of the element
        // Note: There that Element also has a node() method to obtain the SVGNode. However during the pre-processing
        // phase the SVGNode is not yet fully parsed and doesn't contain any non-defaulted information.
        String nodeId = element.id();

        // Check if this element is one of the elements we want to change the color of
        if (customColors.containsKey(nodeId)) {
            // The attribute node contains all the attributes of the element specified in the markup
            // Even those which aren't valid for the element
            AttributeNode attributeNode = element.attributeNode();
            DynamicAWTSvgPaint dynamicColor = customColors.get(nodeId);

            // This assumed that the fill attribute is a color and not a gradient or pattern.
            Color color = attributeNode.getColor("fill");

            dynamicColor.setColor(color);

            // This can be anything as long as it's unique
            String uniqueIdForDynamicColor = UUID.randomUUID().toString();

            // Register the dynamic color as a custom element
            element.registerNamedElement(uniqueIdForDynamicColor, dynamicColor);

            // Refer to the custom element as the fill attribute
            attributeNode.attributes().put("fill", uniqueIdForDynamicColor);

            // Note: This class can easily be adapted to also support changing the stroke color.
            // With a bit more work it could also support changing the color of gradients and patterns.
        }
    }
}

class DynamicAWTSvgPaint implements SimplePaintSVGPaint {

    private @NotNull Color color;

    DynamicAWTSvgPaint(@NotNull Color color) {
        this.color = color;
    }

    public void setColor(@NotNull Color color) {
        this.color = color;
    }

    public @NotNull Color color() {
        return color;
    }

    @Override
    public @NotNull Paint paint() {
        return color;
    }
}
````

Now we simply have to obtain the `DynamicAWTSvgPaint` instance for the element we want to change the color of and
hook it up in our UI:

````java
DynamicAWTSvgPaint dynamicColor = processor.customColorForId("myRect");

SVGPanel panel = new SVGPanel(document);
JButton button = new JButton("Change color");
button.addActionListener(e -> {
    Color newColor = JColorChooser.showDialog(panel, "Choose a color", dynamicColor.color());
    if (newColor != null) {
        dynamicColor.setColor(newColor);
        // Make sure to repaint the panel to see the changes
        panel.repaint();
    }
});
JPanel content = new JPanel(new BorderLayout());
content.add(panel, BorderLayout.CENTER);
content.add(button, BorderLayout.SOUTH);
frame.setContentPane(content);
````
### Animations

JSVG provides a helper class `AnimationPlayer` for implementing animations in Swing components.
The following example demonstrates how to use the `AnimationPlayer` to animate an SVG element:

````java
import javax.swing.*;

public class AnimationPanel extends JComponent {
    private final @NotNull SVGDocument document;
    private final @NotNull AnimationPlayer player;

    public AnimationPanel(@NotNull SVGDocument document) {
        this.document = document;
        this.player = new AnimationPlayer(e -> repaint());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Setup rendering hints (see above)
        // ...
        document.renderWithPlatform(
            new AwtComponentPlatformSupport(this),
            new Graphics2DOutput((Graphics2D) g),
            new ViewBox(0, 0, getWidth(), getHeight()),
            animationPlayer.animationState());
    }

    public void startAnimation() {
        player.start();
    }

    public void stopAnimation() {
        player.stop();
    }
}
````
