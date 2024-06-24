/*
 * MIT License
 *
 * Copyright (c) 2023-2024 Jannis Weis
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
package com.github.weisj.jsvg.parser;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.*;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.AnimateTransform;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.filter.*;
import com.github.weisj.jsvg.nodes.mesh.MeshGradient;
import com.github.weisj.jsvg.nodes.mesh.MeshPatch;
import com.github.weisj.jsvg.nodes.mesh.MeshRow;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.nodes.text.TextPath;
import com.github.weisj.jsvg.nodes.text.TextSpan;

@SuppressWarnings("Convert2MethodRef")
public final class NodeSupplier {

    private final Map<String, Supplier<SVGNode>> constructorMap;

    public NodeSupplier() {
        this(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }


    public NodeSupplier(final @NotNull Map<@NotNull String, @NotNull Supplier<@NotNull SVGNode>> mapImpl) {
        mapImpl.clear();
        constructorMap = mapImpl;

        constructorMap.put(Anchor.TAG, () -> new Anchor());
        constructorMap.put(ClipPath.TAG, () -> new ClipPath());
        constructorMap.put(Defs.TAG, () -> new Defs());
        constructorMap.put(Group.TAG, () -> new Group());
        constructorMap.put(Image.TAG, () -> new Image());
        constructorMap.put(Marker.TAG, () -> new Marker());
        constructorMap.put(Mask.TAG, () -> new Mask());
        constructorMap.put(SVG.TAG, () -> new SVG());
        constructorMap.put(Style.TAG, () -> new Style());
        constructorMap.put(Symbol.TAG, () -> new Symbol());
        constructorMap.put(Use.TAG, () -> new Use());
        constructorMap.put(View.TAG, () -> new View());

        populateShapeNodeConstructors();
        populatePaintNodeConstructors();
        populateTextNodeConstructors();
        populateFilterNodeConstructors();
        populateAnimationNodeConstructors();
        populateMetaNodeConstructors();
        populateDummyNodeConstructors();
    }

    public @Nullable SVGNode create(@NotNull String tagName) {
        @Nullable Supplier<SVGNode> supplier = constructorMap.get(tagName);
        if (supplier == null) return null;
        return supplier.get();
    }

    private void populateShapeNodeConstructors() {
        constructorMap.put(Circle.TAG, () -> new Circle());
        constructorMap.put(Ellipse.TAG, () -> new Ellipse());
        constructorMap.put(Line.TAG, () -> new Line());
        constructorMap.put(Path.TAG, () -> new Path());
        constructorMap.put(Polygon.TAG, () -> new Polygon());
        constructorMap.put(Polyline.TAG, () -> new Polyline());
        constructorMap.put(Rect.TAG, () -> new Rect());
    }

    private void populatePaintNodeConstructors() {
        constructorMap.put(LinearGradient.TAG, () -> new LinearGradient());
        constructorMap.put(MeshGradient.TAG, () -> new MeshGradient());
        constructorMap.put(MeshPatch.TAG, () -> new MeshPatch());
        constructorMap.put(MeshRow.TAG, () -> new MeshRow());
        constructorMap.put(Pattern.TAG, () -> new Pattern());
        constructorMap.put(RadialGradient.TAG, () -> new RadialGradient());
        constructorMap.put(SolidColor.TAG, () -> new SolidColor());
        constructorMap.put(Stop.TAG, () -> new Stop());
    }

    private void populateTextNodeConstructors() {
        constructorMap.put(Text.TAG, () -> new Text());
        constructorMap.put(TextPath.TAG, () -> new TextPath());
        constructorMap.put(TextSpan.TAG, () -> new TextSpan());
    }

    private void populateFilterNodeConstructors() {
        constructorMap.put(Filter.TAG, () -> new Filter());
        constructorMap.put(FeBlend.TAG, () -> new FeBlend());
        constructorMap.put(FeColorMatrix.TAG, () -> new FeColorMatrix());
        constructorMap.put(FeComposite.TAG, () -> new FeComposite());
        constructorMap.put(FeDisplacementMap.TAG, () -> new FeDisplacementMap());
        constructorMap.put(FeDropShadow.TAG, () -> new FeDropShadow());
        constructorMap.put(FeFlood.TAG, () -> new FeFlood());
        constructorMap.put(FeGaussianBlur.TAG, () -> new FeGaussianBlur());
        constructorMap.put(FeMerge.TAG, () -> new FeMerge());
        constructorMap.put(FeMergeNode.TAG, () -> new FeMergeNode());
        constructorMap.put(FeTurbulence.TAG, () -> new FeTurbulence());
        constructorMap.put(FeOffset.TAG, () -> new FeOffset());
        constructorMap.put(FeComponentTransfer.TAG, () -> new FeComponentTransfer());
        constructorMap.put(TransferFunctionElement.FeFuncB.TAG, () -> new TransferFunctionElement.FeFuncB());
        constructorMap.put(TransferFunctionElement.FeFuncG.TAG, () -> new TransferFunctionElement.FeFuncG());
        constructorMap.put(TransferFunctionElement.FeFuncR.TAG, () -> new TransferFunctionElement.FeFuncR());
        constructorMap.put(TransferFunctionElement.FeFuncA.TAG, () -> new TransferFunctionElement.FeFuncA());
    }

    private void populateAnimationNodeConstructors() {
        constructorMap.put(Animate.TAG, () -> new Animate());
        constructorMap.put(AnimateTransform.TAG, () -> new AnimateTransform());
        constructorMap.put(Set.TAG, () -> new Set());
    }

    private void populateMetaNodeConstructors() {
        constructorMap.put(Desc.TAG, () -> new Desc());
        constructorMap.put(Metadata.TAG, () -> new Metadata());
        constructorMap.put(Title.TAG, () -> new Title());
    }

    private void populateDummyNodeConstructors() {
        constructorMap.put("feConvolveMatrix", () -> new DummyFilterPrimitive("feConvolveMatrix"));
        constructorMap.put("feDiffuseLightning", () -> new DummyFilterPrimitive("feDiffuseLightning"));
        constructorMap.put("feImage", () -> new DummyFilterPrimitive("feImage"));
        constructorMap.put("feMorphology", () -> new DummyFilterPrimitive("feMorphology"));
        constructorMap.put("feSpecularLighting", () -> new DummyFilterPrimitive("feSpecularLighting"));
        constructorMap.put("feTile", () -> new DummyFilterPrimitive("feTile"));
    }

}
