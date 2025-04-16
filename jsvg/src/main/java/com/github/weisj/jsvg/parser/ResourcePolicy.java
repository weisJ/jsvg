/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Jannis Weis
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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.parser.impl.DefaultResourcePolicy;

@ApiStatus.Experimental
public interface ResourcePolicy {
    /**
     * Deny loading of external resources.
     */
    ResourcePolicy DENY_ALL = new DefaultResourcePolicy(0);
    ResourcePolicy DENY_EXTERNAL = new DefaultResourcePolicy(DefaultResourcePolicy.FLAG_ALLOW_EMBEDDED_DATA);
    /**
     * Allow external resources to be loaded relative to the base document.
     */
    ResourcePolicy ALLOW_RELATIVE = new DefaultResourcePolicy(
            DefaultResourcePolicy.FLAG_ALLOW_RELATIVE
                    | DefaultResourcePolicy.FLAG_ALLOW_EMBEDDED_DATA);
    ResourcePolicy ALLOW_ALL = new DefaultResourcePolicy(
            DefaultResourcePolicy.FLAG_ALLOW_RELATIVE
                    | DefaultResourcePolicy.FLAG_ALLOW_ABSOLUTE
                    | DefaultResourcePolicy.FLAG_ALLOW_NON_LOCAL
                    | DefaultResourcePolicy.FLAG_ALLOW_EMBEDDED_DATA);


    @Nullable
    URI resolveResourceURI(@Nullable URI baseDocumentUri, @NotNull String resourcePath);

    @Nullable
    URI resolveResourceURI(@Nullable URI baseDocumentUri, @NotNull URI resourceUri);
}
