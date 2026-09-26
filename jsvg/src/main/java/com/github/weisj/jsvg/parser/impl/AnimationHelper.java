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

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.animation.value.AnimatedColor;
import com.github.weisj.jsvg.animation.value.AnimatedFloatList;
import com.github.weisj.jsvg.animation.value.AnimatedLength;
import com.github.weisj.jsvg.animation.value.AnimatedPaint;
import com.github.weisj.jsvg.animation.value.AnimatedPercentage;
import com.github.weisj.jsvg.animation.value.AnimatedTransform;
import com.github.weisj.jsvg.animation.value.NeutralElements;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.ColorValue;
import com.github.weisj.jsvg.attributes.value.ConstantLengthTransform;
import com.github.weisj.jsvg.attributes.value.FloatListValue;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.BaseAnimationNode;
import com.github.weisj.jsvg.paint.SVGPaint;

/** Builds animated attribute values from the animation children of an element. */
public final class AnimationHelper {
    private AnimationHelper() {}

    public static @Nullable AnimatedLength animatedLength(@NotNull AttributeNode attributeNode,
            @NotNull String property, @NotNull LengthValue initial, @NotNull PercentageDimension dimension) {
        return makeAnimated(
                animationNodes(attributeNode, property, Animate.class), initial,
                (animate, currentInitial) -> animate.animatedLength(currentInitial, dimension, attributeNode));
    }

    static @Nullable LengthValue applyLengthAnimation(@NotNull AttributeNode attributeNode,
            @NotNull String property, @NotNull PercentageDimension dimension, @Nullable LengthValue value,
            @NotNull Inherited inherited) {
        LengthValue initial = value != null ? value : inheritedLengthValue(inherited);
        if (initial instanceof AnimatedLength) {
            initial = ((AnimatedLength) initial).initial();
        }
        AnimatedLength animatedLength = animatedLength(attributeNode, property, initial, dimension);
        return animatedLength != null ? animatedLength : value;
    }

    public static @NotNull FloatListValue applyFloatListAnimation(@NotNull AttributeNode attributeNode,
            @NotNull String property, @Nullable FloatListValue value, @NotNull Inherited inherited) {
        FloatListValue initial = value;
        if (initial == null) {
            if (inherited == Inherited.YES) {
                throw new IllegalStateException("Inherited values for float lists aren't implemented yet");
            }
            initial = NeutralElements.NEUTRAL_FLOAT_LIST;
        }
        AnimatedFloatList animated = animatedFloatList(attributeNode, property, initial);
        return animated != null ? animated : initial;
    }

    static @Nullable AnimatedFloatList animatedFloatList(@NotNull AttributeNode attributeNode,
            @NotNull String property, @NotNull FloatListValue initial) {
        return makeAnimated(
                animationNodes(attributeNode, property, Animate.class), initial,
                // NOTE: For some configurations the compiler needs these explicit parameter types.
                (Animate animate, FloatListValue currentInitial) -> animate.animatedFloatList(currentInitial,
                        attributeNode));
    }

    static @Nullable PercentageValue applyPercentageAnimation(@NotNull AttributeNode attributeNode,
            @NotNull String property, @Nullable PercentageValue value, @NotNull Inherited inherited) {
        PercentageValue initial = value != null ? value : inheritedPercentageValue(inherited);
        if (initial instanceof AnimatedPercentage) {
            initial = ((AnimatedPercentage) initial).initial();
        }
        AnimatedPercentage animated = animatedPercentage(attributeNode, property, initial);
        return animated != null ? animated : value;
    }

    static @Nullable AnimatedPercentage animatedPercentage(@NotNull AttributeNode attributeNode,
            @NotNull String property, @NotNull PercentageValue initial) {
        return makeAnimated(
                animationNodes(attributeNode, property, Animate.class), initial,
                (animate, currentInitial) -> animate.animatedPercentage(currentInitial, attributeNode));
    }

    static @Nullable AnimatedPaint animatedPaint(@NotNull AttributeNode attributeNode,
            @NotNull String property, @NotNull SVGPaint initial) {
        return makeAnimated(
                animationNodes(attributeNode, property, Animate.class), initial,
                (animate, currentInitial) -> animate.animatedPaint(currentInitial, attributeNode));
    }

    static @Nullable AnimatedColor animatedColor(@NotNull AttributeNode attributeNode,
            @NotNull String property, @NotNull ColorValue initial) {
        return makeAnimated(
                animationNodes(attributeNode, property, Animate.class), initial,
                (animate, currentInitial) -> animate.animatedColor(currentInitial, attributeNode));
    }

    static @Nullable TransformValue applyTransformAnimation(@NotNull AttributeNode attributeNode,
            @NotNull String property, @Nullable TransformValue value, @NotNull Inherited inherited) {
        TransformValue initial = value != null ? value : inheritedTransformValue(inherited);
        if (initial instanceof AnimatedTransform) {
            initial = ((AnimatedTransform) initial).initial();
        }
        AnimatedTransform animated = animatedTransform(attributeNode, property, initial);
        return animated != null ? animated : value;
    }

    static @Nullable AnimatedTransform animatedTransform(@NotNull AttributeNode attributeNode,
            @NotNull String property, @NotNull TransformValue initial) {
        return makeAnimated(
                animationNodes(attributeNode, property, AnimateTransform.class), initial,
                (animate, currentInitial) -> animate.animatedTransform(currentInitial, attributeNode));
    }

    private static @NotNull LengthValue inheritedLengthValue(@NotNull Inherited inherited) {
        return inherited == Inherited.YES ? Length.INHERITED : NeutralElements.NEUTRAL_LENGTH;
    }

    private static @NotNull PercentageValue inheritedPercentageValue(@NotNull Inherited inherited) {
        return inherited == Inherited.YES ? Percentage.INHERITED : NeutralElements.NEUTRAL_PERCENTAGE;
    }

    private static @NotNull TransformValue inheritedTransformValue(@NotNull Inherited inherited) {
        return inherited == Inherited.YES
                ? ConstantLengthTransform.INHERITED
                : NeutralElements.NEUTRAL_TRANSFORM;
    }

    private static <T extends BaseAnimationNode> @NotNull List<T> animationNodes(
            @NotNull AttributeNode attributeNode, @NotNull String property, @NotNull Class<T> type) {
        List<ParsedElement> parsedElements = attributeNode.element().animationElements().get(property);
        if (parsedElements == null || parsedElements.isEmpty()) return Collections.emptyList();

        List<T> animationNodes = parsedElements.stream()
                .filter(n -> type.isInstance(n.node()))
                .map(n -> type.cast(n.nodeEnsuringBuildStatus(
                        attributeNode.document().currentNestingDepth(), ParsedElement.BuildMode.ALL)))
                .filter(n -> n != null)
                .collect(Collectors.toList());
        for (T animationNode : animationNodes) {
            attributeNode.document().registerAnimatedElement(animationNode);
        }
        return animationNodes;
    }

    private static <T, A extends T, N extends BaseAnimationNode> @Nullable A makeAnimated(
            @NotNull List<N> animationNodes,
            @NotNull T initial,
            @NotNull BiFunction<@NotNull N, @NotNull T, A> factory) {
        if (animationNodes.isEmpty()) return null;

        @NotNull T currentInitial = initial;
        @Nullable A lastAnimationValue = null;
        for (N animate : animationNodes) {
            A animated = factory.apply(animate, currentInitial);
            if (animated != null) {
                currentInitial = animated;
                lastAnimationValue = animated;
            }
        }
        return lastAnimationValue;
    }
}
