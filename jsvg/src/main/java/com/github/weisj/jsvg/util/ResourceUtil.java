/*
 * MIT License
 *
 * Copyright (c) 2022-2026 Jannis Weis
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
package com.github.weisj.jsvg.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.Logger.Level;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.parser.DomDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.RenderableResource;
import com.github.weisj.jsvg.parser.resources.impl.ImageResource;
import com.github.weisj.jsvg.parser.resources.impl.SVGResource;
import com.github.weisj.jsvg.view.impl.FragmentView;

public final class ResourceUtil {

    private static final Logger LOGGER = LogFactory.createLogger(ResourceUtil.class);

    private ResourceUtil() {}

    private static final Set<String> SUPPORTED_MIME_TYPES = Arrays
            .stream(ImageIO.getReaderFormatNames())
            .map(s -> "image/" + s.toLowerCase(Locale.ENGLISH))
            .collect(Collectors.toSet());

    public static @Nullable RenderableResource loadImage(@NotNull DomDocument document, @NotNull URI uri)
            throws IOException {
        URI resourceUri = document
                .loaderContext()
                .externalResourcePolicy()
                .resolveResourceURI(document.rootURI(), uri);
        if (resourceUri == null) return null;

        URI documentUri = UriUtil.removeFragment(resourceUri);
        DataUri dataUri = null;
        if ("data".equals(resourceUri.getScheme())) {
            dataUri = DataUri.parse(documentUri.toString(), StandardCharsets.UTF_8);
        }

        String path = resourceUri.getPath();
        if ((path != null && path.endsWith(".svg"))
                || (dataUri != null && "image/svg+xml".equalsIgnoreCase(dataUri.mime()))) {
            RenderableResource svg = loadSvg(document, resourceUri, documentUri, dataUri);
            if (svg != null) return svg;
        }

        BufferedImage img = loadToBufferedImage(resourceUri, dataUri);
        if (img == null) return null;
        return new ImageResource(img);
    }

    private static @Nullable RenderableResource loadSvg(@NotNull DomDocument document, @NotNull URI resourceUri,
            @NotNull URI documentUri, @Nullable DataUri dataUri) {
        SVGLoader loader = new SVGLoader();
        try {
            SVGDocument imageDocument = dataUri != null
                    ? loader.load(new ByteArrayInputStream(dataUri.data()), documentUri, document.loaderContext())
                    : loader.load(documentUri.toURL(), document.loaderContext());
            if (imageDocument == null) return null;

            // URI components must be separated before percent-encoded octets are decoded:
            // https://www.w3.org/TR/media-frags/#processing-name-value-components
            String fragment = resourceUri.getRawFragment();
            return new SVGResource(imageDocument, fragment != null ? FragmentView.parse(fragment) : null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load svg resource", e);
            return null;
        }
    }

    private static @Nullable BufferedImage loadToBufferedImage(@NotNull URI uri, @Nullable DataUri parsedDataUri)
            throws IOException {
        String scheme = uri.getScheme();
        if ("data".equals(scheme)) {
            DataUri dataUri = parsedDataUri != null
                    ? parsedDataUri
                    : DataUri.parse(UriUtil.removeFragment(uri).toString(), StandardCharsets.UTF_8);
            if (!isSupportedMimeType(dataUri.mime())) throw new IOException("Unsupported Mime type " + dataUri.mime());
            try (ByteArrayInputStream in = new ByteArrayInputStream(dataUri.data())) {
                return readPossiblyCorruptedFile(in);
            }
        }
        return ImageIO.read(uri.toURL());
    }

    private static boolean isSupportedMimeType(@NotNull String mimeType) {
        return SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase(Locale.ENGLISH));
    }

    private static @Nullable BufferedImage readPossiblyCorruptedFile(@NotNull InputStream inputStream)
            throws IOException {
        ImageInputStream input = ImageIO.createImageInputStream(inputStream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) return null;

        ImageReader reader = readers.next();
        reader.setInput(input);

        // Create destination image to hold possibly partially decoded result
        ImageReadParam param = reader.getDefaultReadParam();
        BufferedImage image = reader.getImageTypes(0).next()
                .createBufferedImage(reader.getWidth(0), reader.getHeight(0));
        param.setDestination(image);

        try {
            image = reader.read(0, param);
            // Don't really need the return value here, as it will always be same value
            // as "image"
        } catch (Exception e) {
            // Ignore this exception or display a warning or similar, for exceptions happening during decoding
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
        return image;
    }
}
