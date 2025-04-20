/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jannis Weis
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
package com.github.weisj.jsvg.parser.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.ReferenceTest;
import com.github.weisj.jsvg.parser.LoaderContext;

class UseValidationTest {

    private final StaxSVGLoader loader = new StaxSVGLoader();

    private void tryLoad(@NotNull String path) throws IOException, XMLStreamException {
        URL url = Objects.requireNonNull(ReferenceTest.class.getResource(path));
        try (InputStream stream = url.openStream()) {
            loader.load(loader.createXMLInput(stream), url.toURI(), LoaderContext.createDefault());
        } catch (URISyntaxException e) {
            Assertions.fail(e);
        }
    }

    @Test
    void detectReferenceCycle() {
        assertThrows(IllegalStateException.class, () -> tryLoad("parser/useCycle.svg"));
        assertThrows(IllegalStateException.class, () -> tryLoad("parser/useCycleSelfReference.svg"));
    }

    @Test
    void detectDeepNesting() {
        assertThrows(IllegalStateException.class, () -> tryLoad("parser/useNesting.svg"));
    }

    @Test
    void detectManyImplicitPaths() {
        assertThrows(IllegalStateException.class, () -> tryLoad("parser/manyImplicitPathsThroughUse.svg"));
    }
}
