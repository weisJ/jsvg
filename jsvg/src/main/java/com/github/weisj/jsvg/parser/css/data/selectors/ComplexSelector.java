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
package com.github.weisj.jsvg.parser.css.data.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.impl.ParsedElement;
import com.google.errorprone.annotations.Immutable;

/**
 * A <a href="https://www.w3.org/TR/selectors-4/#complex">complex selector</a> (Selectors Level 4):
 * compound selectors (leftmost first) joined by combinators, matched right-to-left from
 * {@link #rightmostSequence()}. Invariant: {@code combinators.size() == sequences.size() - 1}, where
 * {@code combinators[i]} relates {@code sequences[i]} to {@code sequences[i+1]}.
 */
@Immutable
public final class ComplexSelector {

    private final @NotNull List<@NotNull CompoundSelector> sequences;
    private final @NotNull List<@NotNull Combinator> combinators;
    private final @NotNull Specificity specificity;

    public ComplexSelector(@NotNull List<@NotNull CompoundSelector> sequences,
            @NotNull List<@NotNull Combinator> combinators) {
        if (sequences.isEmpty()) {
            throw new IllegalArgumentException("complex selector must contain at least one compound selector");
        }
        if (combinators.size() != sequences.size() - 1) {
            throw new IllegalArgumentException(
                    "combinators.size() must equal sequences.size() - 1 (got "
                            + combinators.size() + " and " + sequences.size() + ")");
        }
        this.sequences = Collections.unmodifiableList(new ArrayList<>(sequences));
        this.combinators = Collections.unmodifiableList(new ArrayList<>(combinators));
        this.specificity = computeSpecificity();
    }

    public @NotNull List<@NotNull CompoundSelector> sequences() {
        return sequences;
    }

    public @NotNull List<@NotNull Combinator> combinators() {
        return combinators;
    }

    /** The rightmost compound selector — the one matched against the targeted element itself. */
    public @NotNull CompoundSelector rightmostSequence() {
        return sequences.get(sequences.size() - 1);
    }

    public @NotNull Specificity specificity() {
        return specificity;
    }

    /** Sum of the specificity contributions of every compound selector in the chain. */
    private @NotNull Specificity computeSpecificity() {
        int idSelectors = 0, classSelectors = 0, typeSelectors = 0;
        for (CompoundSelector compound : sequences) {
            for (SimpleSelector simple : compound.simpleSelectors()) {
                idSelectors += simple.specificity().idSelectors();
                classSelectors += simple.specificity().classSelectors();
                typeSelectors += simple.specificity().typeSelectors();
            }
        }
        return new Specificity(false, idSelectors, classSelectors, typeSelectors);
    }

    public @NotNull MatchResult matches(ParsedElement targetElement) {
        return matchesRecursive(sequences().size() - 1, targetElement);
    }

    /**
     * sequence at index must match element; recurses right-to-left, backtracking on descendant/sibling.
     * The returned {@link MatchResult#selectorsUseElementPositionInDom} flag is the OR over every
     * compound selector evaluated along the way, regardless of whether the chain ultimately matched.
     */
    private @NotNull MatchResult matchesRecursive(int indexInSequences, @NotNull ParsedElement targetElement) {
        MatchResult here = sequences().get(indexInSequences).matches(targetElement);
        if (!here.matches) {
            return new MatchResult(false, here.selectorsUseElementPositionInDom);
        }
        if (indexInSequences == 0) {
            return new MatchResult(true, here.selectorsUseElementPositionInDom);
        }
        Combinator combinator = combinators().get(indexInSequences - 1);
        int nextIndex = indexInSequences - 1;
        switch (combinator) {
            case DESCENDANT:
                for (ParsedElement ancestor = targetElement.parent(); ancestor != null; ancestor = ancestor.parent()) {
                    MatchResult sub = matchesRecursive(nextIndex, ancestor);
                    if (sub.matches) return new MatchResult(true, true);
                }
                return new MatchResult(false, true);
            case CHILD: {
                ParsedElement parent = targetElement.parent();
                if (parent == null) return new MatchResult(false, true);
                MatchResult sub = matchesRecursive(nextIndex, parent);
                return new MatchResult(sub.matches, true);
            }
            case NEXT_SIBLING: {
                ParsedElement previous = targetElement.previousSibling();
                if (previous == null) return new MatchResult(false, true);
                MatchResult sub = matchesRecursive(nextIndex, previous);
                return new MatchResult(sub.matches, true);
            }
            case SUBSEQUENT_SIBLING:
                for (ParsedElement sibling = targetElement.previousSibling(); sibling != null; sibling =
                        sibling.previousSibling()) {
                    MatchResult sub = matchesRecursive(nextIndex, sibling);
                    if (sub.matches) return new MatchResult(true, true);
                }
                return new MatchResult(false, true);
            default:
                throw new IllegalStateException("Unknown combinator: " + combinator);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplexSelector)) return false;
        ComplexSelector that = (ComplexSelector) o;
        return sequences.equals(that.sequences) && combinators.equals(that.combinators);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequences, combinators);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sequences.get(0));
        for (int i = 0; i < combinators.size(); i++) {
            sb.append(combinatorToString(combinators.get(i)));
            sb.append(sequences.get(i + 1));
        }
        return sb.toString();
    }

    private static @NotNull String combinatorToString(@NotNull Combinator c) {
        switch (c) {
            case DESCENDANT:
                return " ";
            case CHILD:
                return " > ";
            case NEXT_SIBLING:
                return " + ";
            case SUBSEQUENT_SIBLING:
                return " ~ ";
            default:
                throw new IllegalStateException("unknown combinator: " + c);
        }
    }
}
