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
package com.github.weisj.jsvg.attribute;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.parser.NumberListSplitter;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParserInput;
import com.github.weisj.jsvg.parser.impl.AttributeParser;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.util.RandomData;
import com.github.weisj.jsvg.view.FloatSize;

class AttributeParserTest {

    private AttributeParser parser;

    @BeforeEach
    void setup() {
        parser = AttributeParser.INSTANCE;
    }

    @Test
    void testStringListNoRequiredComma() {
        testStringList(false);
    }

    @Test
    void testStringListRequiredComma() {
        testStringList(true);
    }

    @Test
    void testNumberSplitter() {
        BiConsumer<String, String[]> test = (str, expected) -> {
            String[] result = parser.parseStringList(str, NumberListSplitter.INSTANCE);
            Assertions.assertArrayEquals(expected, result, "input: " + str);
        };
        test.accept("1,1", new String[] {"1", "1"});
        test.accept("1 1", new String[] {"1", "1"});
        test.accept("1-1", new String[] {"1", "-1"});
        test.accept("1, 1", new String[] {"1", "1"});
        test.accept("1,  1", new String[] {"1", "1"});
        test.accept("1 , 1", new String[] {"1", "1"});
        test.accept("1, ,1", new String[] {"1", "", "1"});

        // Negative numbers
        test.accept("-1-1", new String[] {"-1", "-1"});
        test.accept("-1 -1", new String[] {"-1", "-1"});
        test.accept("-1,-1", new String[] {"-1", "-1"});

        // Positive sign
        test.accept("1+1", new String[] {"1", "+1"});
        test.accept("+1+1", new String[] {"+1", "+1"});
        test.accept("+1-1", new String[] {"+1", "-1"});

        // Decimals
        test.accept("1.5 2.5", new String[] {"1.5", "2.5"});
        test.accept("1.5,2.5", new String[] {"1.5", "2.5"});
        test.accept("1.5-2.5", new String[] {"1.5", "-2.5"});
        test.accept("1.5+2.5", new String[] {"1.5", "+2.5"});

        // Scientific notation – sign after 'e'/'E' must NOT split the token
        test.accept("1e10 2e-3", new String[] {"1e10", "2e-3"});
        test.accept("1e+10 2e-3", new String[] {"1e+10", "2e-3"});

        // Leading sign only at position 0 should not split
        test.accept("+1 -2", new String[] {"+1", "-2"});

        // Multiple whitespace / mixed separators
        test.accept("1  2  3", new String[] {"1", "2", "3"});
        test.accept("1 , 2 , 3", new String[] {"1", "2", "3"});
        test.accept("10-20-30", new String[] {"10", "-20", "-30"});

        // Empty / blank input
        test.accept("", new String[] {});
        test.accept("   ", new String[] {});

        // Single value
        test.accept("42", new String[] {"42"});
        test.accept("-42", new String[] {"-42"});
    }

    private void testStringList(boolean requireComma) {
        Random r = new Random();
        for (int i = 0; i < 20; i++) {
            String[] arr = RandomData.generateRandomStringArray(r);
            String[] parsed = parser.parseStringList(
                    appendToList(arr, r, requireComma),
                    requireComma ? SeparatorMode.COMMA_ONLY : SeparatorMode.COMMA_AND_WHITESPACE);
            Assertions.assertArrayEquals(arr, parsed);
        }
    }

    private <T> String appendToList(T[] arr, Random r, boolean requireComma) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i < arr.length - 1) {
                char separator = !requireComma && r.nextBoolean() ? ' ' : ',';
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    private static @NotNull AffineTransform resolve(@Nullable List<TransformPart> parts) {
        assertNotNull(parts);
        MeasureContext ctx = MeasureContext.createInitial(new FloatSize(0, 0), 0, 0, new AnimationState(0, 0));
        AffineTransform t = new AffineTransform();
        for (TransformPart part : parts) {
            t = part.applyToTransform(t, ctx);
        }
        return t;
    }

    private static void assertTransform(@NotNull AffineTransform expected, @Nullable List<TransformPart> parts) {
        AffineTransform actual = resolve(parts);
        double[] e = new double[6];
        double[] a = new double[6];
        expected.getMatrix(e);
        actual.getMatrix(a);
        Assertions.assertArrayEquals(e, a, 1e-6, "expected " + expected + " but was " + actual);
    }

    private @Nullable List<TransformPart> parseTokens(@NotNull String css) {
        return parser.parseTransform(BasicParser.parseListOfComponentValues(BasicParserInput.fromString(css)));
    }

    @Test
    void transformSvgAttributeGrammar() {
        // Unitless arguments are user units and degrees (SVG 1.1 §7.6), both separators are allowed.
        AffineTransform rotate90 = AffineTransform.getRotateInstance(Math.PI / 2);
        assertTransform(rotate90, parseTokens("rotate(90)"));
        assertTransform(AffineTransform.getRotateInstance(Math.PI / 2, 10, 20), parseTokens("rotate(90 10 20)"));
        assertTransform(AffineTransform.getRotateInstance(Math.PI / 2, 10, 20), parseTokens("rotate(90,10,20)"));
        assertTransform(AffineTransform.getTranslateInstance(10, 20), parseTokens("translate(10 20)"));
        assertTransform(AffineTransform.getTranslateInstance(10, 20), parseTokens("translate(10, 20)"));
        assertTransform(AffineTransform.getTranslateInstance(10, 0), parseTokens("translate(10)"));
        assertTransform(AffineTransform.getScaleInstance(2, 2), parseTokens("scale(2)"));
        assertTransform(AffineTransform.getScaleInstance(2, 3), parseTokens("scale(2 3)"));
        assertTransform(AffineTransform.getShearInstance(1, 0), parseTokens("skewX(45)"));
        assertTransform(AffineTransform.getShearInstance(0, 1), parseTokens("skewY(45)"));
        assertTransform(new AffineTransform(1, 2, 3, 4, 5, 6), parseTokens("matrix(1 2 3 4 5 6)"));

        AffineTransform composed = AffineTransform.getTranslateInstance(10, 20);
        composed.rotate(Math.PI / 2);
        assertTransform(composed, parseTokens("translate(10 20) rotate(90)"));
        assertTransform(composed, parseTokens("translate(10,20),rotate(90)"));

        // The string parser (SVG-only attributes, SMIL values) agrees.
        assertTransform(rotate90, parser.parseTransform("rotate(90)"));
        assertTransform(AffineTransform.getRotateInstance(Math.PI / 2, 10, 20),
                parser.parseTransform("rotate(90 10 20)"));
        assertTransform(AffineTransform.getShearInstance(1, 0), parser.parseTransform("skewX(45)"));
        assertTransform(composed, parser.parseTransform("translate(10,20),rotate(90)"));

        Assertions.assertNull(parseTokens("none"));
        Assertions.assertNull(parser.parseTransform("none"));
        Assertions.assertNull(parseTokens("rotate()"));
        Assertions.assertNull(parseTokens("frobnicate(1)"));
    }

    @Test
    void transformAnglesAcceptUnits() {
        // <angle> units (allowed in the attribute by SVG 2 §8.5) resolve like unitless degrees.
        AffineTransform rotate45 = resolve(parseTokens("rotate(45)"));
        assertTransform(rotate45, parseTokens("rotate(45deg)"));
        assertTransform(rotate45, parseTokens("rotate(50grad)"));
        assertTransform(rotate45, parseTokens("rotate(0.125turn)"));
        assertTransform(rotate45, parseTokens("rotate(0.7853982rad)"));
        assertTransform(resolve(parseTokens("rotate(45 10 20)")), parseTokens("rotate(45deg, 10, 20)"));

        assertTransform(resolve(parseTokens("skewX(30)")), parseTokens("skewX(30deg)"));
        assertTransform(resolve(parseTokens("skewY(30)")), parseTokens("skewY(30DEG)"));
        assertTransform(resolve(parseTokens("skew(10 20)")), parseTokens("skew(10deg, 20deg)"));

        // The string parser (SVG-only attributes, SMIL values) follows the same grammar.
        assertTransform(rotate45, parser.parseTransform("rotate(45deg)"));
        assertTransform(rotate45, parser.parseTransform("rotate(50grad)"));
        assertTransform(resolve(parser.parseTransform("skewX(30)")), parser.parseTransform("skewX(30deg)"));

        // Lengths are not angles.
        Assertions.assertNull(parseTokens("rotate(45px)"));
        Assertions.assertNull(parseTokens("skewX(30px)"));
        Assertions.assertNull(parser.parseTransform("rotate(45px)"));
        // Scale factors stay plain numbers.
        Assertions.assertNull(parseTokens("scale(2deg)"));
    }
}
