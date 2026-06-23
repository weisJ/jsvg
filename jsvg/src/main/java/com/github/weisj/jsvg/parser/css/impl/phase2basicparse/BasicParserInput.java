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
package com.github.weisj.jsvg.parser.css.impl.phase2basicparse;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.impl.phase1lexer.LexerInput;

public interface BasicParserInput {
    static BasicParserInput fromSegments(@NotNull List<char[]> input) {
        return new CharArrayListInput(input);
    }

    static BasicParserInput fromString(@NotNull String input) {
        return new StringInput(input);
    }

    static BasicParserInput fromComponentValues(@NotNull List<@NotNull ComponentValue> input) {
        return new ComponentValuesInput(input);
    }

    class CharArrayListInput implements BasicParserInput, LexerInput {
        public final @NotNull List<char[]> input;

        private CharArrayListInput(@NotNull List<char[]> input) {
            this.input = input;
        }

        @Override
        public @NotNull List<char[]> segments() {
            return input;
        }
    }
    class StringInput implements BasicParserInput, LexerInput {
        public final @NotNull String input;

        private StringInput(@NotNull String input) {
            this.input = input;
        }

        @Override
        public @NotNull List<char[]> segments() {
            return Collections.singletonList(input.toCharArray());
        }
    }
    class ComponentValuesInput implements BasicParserInput {
        public final @NotNull List<@NotNull ComponentValue> values;

        private ComponentValuesInput(@NotNull List<@NotNull ComponentValue> values) {
            this.values = values;
        }
    }
}
