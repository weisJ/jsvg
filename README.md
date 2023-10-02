[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.weisj%3Ajsvg&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=com.github.weisj%3Ajsvg)
[![Code Style](https://github.com/weisJ/jsvg/actions/workflows/spotless.yml/badge.svg)](https://github.com/weisJ/jsvg/actions/workflows/spotless.yml)
[![CI](https://github.com/weisJ/jsvg/actions/workflows/gradle.yml/badge.svg)](https://github.com/weisJ/jsvg/actions/workflows/gradle.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.weisj/jsvg?label=Maven%20Central)](https://search.maven.org/artifact/com.github.weisj/jsvg)

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

JSVG is used by the [Jetbrains IDEA IDE](https://github.com/JetBrains/intellij-community) suite for rendering their interface icons.

## How to use

The library is available on maven central:

````kotlin
dependencies {
    implementation("com.github.weisj:jsvg:1.2.0")
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

Note that `SVGLoader` is not guaranteed to be thread safe hence shouldn't be used across multiple threads.

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

## Supported features

For supported elements most of the attributes which apply to them are implemented.

- :white_check_mark:: The element is supported. Note that this doesn't mean that every attribute is supported.
- (:white_check_mark:): The element is supported, but won't have any effect (e.g. it's currently not possible to query
  the content of a `<desc>` element)
- :ballot_box_with_check:: The element is partially implemented and might not support most basic features of the
  element.
- :x:: The element is currently not supported
- :warning:: The element is deprecated in the spec and has a low priority of getting implemented.
- :test_tube:: The element is an experimental part of the svg 2.* spec. It may not fully behave as expected.

## Shape and container elements

| Element       | Status               |
|---------------|----------------------|
| a             | :white_check_mark:   |
| circle        | :white_check_mark:   |
| clipPath      | :white_check_mark:   |
| defs          | :white_check_mark:   |
| ellipse       | :white_check_mark:   |
| foreignObject | :x:                  |
| g             | :white_check_mark:   |
| image         | :white_check_mark:   |
| line          | :white_check_mark:   |
| marker        | :white_check_mark:   |
| mask          | :white_check_mark:   |
| path          | :white_check_mark:   |
| polygon       | :white_check_mark:   |
| polyline      | :white_check_mark:   |
| rect          | :white_check_mark:   |
| svg           | :white_check_mark:   |
| symbol        | :white_check_mark:   |
| use           | :white_check_mark:   |
| view          | (:white_check_mark:) |

## Paint server elements

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

## Text elements

| Element       | Status             |
|---------------|--------------------|
| text          | :white_check_mark: |
| textPath      | :white_check_mark: |
| :warning:tref | :x:                |
| tspan         | :white_check_mark: |

## Animation elements

| Element               | Status |
|-----------------------|--------|
| animate               | :x:    |
| :warning:animateColor | :x:    |
| animateMotion         | :x:    |
| animateTransform      | :x:    |
| mpath                 | :x:    |
| set                   | :x:    |
| switch                | :x:    |

## Filter elements

| Element             | Status                  |
|---------------------|-------------------------|
| feBlend             | :white_check_mark:      |
| feColorMatrix       | :white_check_mark:      |
| feComponentTransfer | :x:                     |
| feComposite         | :white_check_mark:      |
| feConvolveMatrix    | :x:                     |
| feDiffuseLighting   | :white_check_mark:      |
| feDisplacementMap   | :white_check_mark:      |
| feDistantLight      | :x:                     |
| feFlood             | :white_check_mark:      |
| feFuncA             | :x:                     |
| feFuncB             | :x:                     |
| feFuncG             | :x:                     |
| feFuncR             | :x:                     |
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

## Other elements

| Element         | Status                  |
|-----------------|-------------------------|
| desc            | (:white_check_mark:)    |
| title           | (:white_check_mark:)    |
| metadata        | (:white_check_mark:)    |
| color-profile   | :x:                     |
| :warning:cursor | :x:                     |
| script          | :x:                     |
| style           | :ballot_box_with_check: |
