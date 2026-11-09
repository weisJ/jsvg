/*
 * MIT License
 *
 * Copyright (c) 2022-2026 Jannis Weis
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

import java.util.Map;

import org.jetbrains.annotations.NotNull;

public final class Utils {
    private Utils() {}

    public static String wrapTag(int width, int height, @NotNull String tag) {
        return element("svg")
                .attributes("xmlns='http://www.w3.org/2000/svg'", "xmlns:xlink='http://www.w3.org/1999/xlink'")
                .attributes(Map.of("width", width, "height", height, "viewBox", "0 0 " + width + " " + height))
                .children(tag).build();
    }

    public static ElementBuilder element(@NotNull String name) {
        return new ElementBuilder(name);
    }

    public static String rectangle(int x, int y, int width, int height, int color) {
        return element("rect")
                .attributes(Map.of("x", x, "y", y, "width", width, "height", height,
                        "fill", "#" + String.format("%06x", color & 0xffffff)))
                .build();
    }

    /** Wraps a source and a user-space filter named 'f', defaulting its region to the viewport. */
    public static String filterDocument(int width, int height, @NotNull String primitives,
            @NotNull String source, @NotNull String region, @NotNull String filterAttributes) {
        ElementBuilder filter = element("filter").attributes("id='f'", "filterUnits='userSpaceOnUse'");
        if (region.isEmpty()) {
            filter.attributes(Map.of("x", 0, "y", 0, "width", width, "height", height));
        } else {
            filter.attributes(region);
        }
        filter.attributes(filterAttributes).children(primitives);
        return wrapTag(width, height, element("defs").children(filter).build() + source);
    }

    public static final class ElementBuilder {
        private final String name;
        private final StringBuilder attributes = new StringBuilder();
        private final StringBuilder children = new StringBuilder();
        private boolean hasChildren;

        private ElementBuilder(String name) {
            this.name = name;
        }

        // Raw fragments allow tests to deliberately construct invalid markup as well.
        public ElementBuilder attributes(String... fragments) {
            for (String fragment : fragments) {
                if (!fragment.isEmpty()) {
                    attributes.append(' ').append(fragment);
                }
            }
            return this;
        }

        public ElementBuilder attributes(Map<String, ?> values) {
            values.forEach((key, value) -> attributes.append(' ').append(key).append("='")
                    .append(value.toString().replace("&", "&amp;").replace("'", "&apos;")
                            .replace("<", "&lt;").replace("\"", "&quot;"))
                    .append("'"));
            return this;
        }

        public ElementBuilder children(String... fragments) {
            for (String fragment : fragments) {
                children.append(fragment);
            }
            hasChildren |= fragments.length > 0;
            return this;
        }

        public ElementBuilder children(ElementBuilder... elements) {
            for (ElementBuilder element : elements) {
                children(element.build());
            }
            return this;
        }

        public String build() {
            String start = "<" + name + attributes;
            if (!hasChildren) return start + "/>";
            return start + ">" + children + "</" + name + ">";
        }
    }
}
