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

import static com.github.weisj.jsvg.ImageComparison.ImageInfo.actual;
import static com.github.weisj.jsvg.ImageComparison.ImageInfo.expected;
import static com.github.weisj.jsvg.ImageComparison.ReferenceTestResult.SUCCESS;
import static com.github.weisj.jsvg.ImageComparison.compareImages;
import static com.github.weisj.jsvg.ImageComparison.renderJsvg;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ImageComparison.CompareInfo;
import com.github.weisj.jsvg.ImageComparison.ImageSource.MemoryImageSource;
import com.github.weisj.jsvg.ImageComparison.ImageSource.PathImageSource;
import com.github.weisj.jsvg.ImageComparison.RenderType;
import com.github.weisj.jsvg.renderer.animation.AnimationState;

class GradientTest {

    @Test
    void linearGradientRefTest() {
        assertEquals(SUCCESS, compareImages("gradient/linearGradient.svg"));
        assertEquals(SUCCESS, compareImages("gradient/stripes.svg"));
    }

    @Test
    void decreasingDuplicateAndMissingStops() {
        // The reference expresses ordered stops directly. Use the same rasterizer to avoid
        // unrelated endpoint quantization differences in Batik's gradients.
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new PathImageSource("gradient/stopValidation_ref.svg"), RenderType.JSVG),
                actual(new PathImageSource("gradient/stopValidation.svg"), RenderType.JSVG), 0, 0)));
    }

    @Test
    void radialGradientRefTest() {
        assertEquals(SUCCESS, compareImages("gradient/radialGradient.svg"));
        assertEquals(SUCCESS, compareImages("gradient/radialGradient2.svg"));
        assertEquals(SUCCESS, compareImages("gradient/radialGradient3.svg"));
    }

    @Test
    void radialGradientOutOfMemory() {
        assertDoesNotThrow(() -> renderJsvg("gradient/bad_gradient_stop_issue_51.svg"));
    }

    @Test
    void currentColorAndInheritUseDefinitionTreeComputedValues() {
        String actualSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500" color="red">
                  <defs>
                    <!-- Inherited currentColor stays a keyword and therefore uses the stop's green color. -->
                    <linearGradient id="keyword" stop-color="currentColor">
                      <stop offset="0" stop-color="inherit" color="green"/>
                      <stop offset="1" stop-color="currentColor"/>
                    </linearGradient>

                    <!-- stop-color is not naturally inherited: the undeclared gradient computes to black. -->
                    <g stop-color="green">
                      <linearGradient id="immediate-parent">
                        <stop stop-color="inherit"/>
                      </linearGradient>
                    </g>

                    <!-- Explicit inherit on each link carries green through the definition tree. -->
                    <g stop-color="green">
                      <linearGradient id="explicit-chain" stop-color="inherit">
                        <stop stop-color="inherit"/>
                      </linearGradient>
                    </g>
                  </defs>
                  <!-- The referencing element's blue color must not affect any gradient stop. -->
                  <rect width="200" height="500" color="blue" fill="url(#keyword)"/>
                  <rect x="200" width="200" height="500" color="blue" fill="url(#immediate-parent)"/>
                  <rect x="400" width="200" height="500" color="blue" fill="url(#explicit-chain)"/>
                </svg>
                """;
        String referenceSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500">
                  <defs>
                    <linearGradient id="keyword">
                      <stop offset="0" stop-color="green"/>
                      <stop offset="1" stop-color="red"/>
                    </linearGradient>
                  </defs>
                  <rect width="200" height="500" fill="url(#keyword)"/>
                  <rect x="200" width="200" height="500" fill="black"/>
                  <rect x="400" width="200" height="500" fill="green"/>
                </svg>
                """;
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new MemoryImageSource("resolved-definition-colors", referenceSvg), RenderType.JSVG),
                actual(new MemoryImageSource("definition-context-colors", actualSvg), RenderType.JSVG), 0, 0)));
    }

    @Test
    void hslAndContextDependentStopColors() {
        String actualSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500">
                  <g color="green">
                    <defs>
                      <linearGradient id="gradient" stop-color="blue">
                        <stop offset="0" stop-color="hsla(60, 100%, 50%, 0.5)"/>
                        <stop offset="0.5" stop-color="currentColor"/>
                        <stop offset="1" stop-color="inherit"/>
                      </linearGradient>
                    </defs>
                  </g>
                  <rect width="600" height="500" fill="url(#gradient)"/>
                </svg>
                """;
        String referenceSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500">
                  <defs>
                    <linearGradient id="gradient">
                      <stop offset="0" stop-color="#ffff0080"/>
                      <stop offset="0.5" stop-color="green"/>
                      <stop offset="1" stop-color="blue"/>
                    </linearGradient>
                  </defs>
                  <rect width="600" height="500" fill="url(#gradient)"/>
                </svg>
                """;
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new MemoryImageSource("resolved-stop-colors", referenceSvg), RenderType.JSVG),
                actual(new MemoryImageSource("context-dependent-stop-colors", actualSvg), RenderType.JSVG), 0, 0)));
    }

    @Test
    void animatedStopColor() {
        String animatedSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500">
                  <defs>
                    <linearGradient id="gradient">
                      <stop offset="0" stop-color="red">
                        <animate attributeName="stop-color" values="red;blue" dur="1s" fill="freeze"/>
                      </stop>
                      <stop offset="1" stop-color="red">
                        <animate attributeName="stop-color" values="red;blue" dur="1s" fill="freeze"/>
                      </stop>
                    </linearGradient>
                  </defs>
                  <rect width="600" height="500" fill="url(#gradient)"/>
                </svg>
                """;
        assertSolidGradientFrame("animated-stop-color", animatedSvg, 0, "red");
        assertSolidGradientFrame("animated-stop-color", animatedSvg, 500, "purple");
        assertSolidGradientFrame("animated-stop-color", animatedSvg, 1500, "blue");
    }

    @Test
    void currentColorTracksDefinitionTreeAnimation() {
        String animatedSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500">
                  <g color="red">
                    <animate attributeName="color" values="red;blue" dur="1s" fill="freeze"/>
                    <defs>
                      <linearGradient id="gradient">
                        <stop offset="0" stop-color="currentColor"/>
                        <stop offset="1" stop-color="currentColor"/>
                      </linearGradient>
                    </defs>
                  </g>
                  <rect width="600" height="500" fill="url(#gradient)"/>
                </svg>
                """;
        assertSolidGradientFrame("animated-stop-current-color", animatedSvg, 0, "red");
        assertSolidGradientFrame("animated-stop-current-color", animatedSvg, 500, "purple");
        assertSolidGradientFrame("animated-stop-current-color", animatedSvg, 1500, "blue");
    }

    @Test
    void inheritedStopColorTracksDefinitionTreeAnimation() {
        String animatedSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500">
                  <g stop-color="red">
                    <animate attributeName="stop-color" values="red;blue" dur="1s" fill="freeze"/>
                    <linearGradient id="gradient" stop-color="inherit">
                      <stop offset="0" stop-color="inherit"/>
                      <stop offset="1" stop-color="inherit"/>
                    </linearGradient>
                  </g>
                  <rect width="600" height="500" fill="url(#gradient)"/>
                </svg>
                """;
        assertSolidGradientFrame("animated-inherited-stop-color", animatedSvg, 0, "red");
        assertSolidGradientFrame("animated-inherited-stop-color", animatedSvg, 500, "purple");
        assertSolidGradientFrame("animated-inherited-stop-color", animatedSvg, 1500, "blue");
    }

    private static void assertSolidGradientFrame(String name, String animatedSvg, long timestamp, String color) {
        String referenceSvg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="500">
                  <rect width="600" height="500" fill="%s"/>
                </svg>
                """.formatted(color);
        assertEquals(SUCCESS, compareImages(new CompareInfo(
                expected(new MemoryImageSource(name + "-" + color, referenceSvg), RenderType.JSVG),
                actual(new MemoryImageSource(name, animatedSvg),
                        RenderType.JSVG.withAnimationState(new AnimationState(0, timestamp))),
                0, 0)), name + " at " + timestamp + " ms");
    }
}
