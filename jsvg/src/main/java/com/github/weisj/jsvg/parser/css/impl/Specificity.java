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
package com.github.weisj.jsvg.parser.css.impl;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.google.errorprone.annotations.Immutable;

@Immutable
final class Specificity implements Comparable<Specificity> {
    private final int idCount;
    private final int classCount;
    private final int elementCount;

    public Specificity(int idCount, int classCount, int elementCount) {
        this.idCount = idCount;
        this.classCount = classCount;
        this.elementCount = elementCount;
    }

    @Override
    public String toString() {
        return "[" + idCount + ", " + classCount + ", " + elementCount + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Specificity that = (Specificity) o;
        return idCount == that.idCount && classCount == that.classCount && elementCount == that.elementCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCount, classCount, elementCount);
    }

    @Override
    public int compareTo(@NotNull Specificity o) {
        if (idCount != o.idCount) {
            return Integer.compare(idCount, o.idCount);
        }
        if (classCount != o.classCount) {
            return Integer.compare(classCount, o.classCount);
        }
        return Integer.compare(elementCount, o.elementCount);
    }
}
