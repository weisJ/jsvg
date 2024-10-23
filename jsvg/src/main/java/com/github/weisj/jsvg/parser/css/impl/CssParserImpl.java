/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.StyleProperty;
import com.github.weisj.jsvg.parser.css.StyleSheet;

public class CssParserImpl implements CssParser {
    @Override
    public @NotNull StyleSheet parse(@NotNull List<char[]> input) {
        List<CssRule> cssRules = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (char[] chars : input) {
            sb.append(chars);
        }
        String cssInput = sb.toString();

        String[] ruleBlocks = cssInput.split("(?<=})");

        for (String ruleBlock : ruleBlocks) {
            if (ruleBlock.trim().isEmpty()) continue;

            // Split by '{' to separate selector(s) and declaration block
            String[] parts = ruleBlock.split("(?=\\{)", 2);

            if (parts.length != 2) continue;

            String selector = parts[0].trim();
            Map<@NotNull String, @NotNull StyleProperty> properties = parseProperties(parts[1].trim());
            if (properties != null && !properties.isEmpty()) {
                cssRules.add(CssRule.parse(selector, properties));
            }
        }

        return new StyleSheetImpl(cssRules);
    }

    private static @Nullable Map<@NotNull String, @NotNull StyleProperty> parseProperties(
            @NotNull String declarationBlock) {
        Map<String, StyleProperty> properties = new HashMap<>();

        if (!declarationBlock.endsWith("}")) return null;
        if (!declarationBlock.startsWith("{")) return null;

        String block = declarationBlock.substring(1, declarationBlock.length() - 1);

        // Tokenize and parse the declaration block by ';' to separate properties
        String[] declarations = block.split(";");
        for (String declaration : declarations) {
            if (declaration.contains(":")) {
                String[] propertyValue = declaration.split(":", 2);
                if (propertyValue.length == 2) {
                    String property = propertyValue[0].trim();
                    String value = propertyValue[1].trim();
                    properties.put(property, new StyleProperty(property, value));
                }
            }
        }
        return properties;
    }


}
