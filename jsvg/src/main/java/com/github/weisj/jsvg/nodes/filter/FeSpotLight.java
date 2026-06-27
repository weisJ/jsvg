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
package com.github.weisj.jsvg.nodes.filter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;

@ElementCategories(Category.LightSource)
@PermittedContent
public final class FeSpotLight extends FePointLight {
    public static final String TAG = "fespotlight";

    private double directionX;
    private double directionY;
    private double directionZ;
    private double specularExponent;
    private @Nullable Double limitingConeCos;

    @Override
    public @NotNull String tagName() {
        return TAG;
    }

    @Override
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);

        double pointsAtX = attributeNode.getFloat("pointsAtX", 0);
        double pointsAtY = attributeNode.getFloat("pointsAtY", 0);
        double pointsAtZ = attributeNode.getFloat("pointsAtZ", 0);
        double dx = pointsAtX - lightX;
        double dy = pointsAtY - lightY;
        double dz = pointsAtZ - lightZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length != 0) {
            directionX = dx / length;
            directionY = dy / length;
            directionZ = dz / length;
        }

        specularExponent = attributeNode.getNonNegativeFloat("specularExponent", 1);
        String limitingConeAngle = attributeNode.getValue("limitingConeAngle");
        if (limitingConeAngle != null) {
            limitingConeCos = Math.cos(Math.toRadians(attributeNode.getFloat("limitingConeAngle", 0)));
        } else {
            limitingConeCos = null;
        }
    }

    @Override
    public @NotNull Light lightAt(double x, double y, double z) {
        Light light = computePointLight(x, y, z, 1);
        if (light.intensity == 0) return light;

        double dot = -(light.x * directionX + light.y * directionY + light.z * directionZ);
        if (dot <= 0) return new Light(light.x, light.y, light.z, 0);
        if (limitingConeCos != null && dot < limitingConeCos) return new Light(light.x, light.y, light.z, 0);

        return new Light(light.x, light.y, light.z, Math.pow(dot, specularExponent));
    }
}
