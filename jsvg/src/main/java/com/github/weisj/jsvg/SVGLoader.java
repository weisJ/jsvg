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
package com.github.weisj.jsvg;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.github.weisj.jsvg.nodes.*;
import com.github.weisj.jsvg.nodes.text.Text;
import com.github.weisj.jsvg.nodes.text.TextPath;
import com.github.weisj.jsvg.nodes.text.TextSpan;

public class SVGLoader {

    static final Logger LOGGER = Logger.getLogger(SVGLoader.class.getName());
    private final Map<String, Supplier<SVGNode>> nodeMap;

    private final XMLReader xmlReader;

    public SVGLoader() throws ParserConfigurationException, SAXException {
        nodeMap = new HashMap<>();
        nodeMap.put(Anchor.TAG, Anchor::new);
        nodeMap.put(Circle.TAG, Circle::new);
        nodeMap.put(ClipPath.TAG, ClipPath::new);
        nodeMap.put(Defs.TAG, Defs::new);
        nodeMap.put(Desc.TAG, Desc::new);
        nodeMap.put(Ellipse.TAG, Ellipse::new);
        nodeMap.put(Group.TAG, Group::new);
        nodeMap.put(Image.TAG, Image::new);
        nodeMap.put(Line.TAG, Line::new);
        nodeMap.put(LinearGradient.TAG, LinearGradient::new);
        nodeMap.put(Metadata.TAG, Metadata::new);
        nodeMap.put(Path.TAG, Path::new);
        nodeMap.put(Polygon.TAG, Polygon::new);
        nodeMap.put(Polyline.TAG, Polyline::new);
        nodeMap.put(RadialGradient.TAG, RadialGradient::new);
        nodeMap.put(Rect.TAG, Rect::new);
        nodeMap.put(SVG.TAG, SVG::new);
        nodeMap.put(SolidColor.TAG, SolidColor::new);
        nodeMap.put(Stop.TAG, Stop::new);
        nodeMap.put(Style.TAG, Style::new);
        nodeMap.put(Text.TAG, Text::new);
        nodeMap.put(TextPath.TAG, TextPath::new);
        nodeMap.put(TextSpan.TAG, TextSpan::new);
        nodeMap.put(Title.TAG, Title::new);
        nodeMap.put(Use.TAG, Use::new);
        /* @formatter:off
         * Todo
         *  - Filters (et al.)
         *  - Font? (deprecated)
         *  - FontFace? (deprecated)
         *  - Glyph? (deprecated)
         *  - HKern? (deprecated)
         *  - Hatch-path? (experimental)
         *  - Hatch? (experimental)
         *  - Image
         *  - Marker
         *  - Mask
         *  - MissingGlyph? (deprecated)
         *  - Pattern
         *  - Style
         *  - Symbol
         *  - VKern? (deprecated)
         * @formatter:on
         */

        /* @formatter:off
         * Todo: **Ideas**
         *  - Turn current parsers into interfaces. Allow users to specify custom parsers.
         *    This would allow to have darklaf recognize dynamic colors and replace them with appropriate
         *    SVGPaint implementations to avoid patching the svg structure itself.
         *    Doing this avoids the need for keeping the SVG structure around for modification
         *    (which isn't in scope for this project).
         * @formatter:ob
         */

        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        SAXParser saxParser = saxParserFactory.newSAXParser();
        xmlReader = saxParser.getXMLReader();
    }

    public SVGDocument load(URI xmlBase) {
        try {
            xmlReader.setEntityResolver(
                    (publicId, systemId) -> {
                        // Ignore all DTDs
                        return new InputSource(new ByteArrayInputStream(new byte[0]));
                    });
            SVGLoadHandler handler = new SVGLoadHandler();
            xmlReader.setContentHandler(handler);
            InputStream is = xmlBase.toURL().openStream();
            xmlReader.parse(new InputSource(createDocumentInputStream(is)));
            return handler.getDocument();
        } catch (SAXParseException e) {
            LOGGER.log(Level.WARNING, "Error processing " + xmlBase, e);
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Could not load SVG " + xmlBase, e);
        }
        return null;
    }

    private InputStream createDocumentInputStream(InputStream is) throws IOException {
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

    private class SVGLoadHandler extends DefaultHandler {

        private static final boolean DEBUG_PRINT = false;
        private final PrintStream printer = System.out;
        private int nestingLevel = 0;
        private String ident = "";

        private final Map<String, SVGNode> namedElements = new HashMap<>();
        private final Deque<ParsedElement> currentNodeStack = new ArrayDeque<>();
        private ParsedElement rootNode;

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

            Supplier<SVGNode> nodeSupplier = nodeMap.get(localName.toLowerCase(Locale.ENGLISH));
            if (nodeSupplier != null) {
                SVGNode newNode = nodeSupplier.get();

                Map<String, String> attrs = new HashMap<>(attributes.getLength());
                for (int i = 0; i < attributes.getLength(); i++) {
                    attrs.put(attributes.getQName(i), attributes.getValue(i));
                }

                ParsedElement parsedElement = new ParsedElement(
                        attributes.getValue("id"),
                        new AttributeNode(qName, attrs, lastParsedElement != null
                                ? lastParsedElement.attributeNode
                                : null, namedElements),
                        newNode);

                if (lastParsedElement != null) {
                    lastParsedElement.addChild(parsedElement);
                }
                if (rootNode == null) rootNode = parsedElement;

                currentNodeStack.push(parsedElement);
                String id = parsedElement.id;
                if (id != null && !namedElements.containsKey(id)) {
                    namedElements.put(id, parsedElement.node);
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
            if (!currentNodeStack.isEmpty() && currentNodeStack.peek().attributeNode.tagName().equals(qName)) {
                flushText(currentNodeStack.pop(), false);
            }
        }

        private void flushText(@NotNull ParsedElement element, boolean segmentBreak) {
            if (element.characterDataParser != null && element.characterDataParser.canFlush(segmentBreak)) {
                element.node.addContent(element.characterDataParser.flush(segmentBreak));
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

        @NotNull SVGDocument getDocument() {
            rootNode.build();
            return new SVGDocument(rootNode.node);
        }
    }

}
