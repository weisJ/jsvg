package com.github.weisj.jsvg.animation.interpolation;

import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.MeasureContext;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.AffineTransform;

public interface TransformInterpolator {

    @NotNull
    AffineTransform interpolate(@NotNull MeasureContext context,
            @NotNull TransformValue initial, @NotNull TransformPart a, @NotNull TransformPart b, float progress);
}
