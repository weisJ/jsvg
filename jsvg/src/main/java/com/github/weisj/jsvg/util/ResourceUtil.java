/*
 * MIT License
 *
 * Copyright (c) 2022-2023 Jannis Weis
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.resources.ImageResource;
import com.github.weisj.jsvg.parser.resources.RenderableResource;

public final class ResourceUtil {

    private static final Logger LOGGER = Logger.getLogger(ResourceUtil.class.getName());

    private ResourceUtil() {}

    private static final Set<String> SUPPORTED_MIME_TYPES = Arrays
            .stream(ImageIO.getReaderFormatNames())
            .map(s -> "image/" + s.toLowerCase(Locale.ENGLISH))
            .collect(Collectors.toSet());

    public static @Nullable RenderableResource loadImage(@NotNull URI uri) throws IOException {
        BufferedImage img = loadToBufferedImage(uri);
        if (img == null) return null;
        return new ImageResource(img);
    }

    private static @Nullable BufferedImage loadToBufferedImage(@NotNull URI uri) throws IOException {
        String scheme = uri.getScheme();
        if ("data".equals(scheme)) {
            DataUri dataUri = DataUri.parse(uri.toString(), StandardCharsets.UTF_8);
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
            image = reader.read(0, param); // Don't really need the return value here, as it will always be same value
                                           // as "image"
        } catch (Exception e) {
            // Ignore this exception or display a warning or similar, for exceptions happening during decoding
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return image;
    }
}
