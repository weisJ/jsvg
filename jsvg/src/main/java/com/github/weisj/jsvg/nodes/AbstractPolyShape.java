/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Jannis Weis
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
package com.github.weisj.jsvg.nodes;

import java.awt.*;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.animation.value.AnimatedFloatList;
import com.github.weisj.jsvg.animation.value.AnimatedPath;
import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.value.ConstantFloatList;
import com.github.weisj.jsvg.attributes.value.ConstantValue;
import com.github.weisj.jsvg.attributes.value.FloatListValue;
import com.github.weisj.jsvg.geometry.*;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.util.PathUtil;

public abstract class AbstractPolyShape extends ShapeNode {

    @Override
    protected final @NotNull MeasurableShape buildShape(@NotNull AttributeNode attributeNode) {
        FloatListValue points = attributeNode.getFloatList("points", Inherited.NO, Animatable.YES);
        if (points instanceof AnimatedFloatList) {
            return new FillRuleAwareAWTSVGShape(new AnimatedPath((AnimatedFloatList) points, doClose()));
        }

        float[] pointsArray = ((ConstantFloatList) points).value();
        if (pointsArray.length > 0) {
            return new FillRuleAwareAWTSVGShape(
                    new ConstantValue<>(PathUtil.setPolyLine(null, pointsArray, doClose())));
        } else {
            return new AWTSVGShape<>(new Rectangle());
        }
    }

    protected abstract boolean doClose();
}
