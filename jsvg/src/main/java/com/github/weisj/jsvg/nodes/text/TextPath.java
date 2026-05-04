/*
 * MIT License
 *
 * Copyright (c) 2021-2026 Jannis Weis
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
package com.github.weisj.jsvg.nodes.text;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.text.GlyphRenderMethod;
import com.github.weisj.jsvg.attributes.text.Side;
import com.github.weisj.jsvg.attributes.text.Spacing;
import com.github.weisj.jsvg.attributes.text.TextAnchor;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.SVGShape;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.geometry.util.ReversePathIterator;
import com.github.weisj.jsvg.nodes.Anchor;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.ShapeNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.HasGeometryContext;
import com.github.weisj.jsvg.nodes.prototype.Transformable;
import com.github.weisj.jsvg.nodes.prototype.impl.HasGeometryContextImpl;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.NotImplemented;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.nodes.text.TextSegment.RenderableSegment.UseTextLengthForCalculation;
import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.AttributeNode.ElementRelation;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.ElementBounds;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.TextOutput;
import com.github.weisj.jsvg.util.AttributeUtil;
import com.github.weisj.jsvg.util.PathUtil;

@ElementCategories({Category.Graphic, Category.TextContent, Category.TextContentChild})
@PermittedContent(
    categories = {Category.Descriptive},
    anyOf = {Anchor.class, TextSpan.class, Animate.class, AnimateTransform.class, Set.class, /* <altGlyph>, <tref> */},
    charData = true
)
public final class TextPath extends TextContainer<TextSegment>
        implements HasGeometryContext.ByDelegate, TextLayoutGroup, CursorContext {
    public static final String TAG = "textpath";
    private static final boolean DEBUG = false;

    private final LayoutGroupSegment<TextSegment, TextPath> asSegment = new LayoutGroupSegment<>(this, this);

    private HasGeometryContext geometryContext;
    private SVGShape pathShape;
    private @Nullable Transformable pathShapeTransform;

    @SuppressWarnings("UnusedVariable")
    private @NotImplemented Spacing spacing;
    @SuppressWarnings("UnusedVariable")
    private @NotImplemented GlyphRenderMethod renderMethod;
    private Side side;

    private Length startOffset;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        geometryContext = HasGeometryContextImpl.parse(attributeNode);

        renderMethod = attributeNode.getEnum("method", GlyphRenderMethod.Align);
        side = attributeNode.getEnum("side", Side.Left);
        spacing = attributeNode.getEnum("spacing", Spacing.Auto);
        // Todo: Needs to be resolved w.r.t to the paths coordinate system
        startOffset = attributeNode.getLength("startOffset", PercentageDimension.CUSTOM, 0);

        String pathData = attributeNode.getValue("path");
        if (pathData != null) {
            // TODO: If this contains an error, we should use the href attribute instead.
            pathShape = PathUtil.parseFromPathData(pathData, FillRule.EvenOdd);
            pathShapeTransform = null;
        } else {
            String href = attributeNode.getHref();
            ShapeNode shape =
                    attributeNode.getElementByHref(ShapeNode.class, Category.Shape /* BasicShape or Path */, href,
                            ElementRelation.GEOMETRY_DATA);
            if (shape != null) {
                pathShape = shape.shape();
                pathShapeTransform = shape;
            }
        }
    }

    @Override
    protected void doAdd(@NotNull SVGNode node) {
        children.add((TextSegment) node);
    }

    @Override
    public void addContent(@NotNull TextContent.Segment content) {
        if (content.isConstant() && content.text().isEmpty()) return;
        if (children.isEmpty() && content.isConstant() && AttributeUtil.isBlank(content.text())) return;
        children.add(new StringTextSegment(this, this, children.size(), content));
    }

    @Override
    protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
        return node instanceof TextSegment;
    }

    @Override
    public @NotNull HasGeometryContext geometryContextDelegate() {
        return geometryContext;
    }

    @Override
    public boolean isVisible(@NotNull RenderContext context) {
        return pathShape != null && super.isVisible(context);
    }

    @Override
    public @NotNull List<? extends TextSegment> segments() {
        return children();
    }

    @Override
    public @NotNull Point2D renderText(@Nullable Point2D start, @NotNull RenderContext context,
            @NotNull Output output) {
        PathGlyphCursor cursor = createCursorWithAnchorAdjustment(context);
        TextOutput textOutput = output.textOutput();
        textOutput.beginText();
        asSegment.prepareSegmentForRendering(cursor, context, textOutput);
        asSegment.renderSegmentWithoutLayout(cursor, context, output);
        textOutput.endText();
        if (DEBUG) {
            output.debugPaint(g -> paintDebugPath(context, g));
        }
        return GeometryUtil.lastPointOnPath(cursor.pathIterator());
    }

    @Override
    public @NotNull Point2D appendGlyphShape(@Nullable Point2D start, @NotNull RenderContext context,
            @NotNull Path2D shape) {
        PathGlyphCursor cursor = createCursorWithAnchorAdjustment(context);
        shape.append(glyphShape(cursor, context), false);
        return GeometryUtil.lastPointOnPath(cursor.pathIterator());
    }

    @Override
    @NotNull
    Shape glyphShape(@NotNull RenderContext context) {
        return glyphShape(createCursorWithAnchorAdjustment(context), context);
    }

    private @NotNull Shape glyphShape(PathGlyphCursor cursor, @NotNull RenderContext context) {
        MutableGlyphRun glyphRun = new MutableGlyphRun();
        asSegment.appendTextShape(cursor, glyphRun, context);
        return glyphRun.shape();
    }

    @Override
    public @Nullable Length fixedLength() {
        if (textLength.isSpecified()) return textLength;
        return null;
    }

    private @NotNull PathGlyphCursor createCursorWithAnchorAdjustment(@NotNull RenderContext context) {
        return new PathGlyphCursor(
                createPathIterator(context),
                computeAnchorAdjustedStartOffset(context));
    }

    private float computeAnchorAdjustedStartOffset(@NotNull RenderContext context) {
        float offset = computeStartOffset(context);
        TextAnchor textAnchor = textAnchor(context);
        switch (textAnchor) {
            case Start:
                return offset;
            case Middle:
                return offset - computeTotalTextLength(context) / 2f;
            case End:
                return offset - computeTotalTextLength(context);
            default:
                throw new IllegalStateException("Unexpected value: " + textAnchor);
        }
    }

    private float computeTotalTextLength(@NotNull RenderContext context) {
        return (float) asSegment.computeTextMetrics(context, UseTextLengthForCalculation.YES).totalAdjustableLength();
    }

    private float computeStartOffset(@NotNull RenderContext context) {
        float offset = startOffset.resolve(context.measureContext());
        if (startOffset.unit().isPercentage()) {
            if (pathShape.isClosed(context)) {
                // Modulo 1 to obtain value inside [0, 1]
                offset = (offset % 1 + 1) % 1;
            }
            return (float) (offset * pathShape.pathLength(context));
        }
        return offset;
    }

    private void paintDebugPath(@NotNull RenderContext context, @NotNull Graphics2D g) {
        PathIterator pathIterator = createPathIterator(context);
        float startX = 0;
        float startY = 0;
        float curX = 0;
        float curY = 0;
        g.setStroke(new BasicStroke(0.5f));
        float[] cord = new float[2];
        while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(cord)) {
                case PathIterator.SEG_LINETO:
                    g.setColor(Color.MAGENTA);
                    g.draw(new Line2D.Float(curX, curY, cord[0], cord[1]));
                    g.setColor(Color.RED);
                    g.fillRect((int) curX - 2, (int) curY - 2, 4, 4);
                    g.fillRect((int) cord[0] - 2, (int) cord[1] - 2, 4, 4);
                    curX = cord[0];
                    curY = cord[1];
                    break;
                case PathIterator.SEG_MOVETO:
                    curX = cord[0];
                    curY = cord[1];
                    startX = curX;
                    startY = curY;
                    break;
                case PathIterator.SEG_CLOSE:
                    g.setColor(Color.MAGENTA);
                    g.draw(new Line2D.Float(curX, curY, startX, startY));
                    g.setColor(Color.RED);
                    g.fillRect((int) curX - 2, (int) curY - 2, 4, 4);
                    g.fillRect((int) startX - 2, (int) startY - 2, 4, 4);
                    curX = startX;
                    curY = startY;
                    break;
                default:
                    throw new IllegalStateException();
            }
            pathIterator.next();
        }
    }

    private @NotNull PathIterator createPathIterator(@NotNull RenderContext context) {
        MeasureContext measureContext = context.measureContext();
        Shape path = pathShape.shape(context);
        if (pathShapeTransform != null) {
            path = pathShapeTransform.transformShape(path, context,
                    ElementBounds.fromUntransformedBounds(this, context, path.getBounds2D(), Box.BoundingBox));
        }
        // For fonts this is a good enough approximation
        float flatness = 0.1f * measureContext.ex();
        switch (side) {
            case Left:
                return path.getPathIterator(null, flatness);
            case Right:
                return new ReversePathIterator(path.getPathIterator(null, flatness));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public @NotNull GlyphCursor createLocalCursor(boolean isInitial, @NotNull GlyphCursor current) {
        return current;
    }

    @Override
    public void cleanUpLocalCursor(@NotNull GlyphCursor current, @NotNull GlyphCursor local) {
        current.updateFrom(local);
    }
}
