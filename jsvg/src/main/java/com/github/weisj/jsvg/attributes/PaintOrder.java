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
package com.github.weisj.jsvg.attributes;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.css.data.ComponentValue;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.AttributeParser;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import com.github.weisj.jsvg.util.AttributeUtil;

public final class PaintOrder {

    public static final PaintOrder NORMAL = new PaintOrder(Phase.FILL, Phase.STROKE, Phase.MARKERS);

    public enum Phase {
        FILL,
        STROKE,
        MARKERS
    }

    private final @NotNull Phase[] phases;

    public PaintOrder(@NotNull Phase... phases) {
        this.phases = phases;
    }

    public @NotNull Phase[] phases() {
        return phases;
    }

    public static @Nullable PaintOrder parse(@NotNull AttributeNode attributeNode) {
        @NotNull AttributeParser parser = attributeNode.parser();

        List<List<ComponentValue>> rawPhases =
                attributeNode.getSplitTokenList("paint-order", SeparatorMode.COMMA_AND_WHITESPACE);
        if (rawPhases == null || rawPhases.isEmpty()) return null;
        if (rawPhases.size() == 1) {
            ComponentValue only = AttributeParser.singleToken(rawPhases.get(0));
            if (only != null && only.isOneOfKeywords("inherit")) return null;
            if (only != null && only.isOneOfKeywords("none", "normal")) return NORMAL;
        }

        Phase[] phases = new Phase[3];
        int length = Math.min(phases.length, rawPhases.size());
        int phasesIndex = 0;
        int rawPhasesIndex = 0;
        while (phasesIndex < length && rawPhasesIndex < length) {
            Phase phase = parser.parseEnum(rawPhases.get(rawPhasesIndex), Phase.class);
            if (phase != null && !AttributeUtil.arrayContains(phases, phase)) {
                phases[phasesIndex] = phase;
                phasesIndex++;
            }
            rawPhasesIndex++;
        }
        while (phasesIndex < 3) {
            // Fill up with normal order
            phases[phasesIndex] = findNextInNormalOrder(phases, phasesIndex);
            phasesIndex++;
        }
        return new PaintOrder(phases);
    }

    private static @NotNull Phase findNextInNormalOrder(@NotNull Phase[] phases, int maxIndex) {
        for (Phase phase : NORMAL.phases()) {
            boolean found = false;
            for (int i = 0; i < maxIndex; i++) {
                if (phases[i] == phase) {
                    found = true;
                    break;
                }
            }
            if (!found) return phase;
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaintOrder that = (PaintOrder) o;
        return Arrays.equals(phases, that.phases);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(phases);
    }

    @Override
    public String toString() {
        return "PaintOrder" + Arrays.toString(phases);
    }
}
