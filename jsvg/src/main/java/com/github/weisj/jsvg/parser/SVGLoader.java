/*
 * MIT License
 *
 * Copyright (c) 2021-2023 Jannis Weis
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

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.github.weisj.jsvg.SVGDocument;

/**
 * Class for loading svg files as an {@link SVGDocument}.
 * Note that this class isn't guaranteed to be thread safe and hence shouldn't be used across multiple threads.
 */
public final class SVGLoader {

    static final Logger LOGGER = Logger.getLogger(SVGLoader.class.getName());
    private static final @NotNull NodeSupplier NODE_SUPPLIER = new NodeSupplier();

    private final @NotNull SAXParser saxParser;

    public SVGLoader() {
        this(createSaxParser());
    }

    public SVGLoader(@NotNull SAXParser saxParser) {
        this.saxParser = saxParser;
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

    public @Nullable SVGDocument load(@NotNull URL xmlBase) {
        return load(xmlBase, new DefaultParserProvider());
    }


    public @Nullable SVGDocument load(@NotNull URL xmlBase, @NotNull ParserProvider parserProvider) {
        try {
            return load(xmlBase.openStream(), parserProvider);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read " + xmlBase, e);
        }
        return null;
    }

    public @Nullable SVGDocument load(@NotNull InputStream inputStream) {
        return load(inputStream, new DefaultParserProvider());
    }

    public @Nullable SVGDocument load(@NotNull InputStream inputStream, @NotNull ParserProvider parserProvider) {
        return load(inputStream, parserProvider, new SynchronousResourceLoader());
    }


    public @Nullable SVGDocument load(@NotNull InputStream inputStream,
            @NotNull ParserProvider parserProvider,
            @NotNull ResourceLoader resourceLoader) {
        try {
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setEntityResolver(
                    (publicId, systemId) -> {
                        // Ignore all DTDs
                        return new InputSource(new ByteArrayInputStream(new byte[0]));
                    });
            SVGLoadHandler handler = new SVGLoadHandler(parserProvider, resourceLoader);
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(createDocumentInputStream(inputStream)));
            return handler.getDocument();
        } catch (SAXParseException e) {
            LOGGER.log(Level.WARNING, "Error processing ", e);
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Could not load SVG ", e);
        }
        return null;
    }

    private InputStream createDocumentInputStream(@NotNull InputStream is) throws IOException {
        BufferedInputStream bin = new BufferedInputStream(is);
        bin.mark(2);
        int b0 = bin.read();
        int b1 = bin.read();
        bin.reset();

        // Check for gzip magic number
        if ((b1 << 8 | b0) == GZIPInputStream.GZIP_MAGIC) {
            return new GZIPInputStream(bin);
        } else {
            // Plain text
            return bin;
        }
    }

    private static final class SVGLoadHandler extends DefaultHandler {

        private final SVGDocumentBuilder documentBuilder;

        private SVGLoadHandler(@NotNull ParserProvider parserProvider, @NotNull ResourceLoader resourceLoader) {
            documentBuilder = new SVGDocumentBuilder(parserProvider, resourceLoader, NODE_SUPPLIER);
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
