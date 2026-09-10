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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.impl.FullCssParser;
import com.github.weisj.jsvg.parser.css.impl.phase1lexer.Lexer;
import com.github.weisj.jsvg.parser.css.impl.phase1lexer.LexerInput;

interface ComponentValueCursor {

    @NotNull
    ComponentValue next();

    @NotNull
    ComponentValue peek();

    void reconsume();

    static ComponentValueCursor from(@NotNull BasicParserInput input) {
        if (input instanceof LexerInput) {
            return new ComponentValueParser((LexerInput) input);
        } else if (input instanceof BasicParserInput.ComponentValuesInput) {
            return new PassThroughParser((BasicParserInput.ComponentValuesInput) input);
        } else {
            throw new IllegalArgumentException("Unsupported input type: " + input.getClass());
        }
    }

    /**
     * Stream of {@link Token} -> Stream of {@link ComponentValue}
     * <p>
     * Source backed by a {@link Lexer}. Implements §5.4.7 (consume a component value), §5.4.8
     * (consume a simple block) and §5.4.9 (consume a function) inline so bracket and function
     * tokens are converted into the appropriate {@link ComponentValue.SimpleBlock} or
     * {@link ComponentValue.FunctionBlock}.
     */
    class ComponentValueParser implements ComponentValueCursor {
        private final @NotNull Lexer lexer;
        private @Nullable ComponentValue saved;
        private @Nullable ComponentValue current;
        /** Pushback for the underlying token stream, used while building component values. */
        private @Nullable Token savedToken;

        public ComponentValueParser(@NotNull LexerInput input) {
            this.lexer = new Lexer(input);
        }

        @Override
        public @NotNull ComponentValue next() {
            if (saved != null) {
                current = saved;
                saved = null;
                return current;
            }
            current = consumeComponentValue();
            return current;
        }

        @Override
        public @NotNull ComponentValue peek() {
            ComponentValue next = next();
            reconsume();
            return next;
        }

        @Override
        public void reconsume() {
            if (current == null) {
                throw new IllegalStateException("nothing to reconsume");
            }
            saved = current;
            current = null;
        }

        private @NotNull Token nextToken() {
            if (savedToken != null) {
                Token t = savedToken;
                savedToken = null;
                return t;
            }
            return lexer.nextToken();
        }

        private void reconsumeToken(@NotNull Token t) {
            savedToken = t;
        }

        /** §5.4.7 Consume a component value. */
        private @NotNull ComponentValue consumeComponentValue() {
            Token t = nextToken();
            switch (t.type()) {
                case LEFT_BRACE:
                    return new ComponentValue.SimpleBlock.Brace(consumeBlockBody(Token.Static.RIGHT_BRACE, "{}"));
                case LEFT_BRACKET:
                    return new ComponentValue.SimpleBlock.Bracket(consumeBlockBody(Token.Static.RIGHT_BRACKET, "[]"));
                case LEFT_PAREN:
                    return new ComponentValue.SimpleBlock.Paren(consumeBlockBody(Token.Static.RIGHT_PAREN, "()"));
                case FUNCTION:
                    return consumeFunction(((Token.Function) t).name());
                default:
                    return t;
            }
        }

        /**
         * §5.4.8 Consume a simple block (body). The opening token has already been consumed; this
         * collects component values until {@code endType} (the matching closer) or EOF.
         */
        private @NotNull List<@NotNull ComponentValue> consumeBlockBody(
                @NotNull Token endToken, @NotNull String label) {
            List<ComponentValue> values = new ArrayList<>();
            while (true) {
                Token t = nextToken();
                if (t == endToken) {
                    return values;
                }
                if (t == Token.Static.EOF) {
                    FullCssParser.logParseEvent("unexpected EOF inside " + label + "-block");
                    return values;
                }
                reconsumeToken(t);
                values.add(consumeComponentValue());
            }
        }

        /** §5.4.9 Consume a function. The function-token has already been consumed. */
        private @NotNull ComponentValue.FunctionBlock consumeFunction(@NotNull String name) {
            List<ComponentValue> values = new ArrayList<>();
            while (true) {
                Token t = nextToken();
                if (t == Token.Static.RIGHT_PAREN) {
                    return new ComponentValue.FunctionBlock(name, values);
                }
                if (t == Token.Static.EOF) {
                    FullCssParser.logParseEvent("unexpected EOF inside " + name + "()");
                    return new ComponentValue.FunctionBlock(name, values);
                }
                reconsumeToken(t);
                values.add(consumeComponentValue());
            }
        }
    }

    /** Stream of {@link ComponentValue} -> Stream of {@link ComponentValue} */
    final class PassThroughParser implements ComponentValueCursor {
        private final @NotNull List<? extends @NotNull ComponentValue> values;
        private int index = -1;

        public PassThroughParser(@NotNull BasicParserInput.ComponentValuesInput input) {
            values = input.values;
        }

        @Override
        public @NotNull ComponentValue next() {
            index++;
            if (index >= values.size()) {
                return Token.Static.EOF;
            }
            return values.get(index);
        }

        @Override
        public @NotNull ComponentValue peek() {
            if (index + 1 >= values.size()) {
                return Token.Static.EOF;
            }
            return values.get(index + 1);
        }

        @Override
        public void reconsume() {
            if (index == -1) {
                throw new IllegalStateException("nothing to reconsume");
            }
            index--;
        }
    }
}
