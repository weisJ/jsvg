/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DefaultResourcePolicy implements ExternalResourcePolicy {
    private static final Logger LOGGER = Logger.getLogger(DefaultResourcePolicy.class.getName());

    static int FLAG_ALLOW_RELATIVE = 1;
    static int FLAG_ALLOW_ABSOLUTE = 2;
    static int FLAG_ALLOW_NON_LOCAL = 4;

    private int flags;

    DefaultResourcePolicy(int flags) {
        this.flags = flags;
    }

    @Override
    public @Nullable URI resolveResourceURI(@Nullable URI baseURI, @NotNull String path) {
        URI child;
        try {
            child = new URI(path);
        } catch (URISyntaxException e) {
            LOGGER.info("Failed to resolve URI: " + path);
            return null;
        }

        if (child.isAbsolute()) {
            if ((flags & FLAG_ALLOW_ABSOLUTE) == 0) {
                LOGGER.info(() -> String.format("Rejected URI %s because absolute paths are not allowed", child));
                return null;
            }
            if (!"file".equals(child.getScheme()) && (flags & FLAG_ALLOW_NON_LOCAL) == 0) {
                LOGGER.info(() -> String.format("Rejected URI %s because non-local paths are not allowed", child));
                return null;
            }
            return child;
        } else {
            if (baseURI == null) return null;
            return baseURI.resolve(child);
        }
    }
}
