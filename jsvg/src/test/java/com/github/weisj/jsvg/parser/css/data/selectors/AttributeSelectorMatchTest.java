/*
 * MIT License
 *
 * Copyright (c) 2023-2026 Jannis Weis
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
package com.github.weisj.jsvg.parser.css.data.selectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.parser.css.data.selectors.SimpleSelector.Attribute;
import com.github.weisj.jsvg.parser.css.data.selectors.SimpleSelector.Attribute.Operator;
import com.github.weisj.jsvg.parser.impl.ParsedElement;
import com.github.weisj.jsvg.parser.impl.ParserTestUtil;

class AttributeSelectorMatchTest {

    // 'type' is case-insensitive by default (HTML Standard list), 'fill' is case-sensitive.
    private static final ParsedElement ELEMENT = ParserTestUtil
            .createDummyAttributeNode(Map.of("fill", "red", "type", "foo", "class", "a b"))
            .element();

    private static boolean matches(@NotNull String name, @Nullable Operator op, @Nullable String value,
            @Nullable Boolean caseSensitive) {
        return new Attribute(name, op, value, caseSensitive).matches(ELEMENT).matches;
    }

    @Test
    void defaultCaseSensitivity() {
        assertTrue(matches("fill", Operator.EQUALS, "red", null));
        assertFalse(matches("fill", Operator.EQUALS, "RED", null));
        assertTrue(matches("type", Operator.EQUALS, "foo", null));
        assertTrue(matches("type", Operator.EQUALS, "FOO", null));
    }

    @Test
    void explicitFlagOverridesDefault() {
        assertTrue(matches("fill", Operator.EQUALS, "RED", false));
        assertFalse(matches("type", Operator.EQUALS, "FOO", true));
    }

    @Test
    void otherOperatorsUseTheSameDefault() {
        assertFalse(matches("fill", Operator.PREFIX, "R", null));
        assertTrue(matches("type", Operator.PREFIX, "F", null));
        assertFalse(matches("fill", Operator.SUFFIX, "D", null));
        assertTrue(matches("type", Operator.SUFFIX, "O", null));
        assertFalse(matches("fill", Operator.SUBSTRING, "E", null));
        assertTrue(matches("type", Operator.SUBSTRING, "O", null));
        assertTrue(matches("class", Operator.INCLUDES, "b", null));
        assertFalse(matches("class", Operator.INCLUDES, "B", null));
        assertFalse(matches("fill", Operator.DASH_MATCH, "RED", null));
    }

    @Test
    void presence() {
        assertTrue(matches("fill", null, null, null));
        assertFalse(matches("stroke", null, null, null));
        assertFalse(matches("stroke", Operator.EQUALS, "x", null));
    }
}
