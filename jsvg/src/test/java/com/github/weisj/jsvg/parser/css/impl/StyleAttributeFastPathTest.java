/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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
package com.github.weisj.jsvg.parser.css.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.parser.css.data.NormalizedProperty;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParserInput;
import com.github.weisj.jsvg.parser.css.impl.phase3ruleparse.CssNormalizer;
import com.github.weisj.jsvg.renderer.CssHints;

/** The shortcuts of {@link FullCssParser} must give the same result as the full parser pipeline. */
class StyleAttributeFastPathTest {
    private static final Pattern STYLE = Pattern.compile("(?<![\\w-])style=\"([^\"]*)\"|(?<![\\w-])style='([^']*)'");

    private static final List<String> EDGE_CASES = List.of(
            "", " ", ";", ";;", " ; ; ", "fill", "fill:", "fill: ;", ":red", "fill red", "fill:red", " FILL : Red ;",
            "fill:red;fill:blue", "fill:red!important", "fill:red ! important;stroke:blue", "fill:!important",
            "--x:1", "--X:a;fill:red", "-x:1", "-1:2", "1a:2", "_a:1", "a_b-c1:1", "a.b:1;fill:red", "a b:1;fill:red",
            "marker:none", "marker:url(#a)", "MARKER:none;fill:red", "font:12px serif", "font:italic bold 12px/2 'A b'",
            "fill:#000", "fill:#abc", "fill:#-a", "fill:#--", "fill:#_a", "fill:#1a", "fill:#", "fill:# a", "fill:#a b",
            "a:1", "a:01", "a:1.0", "a:1.00", "a:0.00", "a:.5", "a:1.", "a:+1", "a:-1", "a:1e3", "a:1E3", "a:1e-3",
            "a:1e",
            "a:1ex", "a:1em", "a:1EM", "a:1px", "a:1_x", "a:1-x", "a:1x2", "a:50%", "a:50% ", "a:50%%", "a:1.5%",
            "a:1.5px",
            "a:12345678901234567890", "a:0.1234567890123456789012345", "a:1 2", "a:1,2", "a:1, 2",
            "a:url(#x)", "a:URL(#x)", "a:url( #x )", "a:url(#x", "a:url(#x y)", "a:url()", "a:url('#x')",
            "a:url(\"#x\")",
            "a:url(a.png)", "a:url(a/b.png)", "a:url(a;b)", "a:url(#a);b:c", "a:foo(1)", "a:foo(1;2)", "a:(1)", "a:)",
            "a:(", "a:rgb(1,2,3);b:2", "a:'x'", "a:'x;y';b:1", "a:\"x\";b:'y'", "a:'x", "a:'x\ny'", "a:\\66",
            "a:b\\;c;d:e",
            "a:b/* c */;d:e", "/* c */a:b", "a/* c */:b", "a:{b}", "a:[b]", "a:{b;c}", "@media x{a:b}", "@x;a:b",
            "a:b;@x;c:d", "a:b\tc", "a:b\nc", "a\n:\nb\n", "a:b\r\nc", "a:b\fc", "a:\u00e4", "\u00e4:b",
            "a:b" + (char) 0 + "c",
            "a:-b", "a:--b", "a:-", "a:_", "a:b-", "a:<!--", "a:-->", "a:b:c", "a::b", "a:b c:d;e:f",
            "-inkscape-font-specification:'Sans, Normal';font-family:Sans",
            "font:12px serif;fill:red", "fill:red;font:12px serif;stroke:blue", "FONT:12px serif", "a:1!important;b:2",
            "a:url(a'b)c'd);e:f", "a:url(a\"b)c\"d);e:f", "a:foo('x');b:c", "a:foo('x)');b:c");

    private static final List<String> ATTRIBUTE_VALUES = List.of(
            "", " 1 ", "1 ", "red", "Red", "-a", "--a", "_a", "#abc", "#1a", "#-a", "#", "1", "01", "1.0", "1.50", "1.",
            ".5", "-1", "+1", "1e3", "1E-3", "1em", "1ex", "1.5px", "1_x", "1-x", "50%", "50.5%", "50%%", "url(#x)",
            "URL(#x)", "url(a.png)", "url(a/b.png)", "url(#x y)", "url()", "url('#x')", "matrix(1 0 0 1 2 3)", "1 2",
            "a b", "rgb(1,2,3)");

    private static List<NormalizedProperty> full(String style) {
        return CssNormalizer
                .normalizeAttribute(BasicParser.parseListOfDeclarations(BasicParserInput.fromString(style)));
    }

    private static void collectStyles(Path root, Set<String> styles) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(p -> p.toString().endsWith(".svg")).toList();
        }
        for (Path file : files) {
            Matcher m = STYLE.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            while (m.find())
                styles.add(m.group(1) != null ? m.group(1) : m.group(2));
        }
    }

    private static void check(FullCssParser parser, String style) {
        assertEquals(full(style), parser.parseStyleAttribute(style, CssHints.DEFAULT), () -> "style: " + style);
    }

    @Test
    void styleAttributesParseLikeFullParser() throws IOException {
        Set<String> styles = new TreeSet<>(EDGE_CASES);
        Path root = Paths.get(System.getProperty("user.dir"), "..", "..");
        collectStyles(root.resolve("jsvg/src/test/resources"), styles);
        Path resvgSuite = root.resolve("resvg-test-suite/tests");
        if (Files.isDirectory(resvgSuite)) collectStyles(resvgSuite, styles);
        FullCssParser parser = new FullCssParser();
        List<String> failures = new ArrayList<>();
        for (String style : styles) {
            try {
                check(parser, style);
            } catch (AssertionError e) {
                failures.add(e.getMessage());
            }
        }
        assertEquals(List.of(), failures);
    }

    @Test
    void attributeValuesParseLikeFullParser() {
        FullCssParser parser = new FullCssParser();
        for (String value : ATTRIBUTE_VALUES) {
            assertEquals(BasicParser.parseListOfComponentValues(BasicParserInput.fromString(value)),
                    parser.parseCssAttribute(value), () -> "value: " + value);
        }
    }
}
