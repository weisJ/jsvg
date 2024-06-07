/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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


import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.attributes.filter.TransferFunctionType;
import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.AttributeNode;

public abstract class TransferFunctionElement extends AbstractSVGNode {

    static final byte[] IDENTITY_LOOKUP_TABLE = new byte[256];

    static {
        for (int i = 0; i < 256; i++) {
            IDENTITY_LOOKUP_TABLE[i] = (byte) i;
        }
    }

    public enum Channel {
        Red,
        Green,
        Blue,
        Alpha
    }

    private final Channel channel;

    private TransferFunctionType type;
    private byte[] lookupTable;

    private TransferFunctionElement(Channel channel) {

        this.channel = channel;
    }

    public Channel channel() {
        return channel;
    }

    public TransferFunctionType type() {
        return type;
    }

    public byte @NotNull [] lookupTable() {
        return lookupTable;
    }

    @Override
    @MustBeInvokedByOverriders
    public void build(@NotNull AttributeNode attributeNode) {
        super.build(attributeNode);
        type = attributeNode.getEnum("type", TransferFunctionType.Identity);
        byte[] table = createLookupTable(type, attributeNode);

        if (table == null) {
            type = TransferFunctionType.Identity;
            lookupTable = IDENTITY_LOOKUP_TABLE;
        } else {
            lookupTable = table;
        }
    }

    private static byte @Nullable [] createLookupTable(TransferFunctionType type,
            @NotNull AttributeNode attributeNode) {
        switch (type) {
            case Table:
            case Discrete:
                float[] table = attributeNode.getFloatList("tableValues");
                if (table.length == 0) return null;
                int[] intTable = new int[table.length];
                for (int i = 0; i < table.length; i++) {
                    intTable[i] = (int) (255f * table[i]);
                }
                return createTableBasedLookupTable(type, intTable);
            case Linear:
                float slope = attributeNode.getFloat("slope", 1);
                float intercept = attributeNode.getFloat("intercept", 0);
                if (slope == 1 && intercept == 0) return null;
                return createLinearLookupTable(intercept, slope);
            case Gamma:
                float amplitude = attributeNode.getFloat("amplitude", 1);
                float exponent = attributeNode.getFloat("exponent", 1);
                float offset = attributeNode.getFloat("offset", 0);
                if (amplitude == 1 && exponent == 1 && offset == 0) return null;
                return createGammaLookupTable(amplitude, exponent, offset);
            case Identity:
                return IDENTITY_LOOKUP_TABLE;
        }
        return null;
    }

    private static byte @Nullable [] createTableBasedLookupTable(TransferFunctionType type, int[] intTable) {
        int n = intTable.length;
        byte[] lookupTable = new byte[256];
        switch (type) {
            case Table:
                for (int j = 0; j <= 255; j++) {
                    float fi = j * (n - 1) / 255f;
                    int k = (int) Math.floor(fi);
                    int kNext = Math.min(k + 1, n - 1);
                    float r = fi - k;
                    int value = (int) (intTable[k] + r * (intTable[kNext] - intTable[k])) & 0xff;
                    lookupTable[j] = (byte) value;
                }
                break;
            case Discrete:
                for (int j = 0; j <= 255; j++) {
                    int i = (int) Math.floor(j * n / 255f);
                    if (i == n) {
                        i = n - 1;
                    }
                    lookupTable[j] = (byte) (intTable[i] & 0xff);
                }
                break;
            default:
                return null;
        }
        return lookupTable;
    }

    private static byte @Nullable [] createLinearLookupTable(float intercept, float slope) {
        byte[] table = new byte[256];
        float intIntercept = (intercept * 255f) + 0.5f;
        for (int j = 0; j <= 255; j++) {
            int value = (int) (slope * j + intIntercept);
            value = Math.max(0, Math.min(255, value));
            table[j] = (byte) (0xff & value);
        }
        return table;
    }

    private static byte @Nullable [] createGammaLookupTable(float amplitude, float exponent, float offset) {
        byte[] table = new byte[256];
        for (int j = 0; j <= 255; j++) {
            int value = (int) Math.round(255 * (amplitude * Math.pow(j / 255f, exponent) + offset));
            value = Math.max(0, Math.min(255, value));
            table[j] = (byte) (value & 0xff);
        }
        return table;
    }


    @ElementCategories(Category.TransferFunctionElement)
    @PermittedContent(
        anyOf = {Animate.class, Set.class}
    )
    public static final class FeFuncR extends TransferFunctionElement {
        public static final String TAG = "fefuncr";

        public FeFuncR() {
            super(Channel.Red);
        }

        @Override
        public @NotNull String tagName() {
            return TAG;
        }
    }

    @ElementCategories(Category.TransferFunctionElement)
    @PermittedContent(
        anyOf = {Animate.class, Set.class}
    )
    public static final class FeFuncG extends TransferFunctionElement {
        public static final String TAG = "fefuncg";

        public FeFuncG() {
            super(Channel.Green);
        }

        @Override
        public @NotNull String tagName() {
            return TAG;
        }
    }

    @ElementCategories(Category.TransferFunctionElement)
    @PermittedContent(
        anyOf = {Animate.class, Set.class}
    )
    public static final class FeFuncB extends TransferFunctionElement {
        public static final String TAG = "fefuncb";

        public FeFuncB() {
            super(Channel.Blue);
        }

        @Override
        public @NotNull String tagName() {
            return TAG;
        }
    }

    @ElementCategories(Category.TransferFunctionElement)
    @PermittedContent(
        anyOf = {Animate.class, Set.class}
    )
    public static final class FeFuncA extends TransferFunctionElement {
        public static final String TAG = "fefunca";

        public FeFuncA() {
            super(Channel.Alpha);
        }

        @Override
        public @NotNull String tagName() {
            return TAG;
        }
    }
}
