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
package com.github.weisj.jsvg.parser.css.impl.phase3ruleparse;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.css.data.Token;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParser;
import com.github.weisj.jsvg.parser.css.impl.phase2basicparse.BasicParserInput;
import com.github.weisj.jsvg.renderer.CssHints;

/**
 * Evaluates a {@code @media} prelude against {@link CssHints} per
 * <a href="https://drafts.csswg.org/mediaqueries-5">Media Queries Level 5</a>. Supports only media
 * types and {@code prefers-color-scheme}; any other feature yields {@link Match#UNKNOWN}. Boolean
 * combination is three-valued (Kleene) so {@code not (unknown)} stays unknown (§ {@code <general-enclosed>}
 * forward-compat). Recursive descent over the phase-2-flattened prelude. Never throws: a malformed
 * prelude makes {@link #matches} return {@code false}, dropping the at-rule.
 */
public final class MediaAtRuleConditionEvaluator {

    /** Three-valued (Kleene) result of evaluating a media query or condition. */
    private enum Match {
        TRUE,
        FALSE,
        UNKNOWN;

        @NotNull
        Match not() {
            if (this == TRUE) {
                return FALSE;
            }
            if (this == FALSE) {
                return TRUE;
            }
            return UNKNOWN;
        }

        @NotNull
        Match and(@NotNull Match o) {
            if (this == FALSE || o == FALSE) {
                return FALSE;
            }
            if (this == UNKNOWN || o == UNKNOWN) {
                return UNKNOWN;
            }
            return TRUE;
        }

        @NotNull
        Match or(@NotNull Match o) {
            if (this == TRUE || o == TRUE) {
                return TRUE;
            }
            if (this == UNKNOWN || o == UNKNOWN) {
                return UNKNOWN;
            }
            return FALSE;
        }

        static Match from(boolean b) {
            return b ? TRUE : FALSE;
        }
    }

    private final @NotNull CssHints hints;
    private final @NotNull ComponentValueGrammarParser parser;

    private MediaAtRuleConditionEvaluator(@NotNull List<@NotNull ComponentValue> cv, @NotNull CssHints hints) {
        this.hints = hints;
        this.parser = new ComponentValueGrammarParser(cv, true);
    }

    /** @return {@code true} if the prelude matches; {@code false} if it does not or any query is malformed. */
    public static boolean matches(@NotNull List<@NotNull ComponentValue> prelude, @NotNull CssHints hints) {
        List<List<ComponentValue>> queries = BasicParser.parseCommaSeparatedListOfComponentValues(
                BasicParserInput.fromComponentValues(ComponentValueGrammarParser.stripWhitespace(prelude)));
        if (queries.isEmpty()) {
            return true; // bare "@media { ... }" == "all"
        }
        Match aggregate = Match.FALSE;
        for (List<ComponentValue> query : queries) {
            try {
                Match m = new MediaAtRuleConditionEvaluator(query, hints).evalMediaQuery();
                aggregate = aggregate.or(m); // a query list matches if ANY query matches
            } catch (ParseException e) {
                // skipped according to https://drafts.csswg.org/mediaqueries-5/#error-handling
            }
        }
        return aggregate == Match.TRUE;
    }

    /**
     * {@code <media-query> = <media-condition>
     *               | [ not | only ]? <media-type> [ and <media-condition-without-or> ]? }
     *
     * @return <ul>
     *     <li>{@code Match.TRUE}/{@code Match.FALSE} if the {@code CssHints} were sufficient to evaluate
     *     the condition</li>
     *     <li>{@code Match.UNKNOWN} if the {@code CssHints} were insufficient to evaluate the condition</li>
     * </ul>
     */
    private @NotNull Match evalMediaQuery() throws ParseException {
        if (parser.isEof()) {
            throw new ParseException(); // empty query, e.g. "screen,,print"
        }

        // Pure media-condition: starts with a '(' block, or 'not' followed by a '(' block.
        if (startsAMediaCondition(parser.current(), parser.peekNext())) {
            Match m = evalMediaCondition();
            if (!parser.isEof()) {
                throw new ParseException();
            }
            return m;
        }

        boolean negate = false;
        if (parser.isCurrentOneOfKeywords("not")) {
            negate = true;
            parser.advance();
        } else if (parser.isCurrentOneOfKeywords("only")) {
            parser.advance(); // legacy old-browser guard; no effect on evaluation
        }

        Match result = evalMediaType();
        if (parser.isCurrentOneOfKeywords("and")) {
            parser.advance();
            result = result.and(evalMediaConditionWithoutOr());
        }
        if (!parser.isEof()) {
            throw new ParseException();
        }
        return negate ? result.not() : result;
    }

    private static boolean startsAMediaCondition(@NotNull ComponentValue token, @Nullable ComponentValue nextToken) {
        return startsAMediaNot(token, nextToken) || startsAMediaInParens(token);
    }

    private @NotNull Match evalMediaNot() throws ParseException {
        if (parser.isCurrentOneOfKeywords("not")) {
            throw new ParseException();
        }
        parser.advance();
        return evalMediaInParens().not();
    }

    private static boolean startsAMediaNot(@NotNull ComponentValue token, @Nullable ComponentValue nextToken) {
        return token.isOneOfKeywords("not") && nextToken != null && startsAMediaInParens(nextToken);
    }

    private @NotNull Match evalMediaType() throws ParseException {
        if (parser.isEof()) {
            throw new ParseException();
        }
        if (!(parser.current() instanceof Token.Ident)) {
            throw new ParseException();
        }
        String keyword = ((Token.Ident) parser.current()).name();
        if (parser.isCurrentOneOfKeywords("and", "or", "not", "only")) {
            throw new ParseException(); // and/or/not/only cannot be a media type
        }
        parser.advance();
        if ("all".equalsIgnoreCase(keyword)) {
            return Match.TRUE;
        }
        if ("screen".equalsIgnoreCase(keyword)) {
            return Match.from(hints.mediaType() == CssHints.MediaType.SCREEN);
        }
        if ("print".equalsIgnoreCase(keyword)) {
            return Match.from(hints.mediaType() == CssHints.MediaType.PRINT);
        }
        return Match.FALSE; // tv, speech, projection, ... never match
    }

    // <media-condition> = not <in-parens> | <in-parens> [ <and>* | <or>* ]
    private @NotNull Match evalMediaCondition() throws ParseException {
        if (parser.isEof()) {
            throw new ParseException();
        }
        if (startsAMediaNot(parser.current(), parser.peekNext())) {
            return evalMediaNot();
        }
        Match m = evalMediaInParens();
        if (parser.isCurrentOneOfKeywords("and")) {
            while (parser.isCurrentOneOfKeywords("and")) {
                parser.advance();
                m = m.and(evalMediaInParens());
            }
        } else if (parser.isCurrentOneOfKeywords("or")) {
            while (parser.isCurrentOneOfKeywords("or")) {
                parser.advance();
                m = m.or(evalMediaInParens());
            }
        }
        // Mixing 'and' with 'or' without parentheses leaves a stray keyword; the caller's
        // end-of-input check then rejects the query as malformed.
        return m;
    }

    // <media-condition-without-or> = not <in-parens> | <in-parens> <and>*
    private @NotNull Match evalMediaConditionWithoutOr() throws ParseException {
        if (parser.isEof()) {
            throw new ParseException();
        }
        if (startsAMediaNot(parser.current(), parser.peekNext())) {
            return evalMediaNot();
        }
        Match m = evalMediaInParens();
        while (parser.isCurrentOneOfKeywords("and")) {
            parser.advance();
            m = m.and(evalMediaInParens());
        }
        return m;
    }

    // <media-in-parens> = ( <media-condition> ) | ( <media-feature> ) | <general-enclosed>
    // <general-enclosed> = [ <function-token> <any-value>? ) ] | [ ( <any-value>? ) ]
    // <general-enclosed> is for future compatibility and currently always returns Match.UNKNOWN
    private @NotNull Match evalMediaInParens() throws ParseException {
        if (parser.isEof()) {
            throw new ParseException();
        }
        if (parser.current() instanceof ComponentValue.SimpleBlock.Paren) {
            List<ComponentValue> inner = ComponentValueGrammarParser.stripWhitespace(
                    ((ComponentValue.SimpleBlock.Paren) parser.current()).value());
            parser.advance();
            if (inner.isEmpty()) {
                return Match.UNKNOWN; // <general-enclosed> allows () but are not supported
            }
            if (startsAMediaCondition(inner.get(0), inner.size() >= 2 ? inner.get(1) : null)) {
                MediaAtRuleConditionEvaluator sub = new MediaAtRuleConditionEvaluator(inner, hints);
                Match m = sub.evalMediaCondition();
                if (!sub.parser.isEof()) {
                    throw new ParseException();
                }
                return m;
            }
            MediaAtRuleConditionEvaluator sub = new MediaAtRuleConditionEvaluator(inner, hints);
            Match m = sub.evalMediaFeature();
            if (!sub.parser.isEof()) {
                throw new ParseException();
            }
            return m;
        }
        if (parser.current() instanceof ComponentValue.SimpleBlock.FunctionBlock) {
            return Match.UNKNOWN; // <general-enclosed> allows function blocks but are not supported
        }
        throw new ParseException(); // nothing else is allowed
    }

    private static boolean startsAMediaInParens(@NotNull ComponentValue token) {
        return token instanceof ComponentValue.SimpleBlock.Paren
                || token instanceof ComponentValue.FunctionBlock;
    }

    private Match evalMediaFeature() throws ParseException {
        if (parser.isEof()) {
            throw new ParseException();
        }

        if (startsAMediaPlain(parser.current(), parser.peekNext())) {
            return evalMediaPlain();
        }

        if (parser.current() instanceof Token.Ident && parser.peekNext() == null) {
            // <mf-boolean>
            @NotNull Token.Ident featureName = (Token.Ident) parser.current();
            parser.advance();
            if (featureName.isOneOfKeywords("prefers-color-scheme")) {
                return Match.TRUE; // <mf-value> is not provided -> both user settings result in a match
            }
            return Match.UNKNOWN; // unsupported feature name
        }

        // <mf-range>
        return evalMediaRange();
    }

    private boolean startsAMediaFeatureValue(@NotNull ComponentValue token) {
        return token instanceof Token.Number
                || token instanceof Token.Dimension
                || token instanceof Token.Ident;
    }

    // only prefers-color-scheme is supported, with possible values light|dark
    // all others return Match.UNKNOWN (this method returns null to mark that)
    private @Nullable Token.Ident parseMediaFeatureValueIdentOnly() throws ParseException {
        if (parser.current() instanceof Token.Ident) {
            Token.Ident featureValue = (Token.Ident) parser.current();
            parser.advance();
            return featureValue;
        }
        if (parser.current() instanceof Token.Dimension) {
            parser.advance();
        }
        if (parser.current() instanceof Token.Number) {
            parser.advance();
            if (!parser.isEof() && parser.current().isSlash()) { // a <ratio>
                parser.advance(); // skips over "/"
                if (parser.isEof() || !(parser.current() instanceof Token.Number)) {
                    throw new ParseException();
                }
                parser.advance(); // skips over the second number
            }
        }
        return null;
    }

    private boolean startsAMediaPlain(@NotNull ComponentValue token, @Nullable ComponentValue nextToken) {
        return token instanceof Token.Ident && nextToken != null && nextToken.isColon();
    }

    private @NotNull Match evalMediaPlain() throws ParseException {
        @NotNull Token.Ident featureName = (Token.Ident) parser.current();
        parser.advance(); // consume the ident
        parser.advance(); // consume the colon
        if (parser.isEof() || !startsAMediaFeatureValue(parser.current())) {
            throw new ParseException();
        }
        @Nullable Token.Ident featureValue = parseMediaFeatureValueIdentOnly();
        if (featureValue == null) {
            return Match.UNKNOWN; // unsupported feature value
        }
        if (featureName.isOneOfKeywords("prefers-color-scheme")) {
            if (featureValue.isOneOfKeywords(hints.colorScheme().name())) {
                return Match.TRUE;
            } else {
                return Match.FALSE;
            }
        }
        if (!parser.isEof()) {
            throw new ParseException();
        }
        return Match.UNKNOWN; // unsupported feature name
    }

    // always returns Match.UNKNOWN because the supported media features don't have ranges
    private @NotNull Match evalMediaRange() throws ParseException {
        if (parser.isEof()) {
            throw new ParseException();
        }
        parseMediaFeatureValueIdentOnly();
        parseComparisonOperator();
        parseMediaFeatureValueIdentOnly();
        if (!parser.isEof() && startsAComparisonOperator(parser.current())) {
            parseComparisonOperator();
            parseMediaFeatureValueIdentOnly();
        }
        if (!parser.isEof()) {
            throw new ParseException();
        }
        return Match.UNKNOWN; // the supported media features don't have ranges
    }

    private enum ComparisonOperator {
        LessThan,
        LessThanOrEqual,
        Equal,
        GreaterThanOrEqual,
        GreaterThan;
    }

    private boolean startsAComparisonOperator(@NotNull ComponentValue token) {
        return token.isEq() || token.isLt() || token.isGt();
    }

    private @NotNull ComparisonOperator parseComparisonOperator() throws ParseException {
        if (parser.isEof()) {
            throw new ParseException();
        }
        if (parser.current().isEq()) {
            parser.advance();
            return ComparisonOperator.Equal;
        }
        if (parser.current().isLt()) {
            parser.advance();
            if (!parser.isEof() && parser.current().isEq()) {
                parser.advance();
                return ComparisonOperator.LessThanOrEqual;
            }
            return ComparisonOperator.LessThan;
        }
        if (parser.current().isGt()) {
            parser.advance();
            if (!parser.isEof() && parser.current().isEq()) {
                parser.advance();
                return ComparisonOperator.GreaterThanOrEqual;
            }
            return ComparisonOperator.GreaterThan;
        }
        throw new ParseException();
    }

    private static class ParseException extends Exception {
    }
}
