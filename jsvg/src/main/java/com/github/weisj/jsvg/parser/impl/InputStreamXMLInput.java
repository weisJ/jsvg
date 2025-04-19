/*
 * MIT License
 *
 * Copyright (c) 2025 Jannis Weis
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

import java.io.InputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.XMLInput;

public final class InputStreamXMLInput implements XMLInput {
    private final @NotNull XMLInputFactory xmlInputFactory;
    private final @NotNull InputStream inputStream;

    public InputStreamXMLInput(@NotNull XMLInputFactory xmlInputFactory, @NotNull InputStream inputStream) {
        this.xmlInputFactory = xmlInputFactory;
        this.inputStream = inputStream;
    }

    @Override
    public @NotNull XMLEventReader createReader() throws XMLStreamException {
        return xmlInputFactory.createXMLEventReader(inputStream);
    }
}
