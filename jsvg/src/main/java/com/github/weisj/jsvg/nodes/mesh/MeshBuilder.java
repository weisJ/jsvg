/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
package com.github.weisj.jsvg.nodes.mesh;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.geometry.mesh.Bezier;
import com.github.weisj.jsvg.geometry.mesh.CoonPatch;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.Stop;

final class MeshBuilder {
    private MeshBuilder() {}

    static void buildMesh(@NotNull MeshGradient meshGradient, @NotNull Point2D.Float origin) {
        Point2D.Float start = origin;

        int patchCount = -1;
        List<? extends @NotNull SVGNode> rows = meshGradient.children();

        for (int rowIndex = 0, rowCount = rows.size(); rowIndex < rowCount; rowIndex++) {
            SVGNode child = rows.get(rowIndex);
            MeshRow row = (MeshRow) child;
            int rowPatchCount = row.children().size();
            if (patchCount == -1) patchCount = rowPatchCount;
            if (rowPatchCount != patchCount) {
                throw new IllegalStateException("Every mesh row needs to specify the same amount of patched");
            }
            List<? extends @NotNull SVGNode> patchesInRow = row.children();
            for (int patchIndex = 0; patchIndex < rowPatchCount; patchIndex++) {
                SVGNode node = patchesInRow.get(patchIndex);
                MeshPatch patch = (MeshPatch) node;

                List<? extends @NotNull SVGNode> stops = patch.children();
                int stopCount = stops.size();
                int requiredStops = stopsForPatchPosition(rowIndex, patchIndex);
                if (stopCount < requiredStops) {
                    throw new IllegalStateException("Not enough stops specified");
                }
                int offset = offsetForPatchPosition(rowIndex);
                stopCount = requiredStops;

                MeshPatch patchAbove = null;
                if (offset == 1) {
                    patchAbove = (MeshPatch) ((MeshRow) rows.get(rowIndex - 1)).children().get(patchIndex);
                    patch.coonPatch.north = patchAbove.coonPatch.south.inverse();
                    patch.north = patchAbove.west;
                }

                MeshPatch patchLeft = null;
                if (offset + stopCount < 4) {
                    patchLeft = (MeshPatch) patchesInRow.get(patchIndex - 1);
                    patch.coonPatch.west = patchLeft.coonPatch.east.inverse();
                    patch.west = patchLeft.south;
                } else if (patchIndex > 0) {
                    patchLeft = (MeshPatch) patchesInRow.get(patchIndex - 1);
                }

                for (int stopIndex = 0; stopIndex < stopCount; stopIndex++) {
                    Stop stop = (Stop) stops.get(stopIndex);
                    switch (stopIndex + offset) {
                        case 0:
                            patch.coonPatch.north = Objects.requireNonNull(stop.bezierCommand())
                                    .createBezier(start);
                            start = patch.coonPatch.north.d;
                            patch.north = patchIndex > 0 ? Objects.requireNonNull(patchLeft).east : stop.color();
                            break;
                        case 1:
                            patch.coonPatch.east = Objects.requireNonNull(stop.bezierCommand())
                                    .createBezier(patch.coonPatch.north.d);
                            patch.east = offset == 1 ? Objects.requireNonNull(patchAbove).south : stop.color();
                            break;
                        case 2:
                            patch.coonPatch.south = Objects.requireNonNull(stop.bezierCommand())
                                    .createBezier(patch.coonPatch.east.d);
                            patch.south = stop.color();
                            break;
                        case 3:
                            patch.coonPatch.west = Objects.requireNonNull(stop.bezierCommand())
                                    .createBezier(patch.coonPatch.south.d);
                            patch.coonPatch.west.d = patch.coonPatch.north.a;
                            patch.west = stop.color();
                            break;
                        default:
                            assert false;
                            break;
                    }
                }
                if (offset + stopCount < 4) {
                    assert patchLeft != null;
                    patch.coonPatch.south.d = patchLeft.coonPatch.east.d;
                }

                try {
                    assertPatchDefined(patch.coonPatch);
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(rowIndex + "," + patchIndex + " " + patch.coonPatch, e);
                }
            }
        }
    }

    private static int stopsForPatchPosition(int rowIndex, int patchIndex) {
        if (rowIndex > 0) {
            if (patchIndex > 0) {
                return 2;
            } else {
                return 3;
            }
        } else {
            if (patchIndex > 0) {
                return 3;
            } else {
                return 4;
            }
        }
    }

    private static int offsetForPatchPosition(int rowIndex) {
        if (rowIndex > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    private static void assertPatchDefined(@NotNull CoonPatch coonPatch) {
        if (coonPatch.north == null) throw new IllegalStateException("North path is null");
        if (coonPatch.east == null) throw new IllegalStateException("East path is null");
        if (coonPatch.south == null) throw new IllegalStateException("South path is null");
        if (coonPatch.west == null) throw new IllegalStateException("West path is null");
        if (hasUnspecifiedPoint(coonPatch.north))
            throw new IllegalStateException("North path has unspecified point");
        if (hasUnspecifiedPoint(coonPatch.east))
            throw new IllegalStateException("East path has unspecified point");
        if (hasUnspecifiedPoint(coonPatch.south))
            throw new IllegalStateException("South path has unspecified point");
        if (hasUnspecifiedPoint(coonPatch.west))
            throw new IllegalStateException("West path has unspecified point");
    }

    private static boolean hasUnspecifiedPoint(Bezier bezier) {
        if (isUnspecified(bezier.a)) return true;
        if (isUnspecified(bezier.b)) return true;
        if (isUnspecified(bezier.c)) return true;
        return isUnspecified(bezier.d);
    }

    private static boolean isUnspecified(@NotNull Point2D.Float p) {
        return !Length.isSpecified(p.x) || !Length.isSpecified(p.y);
    }
}
