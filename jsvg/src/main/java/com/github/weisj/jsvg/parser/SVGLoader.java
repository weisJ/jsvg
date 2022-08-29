/*
 * MIT License
 *
 * Copyright (c) 2021-2022 Jannis Weis
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
import java.util.function.Supplier;
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
import com.github.weisj.jsvg.attributes.AttributeParser;
import com.github.weisj.jsvg.nodes.*;
import com.github.weisj.jsvg.nodes.filter.*;
import com.github.weisj.jsvg.nodes.mesh.MeshGradient;
import com.github.weisj.jsvg.nodes.mesh.MeshPatch;
import com.github.weisj.jsvg.nodes.mesh.MeshRow;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.nodes.text.TextPath;
import com.github.weisj.jsvg.nodes.text.TextSpan;

/**
 * Class for loading svg files as an {@link SVGDocument}.
 * Note that this class isn't guaranteed to be thread safe and hence shouldn't be used across multiple threads.
 */
public class SVGLoader {

    static final Logger LOGGER = Logger.getLogger(SVGLoader.class.getName());
    private static final @NotNull Map<String, Supplier<SVGNode>> NODE_CONSTRUCTOR_MAP = createNodeConstructorMap();

    private final @NotNull SAXParser saxParser;

    public SVGLoader() {
        this(createSaxParser());
    }

    public SVGLoader(@NotNull SAXParser saxParser) {
        this.saxParser = saxParser;
    }

    private static @NotNull Map<String, Supplier<SVGNode>> createNodeConstructorMap() {
        Map<String, Supplier<SVGNode>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.put(Anchor.TAG, Anchor::new);
        map.put(Circle.TAG, Circle::new);
        map.put(ClipPath.TAG, ClipPath::new);
        map.put(Defs.TAG, Defs::new);
        map.put(Desc.TAG, Desc::new);
        map.put(Ellipse.TAG, Ellipse::new);
        map.put(FeColorMatrix.TAG, FeColorMatrix::new);
        map.put(FeDisplacementMap.TAG, FeDisplacementMap::new);
        map.put(FeGaussianBlur.TAG, FeGaussianBlur::new);
        map.put(FeTurbulence.TAG, FeTurbulence::new);
        map.put(Filter.TAG, Filter::new);
        map.put(Group.TAG, Group::new);
        map.put(Image.TAG, Image::new);
        map.put(Line.TAG, Line::new);
        map.put(LinearGradient.TAG, LinearGradient::new);
        map.put(Marker.TAG, Marker::new);
        map.put(Mask.TAG, Mask::new);
        map.put(MeshGradient.TAG, MeshGradient::new);
        map.put(MeshPatch.TAG, MeshPatch::new);
        map.put(MeshRow.TAG, MeshRow::new);
        map.put(Metadata.TAG, Metadata::new);
        map.put(Path.TAG, Path::new);
        map.put(Pattern.TAG, Pattern::new);
        map.put(Polygon.TAG, Polygon::new);
        map.put(Polyline.TAG, Polyline::new);
        map.put(RadialGradient.TAG, RadialGradient::new);
        map.put(Rect.TAG, Rect::new);
        map.put(SVG.TAG, SVG::new);
        map.put(SolidColor.TAG, SolidColor::new);
        map.put(Stop.TAG, Stop::new);
        map.put(Style.TAG, Style::new);
        map.put(Symbol.TAG, Symbol::new);
        map.put(Text.TAG, Text::new);
        map.put(TextPath.TAG, TextPath::new);
        map.put(TextSpan.TAG, TextSpan::new);
        map.put(Title.TAG, Title::new);
        map.put(Use.TAG, Use::new);
        map.put(View.TAG, View::new);

        map.put(FeBlend.TAG, FeBlend::new);

        map.put("feComponentTransfer", () -> new DummyFilterPrimitive("feComponentTransfer"));
        map.put("feComposite", () -> new DummyFilterPrimitive("feComposite"));
        map.put("feConvolveMatrix", () -> new DummyFilterPrimitive("feConvolveMatrix"));
        map.put("feDiffuseLightning", () -> new DummyFilterPrimitive("feDiffuseLightning"));
        map.put("feDisplacementMap", () -> new DummyFilterPrimitive("feDisplacementMap"));
        map.put("feDropShadow", () -> new DummyFilterPrimitive("feDropShadow"));
        map.put("feFlood", () -> new DummyFilterPrimitive("feFlood"));
        map.put("feFuncA", () -> new DummyFilterPrimitive("feFuncA"));
        map.put("feFuncB", () -> new DummyFilterPrimitive("feFuncB"));
        map.put("feFuncG", () -> new DummyFilterPrimitive("feFuncG"));
        map.put("feFuncR", () -> new DummyFilterPrimitive("feFuncR"));
        map.put("feImage", () -> new DummyFilterPrimitive("feImage"));
        map.put("feMerge", () -> new DummyFilterPrimitive("feMerge"));
        map.put("feMorphology", () -> new DummyFilterPrimitive("feMorphology"));
        map.put("feOffset", () -> new DummyFilterPrimitive("feOffset"));
        map.put("feSpecularLighting", () -> new DummyFilterPrimitive("feSpecularLighting"));
        map.put("feTile", () -> new DummyFilterPrimitive("feTile"));

        return map;
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

    interface LoadHelper {
        @NotNull
        AttributeParser attributeParser();

        @NotNull
        ResourceLoader resourceLoader();
    }

    private static class SVGLoadHandler extends DefaultHandler implements LoadHelper {

        private static final boolean DEBUG_PRINT = false;
        private final PrintStream printer = System.out;
        private int nestingLevel = 0;
        private String ident = "";

        private final Map<String, Object> namedElements = new HashMap<>();
        private final Deque<ParsedElement> currentNodeStack = new ArrayDeque<>();

        private ParsedElement rootNode;

        private final @NotNull AttributeParser attributeParser;
        private final @NotNull ResourceLoader resourceLoader;
        private final @NotNull ParserProvider parserProvider;

        private SVGLoadHandler(@NotNull ParserProvider parserProvider, @NotNull ResourceLoader resourceLoader) {
            this.attributeParser = new AttributeParser(parserProvider.createPaintParser());
            this.resourceLoader = resourceLoader;
            this.parserProvider = parserProvider;
        }

        @Override
        public @NotNull AttributeParser attributeParser() {
            return attributeParser;
        }

        @Override
        public @NotNull ResourceLoader resourceLoader() {
            return resourceLoader;
        }

        private void setIdent(int level) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < level; i++) {
                builder.append(" ");
            }
            ident = builder.toString();
        }

        private boolean isBlank(String text) {
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) > ' ') return false;
            }
            return true;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (DEBUG_PRINT) {
                printer.print(ident);
                printer.print("<" + localName);
                for (int i = 0, end = attributes.getLength(); i < end; i++) {
                    printer.println();
                    printer.print(ident);
                    printer.print(" ");
                    printer.print(attributes.getQName(i));
                    printer.print(" = ");
                    printer.print(attributes.getValue(i));
                }
                printer.println(">");
                setIdent(++nestingLevel);
            }
            ParsedElement lastParsedElement = currentNodeStack.isEmpty()
                    ? null
                    : currentNodeStack.peek();

            if (lastParsedElement != null) flushText(lastParsedElement, true);

            Supplier<SVGNode> nodeSupplier = NODE_CONSTRUCTOR_MAP.get(localName.toLowerCase(Locale.ENGLISH));
            if (nodeSupplier != null) {
                SVGNode newNode = nodeSupplier.get();

                Map<String, String> attrs = new HashMap<>(attributes.getLength());
                for (int i = 0; i < attributes.getLength(); i++) {
                    attrs.put(attributes.getQName(i), attributes.getValue(i));
                }

                ParsedElement parsedElement = new ParsedElement(
                        attributes.getValue("id"),
                        new AttributeNode(qName, attrs, lastParsedElement != null
                                ? lastParsedElement.attributeNode()
                                : null, namedElements, this),
                        newNode);

                if (lastParsedElement != null) {
                    lastParsedElement.addChild(parsedElement);
                }
                if (rootNode == null) rootNode = parsedElement;

                currentNodeStack.push(parsedElement);
                String id = parsedElement.id();
                if (id != null && !namedElements.containsKey(id)) {
                    namedElements.put(id, parsedElement);
                }
            } else {
                LOGGER.warning("No node registered for tag " + localName);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (DEBUG_PRINT) {
                setIdent(--nestingLevel);
                printer.print(ident);
                printer.println("</" + localName + ">");
            }
            if (!currentNodeStack.isEmpty() && currentNodeStack.peek().attributeNode().tagName().equals(qName)) {
                flushText(currentNodeStack.pop(), false);
            }
        }

        private void flushText(@NotNull ParsedElement element, boolean segmentBreak) {
            if (element.characterDataParser != null && element.characterDataParser.canFlush(segmentBreak)) {
                element.node().addContent(element.characterDataParser.flush(segmentBreak));
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (DEBUG_PRINT) {
                String text = new String(ch, start, length).replace("\n", "\\n");
                if (!isBlank(text)) {
                    printer.print(ident);
                    printer.print("__");
                    printer.print(text);
                    printer.println("__");
                }
            }
            if (!currentNodeStack.isEmpty() && currentNodeStack.peek().characterDataParser != null) {
                currentNodeStack.peek().characterDataParser.append(ch, start, length);
            }
        }

        @NotNull
        SVGDocument getDocument() {
            DomProcessor preProcessor = parserProvider.createPreProcessor();
            if (preProcessor != null) preProcessor.process(rootNode);
            rootNode.build();
            DomProcessor postProcessor = parserProvider.createPostProcessor();
            if (postProcessor != null) postProcessor.process(rootNode);
            return new SVGDocument((SVG) rootNode.node());
        }
    }

}
