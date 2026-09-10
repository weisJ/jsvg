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
package com.github.weisj.jsvg.parser.css.data;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.data.selectors.SelectorList;
import com.google.errorprone.annotations.Immutable;

/**
 * Rule following <a href="https://www.w3.org/TR/css-syntax-3/#parsing">CSS Syntax Module Level 3, §5</a>.
 * Per §5.2 either a {@link QualifiedRule} (§5.4.3) or an {@link AtRule} (§5.4.2). Preludes are kept in
 * unparsed component-value form; turning them into {@link SelectorList}s, media-query lists, etc. is a
 * separate pass.
 */
@Immutable
public interface Rule {

    /** A qualified rule (§5.4.2): {@code <prelude> { <block> }}. In SVG, this is a style rule. */
    @Immutable
    final class QualifiedRule implements Rule {
        private final @NotNull List<? extends @NotNull ComponentValue> prelude;
        private final @NotNull ComponentValue.SimpleBlock.Brace block;

        /** Takes ownership of {@code prelude}. */
        public QualifiedRule(@NotNull List<? extends @NotNull ComponentValue> prelude,
                @NotNull ComponentValue.SimpleBlock.Brace block) {
            this.prelude = prelude;
            this.block = block;
        }

        /** Component values between the start of the rule and the {@code {} block. The selector list, unparsed. */
        public @NotNull List<? extends @NotNull ComponentValue> prelude() {
            return prelude;
        }

        /** The {@code {}} block delimiting the declaration list. */
        public @NotNull ComponentValue.SimpleBlock.Brace block() {
            return block;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof QualifiedRule)) return false;
            QualifiedRule that = (QualifiedRule) o;
            return prelude.equals(that.prelude) && block.equals(that.block);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prelude, block);
        }

        @Override
        public String toString() {
            return "QualifiedRule{prelude=" + prelude + ", block=" + block + "}";
        }
    }

    /** An at-rule (§5.4.2): {@code @<name> <prelude> [ ; | { <block> } ]}. */
    @Immutable
    final class AtRule implements Rule, DeclarationListItem {
        private final @NotNull String name;
        private final @NotNull List<? extends @NotNull ComponentValue> prelude;
        private final @Nullable ComponentValue.SimpleBlock.Brace block;

        /** Takes ownership of {@code prelude}. */
        public AtRule(@NotNull String name,
                @NotNull List<? extends @NotNull ComponentValue> prelude,
                @Nullable ComponentValue.SimpleBlock.Brace block) {
            this.name = name;
            this.prelude = prelude;
            this.block = block;
        }

        /** The at-keyword name without the leading {@code @} (e.g. {@code "media"}, {@code "font-face"}). */
        public @NotNull String name() {
            return name;
        }

        /** Component values between the at-keyword and the block (or terminating {@code ;}). */
        public @NotNull List<? extends @NotNull ComponentValue> prelude() {
            return prelude;
        }

        /** The block, or {@code null} if the at-rule was statement-form (terminated by {@code ;}). */
        public @Nullable ComponentValue.SimpleBlock.Brace block() {
            return block;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AtRule)) return false;
            AtRule that = (AtRule) o;
            return name.equals(that.name) && prelude.equals(that.prelude) && Objects.equals(block, that.block);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, prelude, block);
        }

        @Override
        public String toString() {
            return "AtRule{name='" + name + "', prelude=" + prelude + ", block=" + block + "}";
        }
    }
}
