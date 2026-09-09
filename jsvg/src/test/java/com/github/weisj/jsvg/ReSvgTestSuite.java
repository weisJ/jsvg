/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Jannis Weis
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

import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ImageSource.*;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.attributes.font.FontResolver;
import com.github.weisj.jsvg.renderer.PlatformSupport;

class ReSvgTestSuite {

    private static final Logger LOGGER = Logging.getLogger(ReSvgTestSuite.class);
    private static final String RESVG_TEST_SUITE_PATH = System.getenv("RESVG_TEST_SUITE_PATH");

    // JSVG render type carrying the fonts bundled with the suite (set up in @BeforeAll).
    private static @NotNull RenderType jsvgRenderType = RenderType.JSVG;

    static Collection<DynamicTest> checkDirectory(@NotNull String name) {
        return checkDirectory(name, Collections.emptySet());
    }

    static Collection<DynamicTest> checkDirectory(@NotNull String name, Collection<String> exclude) {
        Path basePath = Path.of(RESVG_TEST_SUITE_PATH);
        Path tests = basePath.resolve(name);
        try (var files = Files.walk(tests)) {
            return files
                    .filter(p -> p.toString().endsWith(".svg"))
                    .filter(p -> !exclude.contains(p.getFileName().toString()))
                    .map(p -> {
                        String testName = basePath.relativize(p).toString();
                        return DynamicTest.dynamicTest(testName, new ReSVGRefTest(p));
                    })
                    .toList();
        } catch (IOException e) {
            Assertions.fail(e);
        }
        return Collections.emptyList();
    }

    @BeforeAll
    static void checkForReSVGRepositoryAndRegisterFonts() {
        var exists = Path.of(RESVG_TEST_SUITE_PATH).toFile().exists();
        var message = """
                The resvg submodule was not found. Skipping ReSVG test suite.
                Please run `git submodule update --init --recursive` to fetch the submodule.
                """.stripIndent();
        if (!exists) {
            LOGGER.warn(message);
        }
        assumeTrue(exists, message);

        jsvgRenderType = new RenderType.JSVGType(RenderType.JSVG.loaderContext(), loadBundledFonts());
        // Drop any fallback fonts a prior test cached for these families in the shared JVM.
        FontResolver.clearFontCache();
    }

    private static @NotNull PlatformSupport loadBundledFonts() {
        Map<String, Font> fonts = new HashMap<>();
        Path fontDir = Path.of(RESVG_TEST_SUITE_PATH).getParent().resolve("fonts");
        try (var files = Files.walk(fontDir)) {
            files.filter(p -> p.toString().endsWith(".ttf")).forEach(p -> {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, p.toFile());
                    // FontResolver looks up the CSS-canonicalized (lower-cased) family name.
                    String family = font.getFamily().toLowerCase(Locale.US);
                    // A family may span multiple files; prefer the regular variant.
                    if (p.getFileName().toString().contains("Regular") || !fonts.containsKey(family)) {
                        fonts.put(family, font);
                    }
                } catch (IOException | FontFormatException e) {
                    LOGGER.warn("Failed to load font " + p, e);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to walk font directory " + fontDir, e);
        }
        return new BundledFontSupport(fonts);
    }

    private record BundledFontSupport(@NotNull Map<String, Font> fonts) implements PlatformSupport {
        @Override
        public @Nullable ImageObserver imageObserver() {
            return null;
        }

        @Override
        public @Nullable TargetSurface targetSurface() {
            return null;
        }

        @Override
        public @Nullable Font customFont(@NotNull String family) {
            return fonts.get(family.toLowerCase(Locale.US));
        }
    }

    @TestFactory
    Collection<DynamicTest> filters_feBlend() {
        return checkDirectory("filters/feBlend", Set.of(
                "mode=color-burn.svg",
                "mode=hue.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feColorMatrix() {
        return checkDirectory("filters/feColorMatrix", Set.of(
                "invalid-type.svg",
                "type=matrix-with-non-normalized-values.svg",
                "type=matrix.svg",
                "type=saturate-with-a-large-coefficient.svg",
                "type=saturate-with-negative-coefficient.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feComponentTransfer() {
        return checkDirectory("filters/feComponentTransfer", Set.of(
                "mixed-types.svg",
                "type=table-and-tableValues=100--100.svg",
                "type=table-and-tableValues=1px.svg",
                "type=table-on-alpha.svg",
                "type=table-with-large-values.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feComposite() {
        return checkDirectory("filters/feComposite");
    }

    @TestFactory
    Collection<DynamicTest> filters_feDiffuseLighting() {
        return checkDirectory("filters/feDiffuseLighting", Set.of(
                "complex-transform.svg",
                "diffuseConstant=-1.svg",
                "lighting-color=currentColor-without-color.svg",
                "lighting-color=currentColor.svg",
                "lighting-color=hsla.svg",
                "lighting-color=inherit.svg",
                "linearRGB-color-interpolation.svg",
                "multiple-light-sources.svg",
                "single-light-source-with-comment.svg",
                "single-light-source-with-desc.svg",
                "single-light-source-with-invalid-child.svg",
                "single-light-source-with-title-and-desc.svg",
                "single-light-source-with-title.svg",
                "single-light-source.svg",
                "surfaceScale=-10.svg",
                "surfaceScale=0.svg",
                "surfaceScale=1.33.svg",
                "surfaceScale=5.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feDisplacementMap() {
        return checkDirectory("filters/feDisplacementMap");
    }

    @TestFactory
    Collection<DynamicTest> filters_feDistantLight() {
        return checkDirectory("filters/feDistantLight");
    }

    @TestFactory
    Collection<DynamicTest> filters_feDropShadow() {
        return checkDirectory("filters/feDropShadow", Set.of(
                "hsla-color.svg",
                "with-flood-color.svg",
                "with-flood-opacity.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feFlood() {
        return checkDirectory("filters/feFlood", Set.of(
                "with-opacity-on-target-element.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feGaussianBlur() {
        return checkDirectory("filters/feGaussianBlur", Set.of(
                "complex-transform.svg",
                "huge-stdDeviation.svg",
                "simple-case.svg",
                "small-stdDeviation.svg",
                "stdDeviation-with-multiple-values.svg",
                "stdDeviation-with-two-different-values.svg",
                "stdDeviation-with-two-values.svg",
                "stdDeviation=0-5.svg",
                "stdDeviation=5-0.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feMerge() {
        return checkDirectory("filters/feMerge", Set.of(
                "complex-transform.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feMorphology() {
        return checkDirectory("filters/feMorphology", Set.of(
                "huge-radius.svg",
                "operator=dilate.svg",
                "radius=0.5-with-objectBoundingBox.svg",
                "radius=1-10.svg",
                "radius=10-0.svg",
                "radius=10-1.svg",
                "simple-case.svg",
                "source-with-opacity.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feOffset() {
        return checkDirectory("filters/feOffset");
    }

    @TestFactory
    Collection<DynamicTest> filters_feSpotLight() {
        return checkDirectory("filters/feSpotLight", Set.of(
                "complex-transform.svg",
                "custom-attributes.svg",
                "limitingConeAngle-anti-aliasing.svg",
                "limitingConeAngle=-30.svg",
                "limitingConeAngle=30.svg",
                "primitiveUnits=objectBoundingBox.svg",
                "specularExponent=-10.svg",
                "specularExponent=0.5.svg",
                "specularExponent=10.svg",
                "with-all-pointsAt.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feTile() {
        return checkDirectory("filters/feTile", Set.of(
                "complex-transform.svg",
                "simple-case.svg",
                "with-region.svg",
                "with-subregion-1.svg",
                "with-subregion-2.svg",
                "with-subregion-3.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_feTurbulence() {
        return checkDirectory("filters/feTurbulence", Set.of(
                "baseFrequency=-0.05.svg",
                "baseFrequency=0.01.svg",
                "baseFrequency=0.05--0.01.svg",
                "baseFrequency=0.05-0.01.svg",
                "baseFrequency=0.05-0.05.svg",
                "baseFrequency=0.05-0.svg",
                "color-interpolation-filters=sRGB.svg",
                "complex-transform.svg",
                "no-attributes.svg",
                "numOctaves=-1.svg",
                "numOctaves=0.svg",
                "numOctaves=5.svg",
                "primitiveUnits=objectBoundingBox.svg",
                "seed=-20.svg",
                "seed=1.5.svg",
                "seed=20.svg",
                "stitchTiles=stitch.svg",
                "type=invalid.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_filter() {
        return checkDirectory("filters/filter", Set.of(
                "color-interpolation-filters=sRGB.svg",
                "complex-order-and-xlink-href.svg",
                "content-outside-the-canvas.svg",
                "default-color-interpolation-filters.svg",
                "everything-via-xlink-href.svg",
                "global-transform.svg",
                "huge-region.svg",
                "in-to-invalid-1.svg",
                "in=BackgroundAlpha-with-enable-background.svg",
                "in=BackgroundAlpha.svg",
                "in=BackgroundImage-with-enable-background.svg",
                "in=BackgroundImage.svg",
                "in=FillPaint-on-g-without-children.svg",
                "in=FillPaint-with-gradient.svg",
                "in=FillPaint-with-pattern.svg",
                "in=FillPaint-with-target-on-g.svg",
                "in=FillPaint.svg",
                "in=StrokePaint.svg",
                "initial-transform.svg",
                "invalid-FuncIRI.svg",
                "invalid-filterUnits.svg",
                "invalid-primitive-1.svg",
                "invalid-xlink-href.svg",
                "multiple-primitives-1.svg",
                "multiple-primitives-2.svg",
                "multiple-primitives-3.svg",
                "multiple-primitives-4.svg",
                "negative-subregion.svg",
                "no-children.svg",
                "on-group-with-child-outside-of-canvas.svg",
                "on-the-root-svg.svg",
                "primitiveUnits=objectBoundingBox.svg",
                "recursive-xlink-href.svg",
                "region-with-stroke.svg",
                "self-recursive-xlink-href.svg",
                "simple-case.svg",
                "some-attributes-via-xlink-href.svg",
                "subregion-and-primitiveUnits=objectBoundingBox-1.svg",
                "subregion-and-primitiveUnits=objectBoundingBox-2.svg",
                "transform-on-filter.svg",
                "transform-on-shape-with-filter-region.svg",
                "transform-on-shape.svg",
                "unresolved-xlink-href.svg",
                "with-clip-path-and-mask.svg",
                "with-clip-path.svg",
                "with-mask-on-parent.svg",
                "with-mask.svg",
                "with-multiple-transforms-1.svg",
                "with-multiple-transforms-2.svg",
                "with-region-and-filterUnits=userSpaceOnUse.svg",
                "with-region-and-subregion.svg",
                "with-region-outside-the-canvas.svg",
                "with-subregion-1.svg",
                "with-subregion-2.svg",
                "with-subregion-3.svg",
                "with-transform-outside-of-canvas.svg",
                "without-region-and-filterUnits=userSpaceOnUse.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_filter_functions() {
        return checkDirectory("filters/filter-functions", Set.of(
                "blur-function-mm-value.svg",
                "blur-function.svg",
                "color-adjust-functions-0percent.svg",
                "color-adjust-functions-100percent.svg",
                "color-adjust-functions-2.svg",
                "color-adjust-functions-200percent.svg",
                "color-adjust-functions-50percent.svg",
                "color-adjust-functions-default-value.svg",
                "drop-shadow-function-color-as-attribute.svg",
                "drop-shadow-function-color-last.svg",
                "drop-shadow-function-currentColor.svg",
                "drop-shadow-function-em-values.svg",
                "drop-shadow-function-filter-region.svg",
                "drop-shadow-function-mm-values.svg",
                "drop-shadow-function-no-color.svg",
                "drop-shadow-function-only-offset.svg",
                "drop-shadow-function.svg",
                "grayscale-and-opacity.svg",
                "hue-rotate-function-0.25turn.svg",
                "hue-rotate-function-45deg.svg",
                "hue-rotate-function-45grad.svg",
                "hue-rotate-function-45rad.svg",
                "hue-rotate-function-999deg.svg",
                "nested-filters.svg",
                "one-invalid-url-in-list.svg",
                "two-exact-urls.svg",
                "two-urls.svg",
                "url-and-grayscale.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_flood_color() {
        return checkDirectory("filters/flood-color", Set.of(
                "hsla-color.svg",
                "inheritance-3.svg"));
    }

    @TestFactory
    Collection<DynamicTest> filters_flood_opacity() {
        return checkDirectory("filters/flood-opacity");
    }

    @TestFactory
    Collection<DynamicTest> masking_clipPath() {
        return checkDirectory("masking/clipPath", Set.of(
                "circle-shorthand-with-stroke-box.svg",
                "circle-shorthand-with-view-box.svg",
                "circle-shorthand.svg",
                "clip-path-on-child-with-transform.svg",
                "clip-path-on-child.svg",
                "clip-path-on-children.svg",
                "clip-path-on-self-2.svg",
                "clip-path-on-self.svg",
                "clip-path-with-transform-on-text.svg",
                "clip-rule-from-parent-node.svg",
                "clip-rule=evenodd.svg",
                "clipping-with-complex-text-1.svg",
                "clipping-with-complex-text-2.svg",
                "clipping-with-complex-text-and-clip-rule.svg",
                "clipping-with-text.svg",
                "g-is-not-a-valid-child.svg",
                "invalid-clip-path-on-child.svg",
                "invalid-clip-path-on-self.svg",
                "invalid-transform-on-clipPath.svg",
                "invisible-child-1.svg",
                "invisible-child-2.svg",
                "mixed-clip-rule.svg",
                "multiple-children.svg",
                "nested-clip-path.svg",
                "on-the-root-svg-without-size.svg",
                "recursive-on-child.svg",
                "recursive-on-self.svg",
                "self-recursive.svg",
                "transform-on-clipPath.svg"));
    }

    @TestFactory
    Collection<DynamicTest> masking_mask() {
        return checkDirectory("masking/mask", Set.of(
                "color-interpolation=linearRGB.svg",
                "half-width-region-with-rotation.svg",
                "mask-on-child.svg",
                "mask-on-self-with-mixed-mask-type.svg",
                "mask-type=invalid.svg",
                "mask-type=luminance.svg",
                "maskUnits=userSpaceOnUse-without-rect.svg",
                "on-a-horizontal-line.svg",
                "recursive-on-child.svg",
                "recursive-on-self.svg",
                "recursive.svg",
                "self-recursive.svg",
                "simple-case.svg",
                "transform-has-no-effect.svg",
                "with-opacity-1.svg",
                "with-opacity-3.svg"));
    }

    @TestFactory
    Collection<DynamicTest> paint_servers_linearGradient() {
        return checkDirectory("paint-servers/linearGradient", Set.of(
                "attributes-via-xlink-href-complex-order.svg",
                "attributes-via-xlink-href-from-radialGradient.svg",
                "attributes-via-xlink-href-from-rect.svg",
                "attributes-via-xlink-href-only-required.svg",
                "attributes-via-xlink-href.svg",
                "default-attributes.svg",
                "gradientTransform-and-transform.svg",
                "gradientTransform.svg",
                "gradientUnits=objectBoundingBox-with-percent.svg",
                "gradientUnits=userSpaceOnUse-with-percent.svg",
                "gradientUnits=userSpaceOnUse.svg",
                "hsla-color.svg",
                "invalid-child-1.svg",
                "invalid-child-2.svg",
                "invalid-gradientTransform.svg",
                "invalid-gradientUnits.svg",
                "invalid-spreadMethod.svg",
                "invalid-xlink-href.svg",
                "no-stops.svg",
                "recursive-xlink-href-1.svg",
                "recursive-xlink-href-2.svg",
                "recursive-xlink-href-3.svg",
                "self-recursive-xlink-href.svg",
                "spreadMethod=pad.svg",
                "spreadMethod=reflect.svg",
                "spreadMethod=repeat.svg",
                "stops-via-xlink-href-complex-order-1.svg",
                "stops-via-xlink-href-complex-order-2.svg",
                "stops-via-xlink-href-from-radialGradient.svg",
                "stops-via-xlink-href-from-rect.svg",
                "stops-via-xlink-href.svg",
                "unresolved-xlink-href.svg"));
    }

    @TestFactory
    Collection<DynamicTest> paint_servers_pattern() {
        return checkDirectory("paint-servers/pattern", Set.of(
                "child-with-invalid-FuncIRI.svg",
                "children-via-xlink-href.svg",
                "everything-via-xlink-href.svg",
                "invalid-patternTransform.svg",
                "invalid-patternUnits-and-patternContentUnits.svg",
                "nested-objectBoundingBox.svg",
                "out-of-order-referencing.svg",
                "overflow=visible.svg",
                "pattern-on-child.svg",
                "patternContentUnits-with-viewBox.svg",
                "patternContentUnits=objectBoundingBox.svg",
                "patternUnits=objectBoundingBox-with-percent.svg",
                "patternUnits=objectBoundingBox.svg",
                "patternUnits=userSpaceOnUse-with-percent.svg",
                "recursive-on-child.svg",
                "self-recursive-on-child.svg",
                "self-recursive.svg",
                "text-child.svg",
                "tiny-pattern-upscaled.svg",
                "transform-and-patternTransform.svg",
                "with-patternTransform.svg",
                "with-x-and-y.svg"));
    }

    @TestFactory
    Collection<DynamicTest> paint_servers_radialGradient() {
        return checkDirectory("paint-servers/radialGradient", Set.of(
                "attributes-via-xlink-href-complex-order.svg",
                "attributes-via-xlink-href-from-linearGradient.svg",
                "attributes-via-xlink-href-from-rect.svg",
                "attributes-via-xlink-href-only-required.svg",
                "attributes-via-xlink-href.svg",
                "default-attributes.svg",
                "focal-point-correction.svg",
                "fr=-1.svg",
                "fr=0.2.svg",
                "fr=0.5.svg",
                "fr=0.7.svg",
                "fx-resolving-1.svg",
                "fx-resolving-2.svg",
                "fx-resolving-3.svg",
                "fy-resolving-1.svg",
                "fy-resolving-2.svg",
                "fy-resolving-3.svg",
                "gradientTransform-and-transform.svg",
                "gradientTransform.svg",
                "gradientUnits=objectBoundingBox-with-percent.svg",
                "gradientUnits=userSpaceOnUse-with-percent.svg",
                "gradientUnits=userSpaceOnUse.svg",
                "hsla-color.svg",
                "invalid-gradientTransform.svg",
                "invalid-gradientUnits.svg",
                "invalid-spreadMethod.svg",
                "invalid-xlink-href.svg",
                "negative-r.svg",
                "no-stops.svg",
                "recursive-xlink-href.svg",
                "self-recursive-xlink-href.svg",
                "spreadMethod=pad.svg",
                "spreadMethod=reflect.svg",
                "spreadMethod=repeat.svg",
                "stops-via-xlink-href-complex-order.svg",
                "stops-via-xlink-href-from-linearGradient.svg",
                "stops-via-xlink-href-from-rect.svg",
                "stops-via-xlink-href.svg",
                "unresolved-xlink-href.svg",
                "xlink-href-not-to-gradient.svg"));
    }

    @TestFactory
    Collection<DynamicTest> paint_servers_stop() {
        return checkDirectory("paint-servers/stop", Set.of(
                "hsla-color.svg",
                "missing-offset-2.svg",
                "missing-offset-4.svg",
                "missing-offset-7.svg",
                "stop-color-with-currentColor-1.svg",
                "stop-color-with-currentColor-2.svg",
                "stop-color-with-currentColor-3.svg",
                "stop-color-with-inherit-1.svg",
                "stop-with-smaller-offset.svg",
                "stops-with-equal-offset-5.svg",
                "stops-with-equal-offset-6.svg",
                "zero-offset-in-the-middle.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_color() {
        return checkDirectory("painting/color");
    }

    @TestFactory
    Collection<DynamicTest> painting_context() {
        return checkDirectory("painting/context", Set.of(
                "in-nested-marker.svg",
                "with-gradient-and-gradient-transform.svg",
                "with-gradient-in-use.svg",
                "with-gradient-on-marker.svg",
                "with-pattern-and-transform-in-use.svg",
                "with-pattern-in-use.svg",
                "with-pattern-objectBoundingBox-in-use.svg",
                "with-pattern-on-marker.svg",
                "with-text.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_display() {
        return checkDirectory("painting/display", Set.of(
                "bBox-impact.svg",
                "none-on-tref.svg",
                "none-on-tspan-1.svg",
                "none-on-tspan-2.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_fill() {
        return checkDirectory("painting/fill", Set.of(
                "currentColor-without-parent.svg",
                "funcIRI-to-a-missing-element-with-a-fallback-color.svg",
                "funcIRI-to-a-missing-element-with-a-none-fallback.svg",
                "funcIRI-to-an-invalid-element-with-a-none-fallback.svg",
                "funcIRI-to-an-unsupported-element.svg",
                "funcIRI-with-a-fallback-color.svg",
                "hsl-120-100percent-25percent.svg",
                "hsl-120-200percent-25percent.svg",
                "hsl-360-100percent-25percent.svg",
                "hsl-999-100percent-25percent.svg",
                "hsl-with-alpha.svg",
                "hsla-with-percentage-s-and-l-values.svg",
                "icc-color.svg",
                "invalid-#RRGGBB-2.svg",
                "invalid-#RRGGBB-3.svg",
                "invalid-FuncIRI-with-a-currentColor-fallback.svg",
                "invalid-FuncIRI-with-a-fallback-color.svg",
                "linear-gradient-on-shape.svg",
                "linear-gradient-on-text.svg",
                "missing-FuncIRI-with-a-currentColor-fallback.svg",
                "pattern-on-text.svg",
                "radial-gradient-on-shape.svg",
                "radial-gradient-on-text.svg",
                "rgb-0-127-0-0.5.svg",
                "rgb-int-int-int.svg",
                "rgba-0-50percent-0-0.5.svg",
                "valid-FuncIRI-with-a-fallback-ICC-color.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_fill_opacity() {
        return checkDirectory("painting/fill-opacity", Set.of(
                "on-text.svg",
                "with-linearGradient.svg",
                "with-opacity.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_fill_rule() {
        return checkDirectory("painting/fill-rule");
    }

    @TestFactory
    Collection<DynamicTest> painting_isolation() {
        return checkDirectory("painting/isolation", Set.of(
                "as-property.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_marker() {
        return checkDirectory("painting/marker", Set.of(
                "inheritance-1.svg",
                "inheritance-2.svg",
                "marker-on-text.svg",
                "on-ArcTo.svg",
                "orient=auto-on-M-C-C-6.svg",
                "orient=auto-on-M-C-C-7.svg",
                "percent-values.svg",
                "recursive-1.svg",
                "recursive-2.svg",
                "recursive-3.svg",
                "recursive-4.svg",
                "recursive-5.svg",
                "target-with-subpaths-2.svg",
                "the-marker-property.svg",
                "with-a-large-stroke.svg",
                "with-an-image-child.svg",
                "with-viewBox-1.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_mix_blend_mode() {
        return checkDirectory("painting/mix-blend-mode", Set.of(
                "color-burn.svg",
                "color-dodge.svg",
                "color.svg",
                "darken.svg",
                "difference.svg",
                "exclusion.svg",
                "hard-light.svg",
                "hue.svg",
                "lighten.svg",
                "luminosity.svg",
                "multiply.svg",
                "opacity-on-element.svg",
                "overlay.svg",
                "saturation.svg",
                "screen.svg",
                "soft-light.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_opacity() {
        return checkDirectory("painting/opacity", Set.of(
                "bBox-impact.svg",
                "group-opacity.svg",
                "mixed-group-opacity.svg",
                "on-an-invalid-element.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_overflow() {
        return checkDirectory("painting/overflow", Set.of(
                "inherit-on-marker-without-parent.svg",
                "inherit-on-marker.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_paint_order() {
        return checkDirectory("painting/paint-order", Set.of(
                "duplicates.svg",
                "on-text.svg",
                "on-tspan.svg",
                "stroke-invalid.svg",
                "trailing-data.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_shape_rendering() {
        return checkDirectory("painting/shape-rendering", Set.of(
                "optimizeSpeed-on-text.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_stroke() {
        return checkDirectory("painting/stroke", Set.of(
                "currentColor-without-a-parent.svg",
                "gradient-with-objectBoundingBox-and-fallback-on-lines.svg",
                "gradient-with-objectBoundingBox-on-path-without-a-bbox-1.svg",
                "gradient-with-objectBoundingBox-on-path-without-a-bbox-2.svg",
                "gradient-with-objectBoundingBox-on-shape-without-a-bbox.svg",
                "linear-gradient-on-text.svg",
                "linear-gradient.svg",
                "pattern-on-text.svg",
                "pattern-with-objectBoundingBox-fallback-on-zero-bbox-shape.svg",
                "radial-gradient-on-text.svg",
                "radial-gradient.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_stroke_dasharray() {
        return checkDirectory("painting/stroke-dasharray", Set.of(
                "negative-sum.svg",
                "negative-values.svg",
                "zero-sum.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_stroke_dashoffset() {
        return checkDirectory("painting/stroke-dashoffset", Set.of(
                "percent-units.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_stroke_linecap() {
        return checkDirectory("painting/stroke-linecap");
    }

    @TestFactory
    Collection<DynamicTest> painting_stroke_linejoin() {
        return checkDirectory("painting/stroke-linejoin", Set.of(
                "arcs.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_stroke_miterlimit() {
        return checkDirectory("painting/stroke-miterlimit");
    }

    @TestFactory
    Collection<DynamicTest> painting_stroke_opacity() {
        return checkDirectory("painting/stroke-opacity", Set.of(
                "on-text.svg",
                "with-linearGradient.svg",
                "with-opacity.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_stroke_width() {
        return checkDirectory("painting/stroke-width", Set.of(
                "negative.svg"));
    }

    @TestFactory
    Collection<DynamicTest> painting_visibility() {
        return checkDirectory("painting/visibility", Set.of(
                "bbox-impact-1.svg",
                "bbox-impact-2.svg",
                "bbox-impact-3.svg",
                "collapse-on-tspan.svg",
                "hidden-on-group.svg",
                "hidden-on-tspan.svg"));
    }

    @TestFactory
    Collection<DynamicTest> shapes_circle() {
        return checkDirectory("shapes/circle");
    }

    @TestFactory
    Collection<DynamicTest> shapes_ellipse() {
        return checkDirectory("shapes/ellipse");
    }

    @TestFactory
    Collection<DynamicTest> shapes_line() {
        return checkDirectory("shapes/line", Set.of(
                "with-transform.svg"));
    }

    @TestFactory
    Collection<DynamicTest> shapes_path() {
        return checkDirectory("shapes/path");
    }

    @TestFactory
    Collection<DynamicTest> shapes_polygon() {
        return checkDirectory("shapes/polygon");
    }

    @TestFactory
    Collection<DynamicTest> shapes_polyline() {
        return checkDirectory("shapes/polyline");
    }

    @TestFactory
    Collection<DynamicTest> shapes_rect() {
        return checkDirectory("shapes/rect", Set.of(
                "cap-values.svg",
                "ch-values.svg",
                "ic-values.svg",
                "lh-values.svg",
                "rlh-values.svg",
                "vi-and-vb-values.svg",
                "vmin-and-vmax-values.svg",
                "vw-and-vh-values.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_a() {
        return checkDirectory("structure/a", Set.of(
                "inside-text.svg",
                "inside-tspan.svg",
                "on-text.svg",
                "on-tspan.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_defs() {
        return checkDirectory("structure/defs", Set.of(
                "multiple-defs.svg",
                "nested-defs.svg",
                "out-of-order.svg",
                "simple-case.svg",
                "style-inheritance-on-text.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_g() {
        return checkDirectory("structure/g", Set.of(
                "recursive-inheritance.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_image() {
        return checkDirectory("structure/image", Set.of(
                "embedded-16bit-png.svg",
                "embedded-gif.svg",
                "embedded-jpeg-as-image-jpeg.svg",
                "embedded-jpeg-as-image-jpg.svg",
                "embedded-jpeg-without-mime.svg",
                "embedded-png.svg",
                "embedded-svg-with-text.svg",
                "embedded-svg-without-mime.svg",
                "embedded-svg.svg",
                "embedded-svgz.svg",
                "external-gif.svg",
                "external-jpeg.svg",
                "external-png.svg",
                "external-svg-with-transform.svg",
                "external-svgz.svg",
                "float-size.svg",
                "no-height-non-square.svg",
                "no-height-on-svg.svg",
                "no-height.svg",
                "no-width-and-height-on-svg.svg",
                "no-width-and-height.svg",
                "no-width-on-svg.svg",
                "no-width.svg",
                "preserveAspectRatio=none-on-svg.svg",
                "preserveAspectRatio=none.svg",
                "preserveAspectRatio=xMaxYMax-meet-on-svg.svg",
                "preserveAspectRatio=xMaxYMax-meet.svg",
                "preserveAspectRatio=xMaxYMax-slice-on-svg.svg",
                "preserveAspectRatio=xMaxYMax-slice.svg",
                "preserveAspectRatio=xMidYMid-meet-on-svg.svg",
                "preserveAspectRatio=xMidYMid-meet.svg",
                "preserveAspectRatio=xMidYMid-slice-on-svg.svg",
                "preserveAspectRatio=xMidYMid-slice.svg",
                "preserveAspectRatio=xMinYMin-meet-on-svg.svg",
                "preserveAspectRatio=xMinYMin-meet.svg",
                "preserveAspectRatio=xMinYMin-slice-on-svg.svg",
                "preserveAspectRatio=xMinYMin-slice.svg",
                "raster-image-and-size-with-odd-numbers.svg",
                "recursive-2.svg",
                "url-to-png.svg",
                "url-to-svg.svg",
                "width-and-height-set-to-auto.svg",
                "with-transform.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_style() {
        return checkDirectory("structure/style", Set.of(
                "attribute-selector.svg",
                "combined-selectors.svg",
                "external-CSS.svg",
                "important.svg",
                "invalid-type.svg",
                "non-presentational-attribute.svg",
                "rule-specificity.svg",
                "universal-selector.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_style_attribute() {
        return checkDirectory("structure/style-attribute", Set.of(
                "comments.svg",
                "non-presentational-attribute.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_svg() {
        return checkDirectory("structure/svg", Set.of(
                "attribute-value-via-ENTITY-reference.svg",
                "elements-via-ENTITY-reference-1.svg",
                "elements-via-ENTITY-reference-2.svg",
                "elements-via-ENTITY-reference-3.svg",
                "funcIRI-parsing.svg",
                "funcIRI-with-invalid-characters.svg",
                "funcIRI-with-quotes.svg",
                "invalid-id-attribute-1.svg",
                "invalid-id-attribute-2.svg",
                "mixed-namespaces.svg",
                "no-size.svg",
                "not-UTF-8-encoding.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_switch() {
        return checkDirectory("structure/switch", Set.of(
                "comment-as-first-child.svg",
                "non-SVG-child.svg",
                "requiredFeatures.svg",
                "simple-case.svg",
                "single-child.svg",
                "systemLanguage.svg",
                "systemLanguage=en-GB.svg",
                "systemLanguage=en-US.svg",
                "systemLanguage=en.svg",
                "systemLanguage=ru-Ru.svg",
                "systemLanguage=ru-en.svg",
                "with-attributes.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_symbol() {
        return checkDirectory("structure/symbol", Set.of(
                "indirect-symbol-reference.svg",
                "with-custom-use-size.svg",
                "with-overflow-visible.svg",
                "with-transform-on-use.svg",
                "with-transform.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_systemLanguage() {
        return checkDirectory("structure/systemLanguage", Set.of(
                "on-svg.svg",
                "on-tspan.svg",
                "ru-Ru.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_transform() {
        return checkDirectory("structure/transform", Set.of(
                "extra-spaces.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_transform_origin() {
        return checkDirectory("structure/transform-origin", Set.of(
                "on-clippath-objectBoundingBox.svg",
                "on-clippath.svg",
                "on-gradient-object-bounding-box.svg",
                "on-gradient-user-space-on-use.svg",
                "on-image.svg",
                "on-pattern-object-bounding-box.svg",
                "on-pattern-user-space-on-use.svg",
                "on-text-path.svg",
                "on-text.svg"));
    }

    @TestFactory
    Collection<DynamicTest> structure_use() {
        return checkDirectory("structure/use", Set.of(
                "indirect-recursive-1.svg",
                "indirect-recursive-2.svg",
                "indirect-recursive-3.svg",
                "nested-recursive-1.svg",
                "nested-recursive-2.svg",
                "nested-xlink-to-svg-element-with-rect-and-size.svg",
                "recursive.svg",
                "self-recursive.svg",
                "xlink-to-a-child-of-a-non-SVG-element.svg",
                "xlink-to-an-external-file.svg",
                "xlink-to-svg-element-with-rect-only-width.svg",
                "xlink-to-svg-element-with-rect.svg",
                "xlink-to-svg-element-with-width-height-on-use.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_dominant_baseline() {
        return checkDirectory("text/dominant-baseline", Set.of(
                "alignment-baseline-and-baseline-shift-on-tspans.svg",
                "alphabetic.svg",
                "auto.svg",
                "central.svg",
                "complex.svg",
                "different-alignment-baseline-on-tspan.svg",
                "equal-alignment-baseline-on-tspan.svg",
                "hanging.svg",
                "ideographic.svg",
                "inherit.svg",
                "mathematical.svg",
                "middle.svg",
                "nested.svg",
                "no-change.svg",
                "reset-size.svg",
                "sequential.svg",
                "text-after-edge.svg",
                "text-before-edge.svg",
                "use-script.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_font_family() {
        return checkDirectory("text/font-family", Set.of(
                "bold-sans-serif.svg",
                "cursive.svg",
                "fallback-1.svg",
                "fallback-2.svg",
                "fantasy.svg",
                "sans-serif.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_font_size() {
        return checkDirectory("text/font-size", Set.of(
                "em-nested-and-mixed.svg",
                "em-on-the-root-element.svg",
                "em.svg",
                "ex-on-the-root-element.svg",
                "ex.svg",
                "mixed-values.svg",
                "named-value.svg",
                "negative-size.svg",
                "nested-percent-values-1.svg",
                "percent-value-without-a-parent.svg",
                "percent-value.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_font_stretch() {
        return checkDirectory("text/font-stretch", Set.of(
                "narrower.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_text() {
        return checkDirectory("text/text", Set.of(
                "bidi-reordering.svg",
                "complex-grapheme-split-by-tspan.svg",
                "complex-graphemes-and-coordinates-list.svg",
                "complex-graphemes.svg",
                "compound-emojis-and-coordinates-list.svg",
                "compound-emojis.svg",
                "dx-and-dy-instead-of-x-and-y.svg",
                "dx-and-dy-with-less-values-than-characters.svg",
                "dx-and-dy-with-more-values-than-characters.svg",
                "dx-and-dy-with-multiple-values.svg",
                "em-and-ex-coordinates.svg",
                "emojis.svg",
                "escaped-text-1.svg",
                "escaped-text-3.svg",
                "fill-rule=evenodd.svg",
                "filter-bbox.svg",
                "ligatures-handling-in-mixed-fonts-1.svg",
                "ligatures-handling-in-mixed-fonts-2.svg",
                "mm-coordinates.svg",
                "nested.svg",
                "no-coordinates.svg",
                "percent-value-on-dx-and-dy.svg",
                "percent-value-on-x-and-y.svg",
                "real-text-height.svg",
                "rotate-on-Arabic.svg",
                "rotate-with-an-invalid-angle.svg",
                "rotate-with-less-values-than-characters.svg",
                "rotate-with-more-values-than-characters.svg",
                "rotate-with-multiple-values-and-complex-text.svg",
                "rotate-with-multiple-values-underline-and-pattern.svg",
                "rotate-with-multiple-values.svg",
                "rotate.svg",
                "simple-case.svg",
                "transform.svg",
                "x-and-y-with-dx-and-dy-lists.svg",
                "x-and-y-with-dx-and-dy.svg",
                "x-and-y-with-multiple-values-and-arabic-text.svg",
                "x-and-y-with-multiple-values-and-tspan.svg",
                "xml-lang=ja.svg",
                "xml-space.svg",
                "zalgo.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_text_anchor() {
        return checkDirectory("text/text-anchor", Set.of(
                "coordinates-list.svg",
                "end-on-text.svg",
                "end-with-letter-spacing.svg",
                "inheritance-2.svg",
                "inheritance-3.svg",
                "invalid-value-on-text.svg",
                "middle-on-text.svg",
                "on-the-first-tspan.svg",
                "on-tspan-with-arabic.svg",
                "on-tspan.svg",
                "start-on-text.svg",
                "text-anchor-not-on-text-chunk.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_text_decoration() {
        return checkDirectory("text/text-decoration", Set.of(
                "all-types-inline-comma-separated.svg",
                "all-types-inline-no-spaces.svg",
                "all-types-inline.svg",
                "all-types-nested.svg",
                "indirect-with-multiple-colors.svg",
                "indirect.svg",
                "line-through.svg",
                "outside-the-text-element.svg",
                "overline.svg",
                "style-resolving-1.svg",
                "style-resolving-2.svg",
                "style-resolving-3.svg",
                "style-resolving-4.svg",
                "tspan-decoration.svg",
                "underline-with-dy-list-1.svg",
                "underline-with-dy-list-2.svg",
                "underline-with-rotate-list-3.svg",
                "underline-with-rotate-list-4.svg",
                "underline-with-y-list.svg",
                "underline.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_textPath() {
        return checkDirectory("text/textPath", Set.of(
                "closed-path.svg",
                "complex.svg",
                "dy-with-tiny-coordinates.svg",
                "invalid-link.svg",
                "invalid-textPath-in-the-middle.svg",
                "link-to-rect.svg",
                "m-A-path.svg",
                "m-L-Z-path.svg",
                "method=stretch.svg",
                "mixed-children-1.svg",
                "mixed-children-2.svg",
                "nested.svg",
                "no-link.svg",
                "path-with-ClosePath.svg",
                "path-with-subpaths-and-startOffset.svg",
                "path-with-subpaths.svg",
                "side=right.svg",
                "simple-case.svg",
                "spacing=auto.svg",
                "startOffset=-100.svg",
                "startOffset=10percent.svg",
                "startOffset=30.svg",
                "startOffset=5mm.svg",
                "tspan-with-absolute-position.svg",
                "tspan-with-relative-position.svg",
                "two-paths.svg",
                "very-long-text.svg",
                "with-baseline-shift-and-rotate.svg",
                "with-baseline-shift.svg",
                "with-big-letter-spacing.svg",
                "with-coordinates-on-text.svg",
                "with-coordinates-on-textPath.svg",
                "with-filter.svg",
                "with-invalid-path-and-xlink-href.svg",
                "with-letter-spacing.svg",
                "with-path-and-xlink-href.svg",
                "with-path.svg",
                "with-rotate.svg",
                "with-text-anchor.svg",
                "with-transform-on-a-referenced-path.svg",
                "with-transform-outside-a-referenced-path.svg",
                "with-underline.svg",
                "writing-mode=tb.svg"));
    }

    @TestFactory
    Collection<DynamicTest> text_tspan() {
        return checkDirectory("text/tspan", Set.of(
                "bidi-reordering.svg",
                "mixed-font-size.svg",
                "mixed-xml-space-1.svg",
                "mixed-xml-space-2.svg",
                "mixed-xml-space-3.svg",
                "mixed.svg",
                "multiple-coordinates.svg",
                "nested-rotate.svg",
                "nested-whitespaces.svg",
                "nested.svg",
                "only-with-y.svg",
                "outside-the-text.svg",
                "rotate-and-display-none.svg",
                "rotate-on-child.svg",
                "sequential.svg",
                "style-override.svg",
                "text-shaping-across-multiple-tspan-1.svg",
                "text-shaping-across-multiple-tspan-2.svg",
                "transform.svg",
                "tspan-bbox-1.svg",
                "tspan-bbox-2.svg",
                "with-clip-path.svg",
                "with-dy.svg",
                "with-filter.svg",
                "with-mask.svg",
                "with-opacity.svg",
                "with-x-and-y.svg",
                "without-attributes.svg",
                "xml-space-1.svg",
                "xml-space-2.svg"));
    }

    record ReSVGRefTest(@NotNull Path testFile) implements Executable {

        @Override
        public void execute() throws Throwable {
            var pngRef = testFile.resolveSibling(testFile.getFileName().toString().replace(".svg", ".png"));
            var result = ImageComparison.compareImages(new ImageComparison.CompareInfo(
                    expected(new UrlImageSource(pngRef.toUri().toURL()),
                            RenderType.DiskImage),
                    actual(new UrlImageSource(testFile.toUri().toURL()),
                            jsvgRenderType)));
            assertEquals(SUCCESS, result);
        }
    }
}
