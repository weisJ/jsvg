/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Jannis Weis
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.darklaf.util.Lambdas;
import com.github.weisj.darklaf.util.StreamUtil;

public final class ResourceWalker implements AutoCloseable {

    private final List<FileSystem> fileSystemList = new ArrayList<>();
    private final String[] packages;
    private Stream<?> stream;

    private ResourceWalker(final String... packages) {
        this.packages = packages;
    }

    public static String[] findIcons(@NotNull Package pack, @NotNull String basePath) {
        String packName = pack.getName();
        try (ResourceWalker walker = ResourceWalker.walkResources(packName)) {
            return walker.stream().filter(p -> p.endsWith("svg"))
                    .map(p -> p.substring(packName.length() + 1))
                    .filter(p -> p.startsWith(basePath))
                    .sorted(ResourceWalker::compareAsPaths)
                    .toArray(String[]::new);
        }
    }

    private static int compareAsPaths(@NotNull String a, @NotNull String b) {
        if (a.contains("/")) {
            if (b.contains("/")) {
                return a.compareTo(b);
            } else {
                return -1;
            }
        }
        if (b.contains("/")) {
            return 1;
        }
        return a.compareTo(b);
    }

    public Stream<String> stream() {
        if (stream != null) throw new IllegalStateException("Stream already open");
        Stream<String> s = Arrays.stream(packages).flatMap(this::walk);
        stream = s;
        return s;
    }

    @Override
    public void close() {
        stream.close();
        stream = null;
        for (FileSystem fileSystem : fileSystemList) {
            try {
                fileSystem.close();
            } catch (final IOException ignored) {
            }
        }
        fileSystemList.clear();
    }

    public static ResourceWalker walkResources(final String... packages) {
        return new ResourceWalker(packages);
    }

    private Stream<String> walk(final String path) {
        String pack = path.replace('.', '/');
        pack = pack.endsWith("/") ? pack : pack + "/";
        String pathName = pack;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Stream<URL> stream = StreamUtil.enumerationAsStream(
                Lambdas.orDefault(classLoader::getResources, Collections.<URL>emptyEnumeration()).apply(pathName));
        return stream.map(Lambdas.wrap(URL::toURI)).flatMap(uri -> {
            if ("jar".equals(uri.getScheme())) {
                try {
                    FileSystem fileSystem;
                    try {
                        fileSystem = FileSystems.getFileSystem(uri);
                    } catch (FileSystemNotFoundException e) {
                        fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    }
                    Path resourcePath = fileSystem.getPath(pathName);
                    fileSystemList.add(fileSystem);
                    return Files.walk(resourcePath, Integer.MAX_VALUE);
                } catch (final IOException e) {
                    return Stream.empty();
                }
            } else {
                return walkFolder(new File(uri)).map(File::toPath);
            }
        }).map(Path::toString).map(p -> p.replace(File.separatorChar, '/')).map(p -> {
            int index = p.indexOf(pathName);
            return index >= 0 ? p.substring(index) : null;
        }).filter(Objects::nonNull);
    }

    private Stream<File> walkFolder(final File file) {
        if (!file.isDirectory()) return Stream.of(file);
        File[] files = file.listFiles();
        if (files == null) files = new File[0];
        return Arrays.stream(files).flatMap(this::walkFolder);
    }
}
