[![Autostyle](https://github.com/weisJ/jsvg/actions/workflows/autostyle.yml/badge.svg)](https://github.com/weisJ/jsvg/actions/workflows/autostyle.yml)
[![CI](https://github.com/weisJ/jsvg/actions/workflows/gradle.yml/badge.svg)](https://github.com/weisJ/jsvg/actions/workflows/gradle.yml)
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

## How to use

There currently isn't any stable version of the library as the api is still evolving.
However, snapshot builds will be release to maven:
````kotlin
repositories {
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation("com.github.weisj:jsvg:latest.integration")
}
````
To load an svg icon you can use the [`SVGLoader`](https://github.com/weisJ/jsvg/blob/master/jsvg/src/main/java/com/github/weisj/jsvg/SVGLoader.java)
class. It will produce an [`SVGDocument`](https://github.com/weisJ/jsvg/blob/master/jsvg/src/main/java/com/github/weisj/jsvg/SVGDocument.java) which
can be rendered to any `Graphics2D` object you like (e.g. a `BufferedImage` or a swing component).

## Supported features

For supported elements most of the attributes which apply to them are implemented.

- :white_check_mark:: The element is supported
- (:white_check_mark:): The element is supported, but won't have any effect (e.g. it's currently not possible to query the content of a `<desc>` element)
- :x:: The element is currently not supported
- :warning:: The element is deprecated in the spec and has a low priority of getting implemented.

|Element|Status|
|-------|------|
|a|:white_check_mark:|
|:warning:altGlyph|:x:|
|:warning:altGlyphDef|:x:|
|:warning:altGlyphItem|:x:|
|animate|:x:|
|:warning:animateColor|:x:|
|animateMotion|:x:|
|animateTransform|:x:|
|circle|:white_check_mark:|
|clipPath|:white_check_mark:|
|color-profile|:x:|
|:warning:cursor|:x:|
|defs|:white_check_mark:|
|desc|(:white_check_mark:)|
|ellipse|:white_check_mark:|
|feBlend|:x:|
|feColorMatrix|:x:|
|feComponentTransfer|:x:|
|feComposite|:x:|
|feConvolveMatrix|:x:|
|feDiffuseLighting|:x:|
|feDisplacementMap|:x:|
|feDistantLight|:x:|
|feFlood|:x:|
|feFuncA|:x:|
|feFuncB|:x:|
|feFuncG|:x:|
|feFuncR|:x:|
|feGaussianBlur|:x:|
|feImage|:x:|
|feMerge|:x:|
|feMergeNode|:x:|
|feMorphology|:x:|
|feOffset|:x:|
|fePointLight|:x:|
|feSpecularLighting|:x:|
|feSpotLight|:x:|
|feTile|:x:|
|feTurbulence|:x:|
|filter|:x:|
|:warning:font|:x:|
|:warning:font-face|:x:|
|:warning:font-face-format|:x:|
|:warning:font-face-name|:x:|
|:warning:font-face-src|:x:|
|:warning:font-face-uri|:x:|
|foreignObject|:x:|
|g|:white_check_mark:|
|:warning:glyph|:x:|
|:warning:glyphRef|:x:|
|:warning:hkern|:x:|
|image|:white_check_mark:|
|line|:white_check_mark:|
|linearGradient|:white_check_mark:|
|marker|:white_check_mark:|
|mask|:x:|
|metadata|(:white_check_mark:)|
|:warning:missing-glyph|:x:|
|mpath|:x:|
|path|:white_check_mark:|
|pattern|:white_check_mark:|
|polygon|:white_check_mark:|
|polyline|:white_check_mark:|
|radialGradient|:white_check_mark:|
|rect|:white_check_mark:|
|script|:x:|
|set|:x:|
|stop|:white_check_mark:|
|style|:x:|
|svg|:white_check_mark:|
|switch|:x:|
|symbol|:white_check_mark:|
|text|:white_check_mark:|
|textPath|:white_check_mark:|
|title|(:white_check_mark:)
|:warning:tref|:x:|
|tspan|:white_check_mark:|
|use|:white_check_mark:|
|view|(:white_check_mark:)|
|:warning:vkern|:x:|
