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
package com.github.weisj.jsvg.parser.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;

public interface AttributeValue {

    /** For values of SVG-only attributes that don't need a CSS parser */
    final class PlainString implements AttributeValue {
        private final @NotNull String value;

        public PlainString(@NotNull String value) {
            this.value = value;
        }

        public @NotNull String string() {
            return value;
        }
    }

    /** For values of CSS attributes */
    final class Parsed implements AttributeValue {
        private final @NotNull List<@NotNull ComponentValue> tokens;

        public Parsed(@NotNull List<@NotNull ComponentValue> tokens) {
            this.tokens = tokens;
        }

        public @NotNull List<@NotNull ComponentValue> tokens() {
            return tokens;
        }

        /** Serializes a component-value list back to CSS text (§5.2).
         * Expensive operation; only use it as a fallback.
         * Prefer to use {@link #tokens()} directly. */
        public @NotNull String reserialize() {
            StringBuilder sb = new StringBuilder();
            for (ComponentValue token : tokens) {
                sb.append(token.serialize());
            }
            return sb.toString().trim();
        }
    }
}
