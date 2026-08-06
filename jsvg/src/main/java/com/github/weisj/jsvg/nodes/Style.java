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
package com.github.weisj.jsvg.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.data.StyleRuleList;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.CssHints;

@ElementCategories({/* None */})
@PermittedContent(any = true, charData = true)
public final class Style extends MetaSVGNode {
    public static final String TAG = "style";

    private StyleRuleList styleSheet;

    private final List<char @NotNull []> data = new ArrayList<>();

    public void parseStyleSheet(@NotNull AttributeNode attributeNode, @NotNull CssParser cssParser,
            @NotNull CssHints cssHints) {
        // Only type "text/css" (or an empty/absent one) is a stylesheet (SVG 1.1 § 6.3).
        String type = attributeNode.getValue("type");
        boolean cssType = type == null || type.trim().isEmpty() || "text/css".equalsIgnoreCase(type.trim());
        styleSheet = cssType
                ? cssParser.parseStyleSheet(data, cssHints)
                : new StyleRuleList(Collections.emptyList());
        data.clear();
    }

    public @NotNull StyleRuleList styleSheet() {
        return Objects.requireNonNull(styleSheet);
    }

    @Override
    public void addContent(@NotNull TextContent.Segment segment) {
        if (!segment.isConstant()) {
            throw new UnsupportedOperationException(
                    "Style elements only support constant text. Encountered: " + segment);
        }
        String content = segment.text();
        data.add(content.toCharArray());
    }

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

}
