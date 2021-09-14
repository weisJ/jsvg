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
package com.github.weisj.jsvg.attributes.paint;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.AttributeParser;

public final class PaintParser {
    public static final Color DEFAULT_COLOR = Color.BLACK;

    private PaintParser() {}

    // Todo: Handle hsl(), hsla() per the SVG 2.0 spec requirement
    public static @Nullable Color parseColor(@Nullable String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            if (value.charAt(0) == '#') {
                int rgba = 0xff000000;
                switch (value.length()) {
                    case 4:
                        // Short rgb
                        rgba = parseHex(new char[] {
                                value.charAt(1), value.charAt(1),
                                value.charAt(2), value.charAt(2),
                                value.charAt(3), value.charAt(3),
                                'F', 'F'});
                        break;
                    case 5:
                        // Short rgba
                        rgba = parseHex(new char[] {
                                value.charAt(1), value.charAt(1),
                                value.charAt(2), value.charAt(2),
                                value.charAt(3), value.charAt(3),
                                value.charAt(4), value.charAt(4)});
                        break;
                    case 7:
                        // Long rgb
                        rgba = parseHex(new char[] {
                                value.charAt(1), value.charAt(2),
                                value.charAt(3), value.charAt(4),
                                value.charAt(5), value.charAt(6),
                                'F', 'F'});
                        break;
                    case 9:
                        // Long rgba
                        rgba = parseHex(value.substring(1).toCharArray());
                        break;
                }
                return new Color(rgba, true);
            } else if (value.length() > 3 && value.substring(0, 3).equalsIgnoreCase("rgb")) {
                boolean isRgba = value.length() > 4 && (value.charAt(3) == 'a' || value.charAt(3) == 'A');
                int startIndex = isRgba ? 5 : 4;
                String[] values = AttributeParser.parseStringList(
                        value.substring(startIndex, value.length() - 1), false);
                isRgba = isRgba && values.length >= 4;
                return new Color(
                        parseColorComponent(values[0], false),
                        parseColorComponent(values[1], false),
                        parseColorComponent(values[2], false),
                        isRgba ? parseColorComponent(values[3], true) : 255);
            }
            return ColorLookup.colorMap().get(value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static @Nullable SVGPaint parsePaint(String value) {
        if ("none".equals(value) || "transparent".equals(value)) return SVGPaint.NONE;
        if ("currentcolor".equals(value)) return SVGPaint.CURRENT_COLOR;
        if ("context-fill".equals(value)) return SVGPaint.CONTEXT_FILL;
        if ("context-stroke".equals(value)) return SVGPaint.CONTEXT_STROKE;
        Color color = parseColor(value);
        if (color == null) return null;
        return new AwtSVGPaint(color);
    }

    private static int parseColorComponent(String value, boolean percentage) {
        float parsed;
        if (value.endsWith("%")) {
            parsed = AttributeParser.parseFloat(value.substring(0, value.length() - 1), 0);
            parsed /= 100;
            parsed *= 255;
        } else {
            parsed = AttributeParser.parseFloat(value, 0);
            if (percentage) parsed *= 255;
        }
        return Math.min(255, Math.max(0, (int) parsed));
    }

    private static int parseHex(char[] chars) {
        int r = charToColorInt(chars[0]) << 4 | charToColorInt(chars[1]);
        int g = charToColorInt(chars[2]) << 4 | charToColorInt(chars[3]);
        int b = charToColorInt(chars[4]) << 4 | charToColorInt(chars[5]);
        int a = charToColorInt(chars[6]) << 4 | charToColorInt(chars[7]);
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF);
    }

    private static int charToColorInt(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'z') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        } else {
            return 0;
        }
    }

    private static class ColorLookup {
        private static Map<String, Color> colorMap;

        private static Map<String, Color> colorMap() {
            if (colorMap != null) return colorMap;
            colorMap = new HashMap<>(143);

            colorMap.put("aliceblue", new Color(0xf0f8ff));
            colorMap.put("antiquewhite", new Color(0xfaebd7));
            colorMap.put("aqua", new Color(0x00ffff));
            colorMap.put("aquamarine", new Color(0x7fffd4));
            colorMap.put("azure", new Color(0xf0ffff));
            colorMap.put("beige", new Color(0xf5f5dc));
            colorMap.put("bisque", new Color(0xffe4c4));
            colorMap.put("black", new Color(0x000000));
            colorMap.put("blanchedalmond", new Color(0xffebcd));
            colorMap.put("blue", new Color(0x0000ff));
            colorMap.put("blueviolet", new Color(0x8a2be2));
            colorMap.put("brown", new Color(0xa52a2a));
            colorMap.put("burlywood", new Color(0xdeb887));
            colorMap.put("cadetblue", new Color(0x5f9ea0));
            colorMap.put("chartreuse", new Color(0x7fff00));
            colorMap.put("chocolate", new Color(0xd2691e));
            colorMap.put("coral", new Color(0xff7f50));
            colorMap.put("cornflowerblue", new Color(0x6495ed));
            colorMap.put("cornsilk", new Color(0xfff8dc));
            colorMap.put("crimson", new Color(0xdc143c));
            colorMap.put("cyan", new Color(0x00ffff));
            colorMap.put("darkblue", new Color(0x00008b));
            colorMap.put("darkcyan", new Color(0x008b8b));
            colorMap.put("darkgoldenrod", new Color(0xb8860b));
            colorMap.put("darkgray", new Color(0xa9a9a9));
            colorMap.put("darkgrey", new Color(0xa9a9a9));
            colorMap.put("darkgreen", new Color(0x006400));
            colorMap.put("darkkhaki", new Color(0xbdb76b));
            colorMap.put("darkmagenta", new Color(0x8b008b));
            colorMap.put("darkolivegreen", new Color(0x556b2f));
            colorMap.put("darkorange", new Color(0xff8c00));
            colorMap.put("darkorchid", new Color(0x9932cc));
            colorMap.put("darkred", new Color(0x8b0000));
            colorMap.put("darksalmon", new Color(0xe9967a));
            colorMap.put("darkseagreen", new Color(0x8fbc8f));
            colorMap.put("darkslateblue", new Color(0x483d8b));
            colorMap.put("darkslategray", new Color(0x2f4f4f));
            colorMap.put("darkslategrey", new Color(0x2f4f4f));
            colorMap.put("darkturquoise", new Color(0x00ced1));
            colorMap.put("darkviolet", new Color(0x9400d3));
            colorMap.put("deeppink", new Color(0xff1493));
            colorMap.put("deepskyblue", new Color(0x00bfff));
            colorMap.put("dimgray", new Color(0x696969));
            colorMap.put("dimgrey", new Color(0x696969));
            colorMap.put("dodgerblue", new Color(0x1e90ff));
            colorMap.put("feldspar", new Color(0xd19275));
            colorMap.put("firebrick", new Color(0xb22222));
            colorMap.put("floralwhite", new Color(0xfffaf0));
            colorMap.put("forestgreen", new Color(0x228b22));
            colorMap.put("fuchsia", new Color(0xff00ff));
            colorMap.put("gainsboro", new Color(0xdcdcdc));
            colorMap.put("ghostwhite", new Color(0xf8f8ff));
            colorMap.put("gold", new Color(0xffd700));
            colorMap.put("goldenrod", new Color(0xdaa520));
            colorMap.put("gray", new Color(0x808080));
            colorMap.put("grey", new Color(0x808080));
            colorMap.put("green", new Color(0x008000));
            colorMap.put("greenyellow", new Color(0xadff2f));
            colorMap.put("honeydew", new Color(0xf0fff0));
            colorMap.put("hotpink", new Color(0xff69b4));
            colorMap.put("indianred", new Color(0xcd5c5c));
            colorMap.put("indigo", new Color(0x4b0082));
            colorMap.put("ivory", new Color(0xfffff0));
            colorMap.put("khaki", new Color(0xf0e68c));
            colorMap.put("lavender", new Color(0xe6e6fa));
            colorMap.put("lavenderblush", new Color(0xfff0f5));
            colorMap.put("lawngreen", new Color(0x7cfc00));
            colorMap.put("lemonchiffon", new Color(0xfffacd));
            colorMap.put("lightblue", new Color(0xadd8e6));
            colorMap.put("lightcoral", new Color(0xf08080));
            colorMap.put("lightcyan", new Color(0xe0ffff));
            colorMap.put("lightgoldenrodyellow", new Color(0xfafad2));
            colorMap.put("lightgrey", new Color(0xd3d3d3));
            colorMap.put("lightgreen", new Color(0x90ee90));
            colorMap.put("lightpink", new Color(0xffb6c1));
            colorMap.put("lightsalmon", new Color(0xffa07a));
            colorMap.put("lightseagreen", new Color(0x20b2aa));
            colorMap.put("lightskyblue", new Color(0x87cefa));
            colorMap.put("lightslateblue", new Color(0x8470ff));
            colorMap.put("lightslategray", new Color(0x778899));
            colorMap.put("lightslategrey", new Color(0x778899));
            colorMap.put("lightsteelblue", new Color(0xb0c4de));
            colorMap.put("lightyellow", new Color(0xffffe0));
            colorMap.put("lime", new Color(0x00ff00));
            colorMap.put("limegreen", new Color(0x32cd32));
            colorMap.put("linen", new Color(0xfaf0e6));
            colorMap.put("magenta", new Color(0xff00ff));
            colorMap.put("maroon", new Color(0x800000));
            colorMap.put("mediumaquamarine", new Color(0x66cdaa));
            colorMap.put("mediumblue", new Color(0x0000cd));
            colorMap.put("mediumorchid", new Color(0xba55d3));
            colorMap.put("mediumpurple", new Color(0x9370d8));
            colorMap.put("mediumseagreen", new Color(0x3cb371));
            colorMap.put("mediumslateblue", new Color(0x7b68ee));
            colorMap.put("mediumspringgreen", new Color(0x00fa9a));
            colorMap.put("mediumturquoise", new Color(0x48d1cc));
            colorMap.put("mediumvioletred", new Color(0xc71585));
            colorMap.put("midnightblue", new Color(0x191970));
            colorMap.put("mintcream", new Color(0xf5fffa));
            colorMap.put("mistyrose", new Color(0xffe4e1));
            colorMap.put("moccasin", new Color(0xffe4b5));
            colorMap.put("navajowhite", new Color(0xffdead));
            colorMap.put("navy", new Color(0x000080));
            colorMap.put("oldlace", new Color(0xfdf5e6));
            colorMap.put("olive", new Color(0x808000));
            colorMap.put("olivedrab", new Color(0x6b8e23));
            colorMap.put("orange", new Color(0xffa500));
            colorMap.put("orangered", new Color(0xff4500));
            colorMap.put("orchid", new Color(0xda70d6));
            colorMap.put("palegoldenrod", new Color(0xeee8aa));
            colorMap.put("palegreen", new Color(0x98fb98));
            colorMap.put("paleturquoise", new Color(0xafeeee));
            colorMap.put("palevioletred", new Color(0xd87093));
            colorMap.put("papayawhip", new Color(0xffefd5));
            colorMap.put("peachpuff", new Color(0xffdab9));
            colorMap.put("peru", new Color(0xcd853f));
            colorMap.put("pink", new Color(0xffc0cb));
            colorMap.put("plum", new Color(0xdda0dd));
            colorMap.put("powderblue", new Color(0xb0e0e6));
            colorMap.put("purple", new Color(0x800080));
            colorMap.put("red", new Color(0xff0000));
            colorMap.put("rosybrown", new Color(0xbc8f8f));
            colorMap.put("royalblue", new Color(0x4169e1));
            colorMap.put("saddlebrown", new Color(0x8b4513));
            colorMap.put("salmon", new Color(0xfa8072));
            colorMap.put("sandybrown", new Color(0xf4a460));
            colorMap.put("seagreen", new Color(0x2e8b57));
            colorMap.put("seashell", new Color(0xfff5ee));
            colorMap.put("sienna", new Color(0xa0522d));
            colorMap.put("silver", new Color(0xc0c0c0));
            colorMap.put("skyblue", new Color(0x87ceeb));
            colorMap.put("slateblue", new Color(0x6a5acd));
            colorMap.put("slategray", new Color(0x708090));
            colorMap.put("slategrey", new Color(0x708090));
            colorMap.put("snow", new Color(0xfffafa));
            colorMap.put("springgreen", new Color(0x00ff7f));
            colorMap.put("steelblue", new Color(0x4682b4));
            colorMap.put("tan", new Color(0xd2b48c));
            colorMap.put("teal", new Color(0x008080));
            colorMap.put("thistle", new Color(0xd8bfd8));
            colorMap.put("tomato", new Color(0xff6347));
            colorMap.put("turquoise", new Color(0x40e0d0));
            colorMap.put("violet", new Color(0xee82ee));
            colorMap.put("violetred", new Color(0xd02090));
            colorMap.put("wheat", new Color(0xf5deb3));
            colorMap.put("white", new Color(0xffffff));
            colorMap.put("whitesmoke", new Color(0xf5f5f5));
            colorMap.put("yellow", new Color(0xffff00));
            colorMap.put("yellowgreen", new Color(0x9acd32));
            colorMap = Collections.unmodifiableMap(colorMap);
            return colorMap;
        }
    }
}
