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
package com.github.weisj.jsvg.view.impl;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.ConstantLengthTransform;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.parser.impl.AttributeParser;
import com.github.weisj.jsvg.util.UriUtil;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * Parses SVG fragment identifiers according to
 * <a href="https://www.w3.org/TR/SVG2/linking.html#SVGFragmentIdentifiersDefinitions">SVG 2, SVG fragment
 * identifier definitions</a> and the
 * <a href="https://www.w3.org/TR/media-frags/#naming-space">Media Fragments URI syntax</a>.
 */
public final class FragmentView {
    private static final Pattern SVG_VIEW = Pattern.compile(
            "^svgView\\s*\\((.*)\\)\\s*$", Pattern.DOTALL);
    private static final Pattern VIEW_ATTRIBUTE = Pattern.compile(
            "^([A-Za-z]+)\\s*\\((.*)\\)\\s*$", Pattern.DOTALL);
    private static final Pattern SPATIAL_FRAGMENT = Pattern.compile(
            "^(?:(pixel|percent):)?(\\d+),(\\d+),(\\d+),(\\d+)$");
    private static final ViewImpl DEFAULT = new FixedView(ResolvedView.DEFAULT);

    private FragmentView() {}

    /** Parses an encoded URI fragment, without its leading {@code #}. */
    public static @NotNull ViewImpl parse(@NotNull String rawFragment) {
        Objects.requireNonNull(rawFragment);

        ParsedFragment fragment = new ParsedFragment();
        // URI components must be separated before percent-encoded octets are decoded:
        // https://www.w3.org/TR/media-frags/#processing-name-value-components
        for (String rawComponent : rawFragment.split("&", -1)) {
            if (!parseComponent(rawComponent, fragment)) return DEFAULT;
        }
        return fragment.toView();
    }

    private static boolean parseComponent(@NotNull String rawComponent, @NotNull ParsedFragment fragment) {
        int equals = rawComponent.indexOf('=');
        if (equals < 0) {
            String primary = decodeComponent(rawComponent);
            return primary == null || primary.isEmpty() || fragment.setPrimary(primary);
        }

        String name = decodeComponent(rawComponent.substring(0, equals));
        String value = decodeComponent(rawComponent.substring(equals + 1));
        if (name == null || value == null) return true;
        if ("t".equals(name)) {
            // TODO: Apply the media time segment to the document timeline as specified by
            // https://www.w3.org/TR/SVG2/linking.html#SVGFragmentIdentifiersDefinitions
        } else if ("xywh".equals(name)) {
            fragment.setSpatial(SpatialView.parse(value));
        }
        // Unknown media-fragment dimensions are ignored.
        return true;
    }

    private static @Nullable String decodeComponent(@NotNull String value) {
        try {
            return UriUtil.percentDecode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static final class ParsedFragment {
        private @Nullable String primary;
        private @Nullable SpatialView spatial;

        private boolean setPrimary(@NotNull String primary) {
            if (this.primary != null) return false;
            this.primary = primary;
            return true;
        }

        private void setSpatial(@Nullable SpatialView spatial) {
            // Media Fragments uses the last valid occurrence of a dimension.
            if (spatial != null) this.spatial = spatial;
        }

        private @NotNull ViewImpl toView() {
            // The SVG grammar only combines a time segment with one non-time form.
            if (primary != null && spatial != null) return DEFAULT;
            if (spatial != null) return spatial;
            if (primary == null) return DEFAULT;

            String value = primary.trim();
            if (!value.startsWith("svgView")) return new NamedView(value);
            ResolvedView view = parseSVGView(value);
            return view != null ? new FixedView(view) : DEFAULT;
        }

        private static @Nullable ResolvedView parseSVGView(@NotNull String value) {
            Matcher viewMatcher = SVG_VIEW.matcher(value);
            if (!viewMatcher.matches()) return null;

            ParsedView view = new ParsedView();
            Set<String> seenAttributes = new HashSet<>();
            for (String attribute : viewMatcher.group(1).split(";", -1)) {
                Matcher attributeMatcher = VIEW_ATTRIBUTE.matcher(attribute.trim());
                if (!attributeMatcher.matches()) return null;
                String name = attributeMatcher.group(1);
                if (!seenAttributes.add(name) || !view.parse(name, attributeMatcher.group(2))) return null;
            }
            return view.toResolvedView();
        }
    }

    private static final class ParsedView {
        private @Nullable ViewBox viewBox;
        private @Nullable PreserveAspectRatio preserveAspectRatio;
        private @Nullable TransformValue transformOverride;

        private boolean parse(@NotNull String name, @NotNull String value) {
            switch (name) {
                case "viewBox":
                    float[] coordinates = AttributeParser.INSTANCE.parseFloatList(value);
                    if (coordinates.length != 4 || coordinates[2] < 0 || coordinates[3] < 0) return false;
                    viewBox = new ViewBox(coordinates);
                    return true;
                case "preserveAspectRatio":
                    try {
                        preserveAspectRatio = PreserveAspectRatio.parse(value, AttributeParser.INSTANCE);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                case "transform":
                    return parseTransform(value);
                case "zoomAndPan":
                    String zoomAndPan = value.trim();
                    // zoomAndPan governs interactive zooming and has no effect on a static render.
                    return "magnify".equals(zoomAndPan) || "disable".equals(zoomAndPan);
                default:
                    return false;
            }
        }

        private boolean parseTransform(@NotNull String value) {
            String transform = value.trim();
            if ("none".equals(transform)) {
                transformOverride = ConstantLengthTransform.IDENTITY;
                return true;
            }
            if (transform.isEmpty()) return false;
            try {
                List<TransformPart> parts = AttributeParser.INSTANCE.parseTransform(transform);
                if (parts == null) return false;
                transformOverride = new ConstantLengthTransform(parts);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

        private @NotNull ResolvedView toResolvedView() {
            return new ResolvedView(viewBox, preserveAspectRatio, transformOverride);
        }
    }

    private static final class FixedView implements ViewImpl {
        private final @NotNull ResolvedView view;

        private FixedView(@NotNull ResolvedView view) {
            this.view = view;
        }

        @Override
        public @NotNull ResolvedView resolve(
                @NotNull Map<String, com.github.weisj.jsvg.nodes.View> views,
                @NotNull FloatSize documentSize) {
            return view;
        }
    }

    private static final class SpatialView implements ViewImpl {
        private final boolean percent;
        private final long x;
        private final long y;
        private final long width;
        private final long height;

        private SpatialView(boolean percent, long x, long y, long width, long height) {
            this.percent = percent;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private static @Nullable SpatialView parse(@NotNull String value) {
            Matcher matcher = SPATIAL_FRAGMENT.matcher(value);
            if (!matcher.matches()) return null;
            try {
                boolean percent = "percent".equals(matcher.group(1));
                long x = Long.parseLong(matcher.group(2));
                long y = Long.parseLong(matcher.group(3));
                long width = Long.parseLong(matcher.group(4));
                long height = Long.parseLong(matcher.group(5));
                if (width == 0 || height == 0) return null;
                if (percent && (x > 100 || y > 100 || width > 100 - x || height > 100 - y)) return null;
                return new SpatialView(percent, x, y, width, height);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public @NotNull ResolvedView resolve(
                @NotNull Map<String, com.github.weisj.jsvg.nodes.View> views,
                @NotNull FloatSize documentSize) {
            if (documentSize.width <= 0 || documentSize.height <= 0) return ResolvedView.DEFAULT;

            double resolvedX = x;
            double resolvedY = y;
            double resolvedWidth = width;
            double resolvedHeight = height;
            if (percent) {
                resolvedX = Math.floor(documentSize.width * x / 100d);
                resolvedY = Math.floor(documentSize.height * y / 100d);
                resolvedWidth = Math.ceil(documentSize.width * width / 100d);
                resolvedHeight = Math.ceil(documentSize.height * height / 100d);
            }
            if (resolvedX >= documentSize.width || resolvedY >= documentSize.height) return ResolvedView.DEFAULT;

            resolvedWidth = Math.min(resolvedWidth, documentSize.width - resolvedX);
            resolvedHeight = Math.min(resolvedHeight, documentSize.height - resolvedY);
            if (resolvedWidth <= 0 || resolvedHeight <= 0) return ResolvedView.DEFAULT;
            return new ResolvedView(new ViewBox(
                    (float) resolvedX, (float) resolvedY,
                    (float) resolvedWidth, (float) resolvedHeight),
                    null);
        }
    }
}
