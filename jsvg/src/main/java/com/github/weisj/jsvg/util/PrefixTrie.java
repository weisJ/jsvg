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
package com.github.weisj.jsvg.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public final class PrefixTrie<T> {

    private static final class TrieNode<T> {
        final @NotNull Map<@NotNull Character, @NotNull TrieNode<T>> children = new HashMap<>();
        final @NotNull List<T> values = new ArrayList<>();
    }

    private final @NotNull TrieNode<T> root = new TrieNode<>();

    /**
     * @param key a non-empty key to add to the trie
     * @return the list of values associated with the key so far. Adding an element to the list will update the trie
     */
    public @NotNull List<T> insert(@NotNull String key) {
        TrieNode<T> node = root;

        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            node = node.children.computeIfAbsent(c, k -> new TrieNode<>());
        }

        return node.values;
    }

    public @NotNull List<T> matchPrefixes(String text) {
        List<T> matches = new ArrayList<>();
        TrieNode<T> node = root;

        for (int i = 0; i < text.length(); i++) {
            node = node.children.get(text.charAt(i));

            if (node == null)
                break;

            matches.addAll(node.values);
        }

        return matches;
    }
}
