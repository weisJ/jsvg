/*
 * MIT License
 *
 * Copyright (c) 2021 Jannis Weis
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

import java.awt.geom.GeneralPath;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.AttributeNode;
import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.Inherit;
import com.github.weisj.jsvg.geometry.AWTSVGShape;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.nodes.path.BuildHistory;
import com.github.weisj.jsvg.nodes.path.PathCommand;
import com.github.weisj.jsvg.nodes.path.PathParser;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;

@ElementCategories({Category.Graphic, Category.Shape})
@PermittedContent(categories = {Category.Animation, Category.Descriptive})
public final class Path extends ShapeNode {
    public static final String TAG = "path";

    @Override
    public final @NotNull String tagName() {
        return TAG;
    }

    @Override
    protected @NotNull SVGShape buildShape(@NotNull AttributeNode attributeNode) {
        FillRule fillRule = FillRule.parse(attributeNode.getValue("fill-rule", Inherit.Yes));
        String pathValue = attributeNode.getValue("d");
        PathCommand[] pathCommands = pathValue != null
                ? new PathParser(pathValue).parsePathCommand()
                : new PathCommand[0];

        int nodeCount = 2;
        for (PathCommand pathCommand : pathCommands) {
            nodeCount += pathCommand.getInnerNodes();
        }

        GeneralPath path = new GeneralPath(fillRule.awtWindingRule, nodeCount);
        BuildHistory hist = new BuildHistory();

        for (PathCommand pathCommand : pathCommands) {
            pathCommand.appendPath(path, hist);
        }

        return new AWTSVGShape(path);
    }
}
