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
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.css.StyleProperty;

final class CssRule {
    private final @NotNull List<@NotNull SelectorPart> selectorParts;
    private final @NotNull Specificity specificity;
    private final @NotNull Map<@NotNull String, @NotNull StyleProperty> properties;

    private CssRule(
            @NotNull List<@NotNull SelectorPart> selectorParts,
            @NotNull Specificity specificity,
            @NotNull Map<@NotNull String, @NotNull StyleProperty> properties) {
        this.selectorParts = selectorParts;
        this.specificity = specificity;
        this.properties = properties;
    }

    static @NotNull CssRule parse(@NotNull String selector,
            @NotNull Map<@NotNull String, @NotNull StyleProperty> properties) {
        List<@NotNull SelectorPart> selectorParts = new ArrayList<>();
        List<String> tokens = tokenize(selector);
        Combinator currentCombinator = Combinator.DESCENDANT;

        for (String token : tokens) {
            switch (token) {
                case ">":
                case "+":
                case "~":
                case " ":
                case "":
                    currentCombinator = Combinator.fromSymbol(token);
                    break;
                default:
                    SelectorType type = identifySelectorType(token);
                    selectorParts.add(new SelectorPart(type, token, currentCombinator));
                    break;
            }
        }
        return new CssRule(selectorParts, calculateSpecificity(selectorParts), properties);
    }

    private static @NotNull List<@NotNull String> tokenize(@NotNull String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        boolean skipWhitespace = false;
        boolean hasPrecedingCombinator = false;

        Runnable commitToken = () -> {
            if (token.length() > 0) {
                tokens.add(token.toString());
                token.setLength(0);
            }
        };

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            boolean isWhitespace = Character.isWhitespace(ch);

            if (skipWhitespace) {
                if (isWhitespace) continue;
                skipWhitespace = false;
                tokens.add(" ");
            }

            if (isWhitespace || ch == '>' || ch == '+' || ch == '~' || ch == '{' || ch == '}') {
                commitToken.run();

                if (isWhitespace) {
                    skipWhitespace = true;
                } else {
                    token.append(ch);
                }
                hasPrecedingCombinator = true;
            } else {
                if (ch == '.') {
                    commitToken.run();

                    if (!hasPrecedingCombinator) {
                        tokens.add("");
                    }
                }

                token.append(ch);
                hasPrecedingCombinator = false;
            }
        }

        commitToken.run();

        return tokens;
    }

    private static @NotNull SelectorType identifySelectorType(@NotNull String token) {
        if (token.startsWith("#")) {
            return SelectorType.ID;
        } else if (token.startsWith(".")) {
            return SelectorType.CLASS;
        } else if (token.startsWith(":")) {
            return SelectorType.PSEUDO_CLASS;
        } else if (token.startsWith("[")) {
            return SelectorType.ATTRIBUTE;
        } else {
            return SelectorType.ELEMENT;
        }
    }

    private static @NotNull Specificity calculateSpecificity(@NotNull List<@NotNull SelectorPart> selectorParts) {
        int idCount = 0;
        int classCount = 0;
        int elementCount = 0;

        for (SelectorPart part : selectorParts) {
            switch (part.type()) {
                case ID:
                    idCount++;
                    break;
                case CLASS:
                case PSEUDO_CLASS:
                case ATTRIBUTE:
                    classCount++;
                    break;
                case ELEMENT:
                    elementCount++;
                    break;
            }
        }

        return new Specificity(idCount, classCount, elementCount);
    }

    public @NotNull Map<@NotNull String, @NotNull StyleProperty> properties() {
        return properties;
    }

    public @NotNull Specificity specificity() {
        return specificity;
    }

    public @NotNull List<@NotNull SelectorPart> selectorParts() {
        return selectorParts;
    }

    public @NotNull String selectorString() {
        StringBuilder builder = new StringBuilder();
        for (SelectorPart part : selectorParts) {
            if (builder.length() > 0) {
                builder.append(part.combinator().symbol());
            }
            builder.append(part.value());
        }
        return builder.toString();
    }
}
