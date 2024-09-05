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

class UseValidationTest {

    private final StaxSVGLoader loader = new StaxSVGLoader(new NodeSupplier());

    private void tryLoad(@NotNull String path) throws IOException, XMLStreamException {
        URL url = Objects.requireNonNull(UseValidationTest.class.getResource(path));
        try (InputStream stream = url.openStream()) {
            loader.load(stream, url.toURI(), LoaderContext.createDefault());
        } catch (URISyntaxException e) {
            Assertions.fail(e);
        }
    }

    @Test
    void detectReferenceCycle() {
        assertThrows(IllegalStateException.class, () -> tryLoad("useCycle.svg"));
        assertThrows(IllegalStateException.class, () -> tryLoad("useCycleSelfReference.svg"));
    }

    @Test
    void detectDeepNesting() {
        assertThrows(IllegalStateException.class, () -> tryLoad("useNesting.svg"));
    }
}
