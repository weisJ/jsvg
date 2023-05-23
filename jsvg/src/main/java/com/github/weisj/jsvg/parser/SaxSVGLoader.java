/*
 * MIT License
 *
 * Copyright (c) 2023 Jannis Weis
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.github.weisj.jsvg.SVGDocument;

public final class SaxSVGLoader {
    private static final Logger LOGGER = Logger.getLogger(SaxSVGLoader.class.getName());

    private final @NotNull SAXParser saxParser = createSaxParser();
    private final @NotNull NodeSupplier nodeSupplier;

    public SaxSVGLoader(@NotNull NodeSupplier nodeSupplier) {
        this.nodeSupplier = nodeSupplier;
    }

    private static @NotNull SAXParser createSaxParser() {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        try {
            SAXParser parser = saxParserFactory.newSAXParser();
            setParserProperty(parser, XMLConstants.ACCESS_EXTERNAL_DTD);
            setParserProperty(parser, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
            return parser;
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setParserProperty(@NotNull SAXParser parser, @NotNull String property) {
        try {
            parser.setProperty(property, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            // We don't care if when the properties aren't recognized or supported.
        }
    }

    public @Nullable SVGDocument load(
            @Nullable InputStream inputStream,
            @NotNull ParserProvider parserProvider,
            @NotNull ResourceLoader resourceLoader) {
        if (inputStream == null) return null;
        try {
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setEntityResolver(
                    (publicId, systemId) -> {
                        // Ignore all DTDs
                        return new InputSource(new ByteArrayInputStream(new byte[0]));
                    });
            SVGLoadHandler handler = new SVGLoadHandler(parserProvider, resourceLoader, nodeSupplier);
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(inputStream));
            return handler.getDocument();
        } catch (SAXParseException e) {
            LOGGER.log(Level.WARNING, "Error processing ", e);
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Could not load SVG ", e);
        }
        return null;
    }

    private static final class SVGLoadHandler extends DefaultHandler {

        private final SVGDocumentBuilder documentBuilder;

        private SVGLoadHandler(
                @NotNull ParserProvider parserProvider,
                @NotNull ResourceLoader resourceLoader,
                @NotNull NodeSupplier nodeSupplier) {
            documentBuilder = new SVGDocumentBuilder(parserProvider, resourceLoader, nodeSupplier);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            documentBuilder.startElement(qName);
            Map<String, String> attrs = new HashMap<>(attributes.getLength());
            for (int i = 0; i < attributes.getLength(); i++) {
                attrs.put(attributes.getQName(i), attributes.getValue(i).trim());
            }
            documentBuilder.addAttributes(attrs);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            documentBuilder.endElement(qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            documentBuilder.addTextContent(ch, start, length);
        }

        @NotNull
        SVGDocument getDocument() {
            return documentBuilder.build();
        }
    }
}
