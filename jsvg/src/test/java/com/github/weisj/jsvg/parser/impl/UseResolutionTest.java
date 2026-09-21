/*
 * MIT License
 *
 * Copyright (c) 2026 Jannis Weis
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

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.Use;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.parser.LoaderContext;

class UseResolutionTest {

    @Test
    void resolvesPositionDependentTargetToRegisteredInstance() throws XMLStreamException {
        SVGDocumentBuilder builder = parse("""
                <svg xmlns='http://www.w3.org/2000/svg'>
                  <style>.special circle { fill: red }</style>
                  <defs class='special'><circle id='target'/></defs>
                  <use id='first' href='#target'/>
                  <use id='second' href='#target'/>
                </svg>
                """);
        ParsedDocument document = builder.parsedDocument();
        ParsedElement source = Objects.requireNonNull(document.getElementById(ParsedElement.class, "target"));
        ParsedElement firstElement = Objects.requireNonNull(document.getElementById(ParsedElement.class, "first"));
        ParsedElement secondElement = Objects.requireNonNull(document.getElementById(ParsedElement.class, "second"));

        builder.build();

        Use first = (Use) firstElement.node();
        Use second = (Use) secondElement.node();
        ParsedElement instance = Objects.requireNonNull(document.useTargetFor(source));
        assertNotSame(source.node(), first.referencedNode());
        assertSame(instance.node(), first.referencedNode());
        assertSame(first.referencedNode(), second.referencedNode());
    }

    @Test
    void resolvesPositionIndependentTargetDirectly() throws XMLStreamException {
        SVGDocumentBuilder builder = parse("""
                <svg xmlns='http://www.w3.org/2000/svg'>
                  <style>circle { fill: red }</style>
                  <defs><circle id='target'/></defs>
                  <use id='instance' href='#target'/>
                </svg>
                """);
        ParsedDocument document = builder.parsedDocument();
        ParsedElement source = Objects.requireNonNull(document.getElementById(ParsedElement.class, "target"));
        ParsedElement useElement = Objects.requireNonNull(document.getElementById(ParsedElement.class, "instance"));

        builder.build();

        assertSame(source, document.useTargetFor(source));
        assertSame(source.node(), ((Use) useElement.node()).referencedNode());
    }

    @Test
    void hrefIsReadOnlyFromDeclaredAttributes() throws XMLStreamException {
        SVGDocumentBuilder builder = parse("""
                <svg xmlns='http://www.w3.org/2000/svg'>
                  <defs><circle id='target'/></defs>
                  <use id='instance' style='href: #target'/>
                </svg>
                """);
        ParsedDocument document = builder.parsedDocument();
        ParsedElement useElement = Objects.requireNonNull(document.getElementById(ParsedElement.class, "instance"));

        builder.build();

        assertNull(((Use) useElement.node()).referencedNode());
    }

    @Test
    void doesNotMaterializeUseTargetBelowNonRenderedTree() throws XMLStreamException {
        SVGDocumentBuilder builder = parse("""
                <svg xmlns='http://www.w3.org/2000/svg'>
                  <style>.special circle { fill: red }</style>
                  <defs class='special' style='display: block'>
                    <circle id='target'/>
                    <use id='unused' href='#target'/>
                  </defs>
                </svg>
                """);
        ParsedElement useElement = Objects.requireNonNull(
                builder.parsedDocument().getElementById(ParsedElement.class, "unused"));

        builder.build();

        assertNull(((Use) useElement.node()).referencedNode());
    }

    @Test
    void doesNotMaterializeUseCycleBelowNonRenderedTree() throws XMLStreamException {
        SVGDocumentBuilder builder = parse("""
                <svg xmlns='http://www.w3.org/2000/svg'>
                  <defs>
                    <use id='first' href='#second'/>
                    <use id='second' href='#first'/>
                  </defs>
                </svg>
                """);
        ParsedDocument document = builder.parsedDocument();
        ParsedElement firstElement = Objects.requireNonNull(document.getElementById(ParsedElement.class, "first"));
        ParsedElement secondElement = Objects.requireNonNull(document.getElementById(ParsedElement.class, "second"));

        builder.build();

        assertNull(((Use) firstElement.node()).referencedNode());
        assertNull(((Use) secondElement.node()).referencedNode());
    }

    @Test
    void specNeverRenderedElementsAreMarkedInTheirMetadata() {
        NodeSupplier nodeSupplier = new NodeSupplier();
        for (String tagName : new String[] {
                "clippath", "defs", "desc", "lineargradient", "marker", "mask", "metadata",
                "pattern", "radialgradient", "script", "style", "title", "symbol"
        }) {
            SVGNode node = Objects.requireNonNull(nodeSupplier.create(tagName));
            assertTrue(Category.elementCategoriesOf(node).neverRendered(), tagName);
        }
    }

    private static @NotNull SVGDocumentBuilder parse(@NotNull String svg) throws XMLStreamException {
        byte[] bytes = svg.getBytes(StandardCharsets.UTF_8);
        SVGDocumentBuilder builder = new StaxSVGLoader().parse(
                new ByteArrayInputStream(bytes), null, LoaderContext.createDefault());
        if (builder == null) throw new AssertionError("SVG did not parse");
        return builder;
    }
}
