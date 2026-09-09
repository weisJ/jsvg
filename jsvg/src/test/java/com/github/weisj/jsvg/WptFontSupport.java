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
package com.github.weisj.jsvg;

import java.awt.Font;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mabb.fontverter.opentype.HeadTable;
import org.mabb.fontverter.opentype.OpenTypeFont;
import org.mabb.fontverter.opentype.OpenTypeTable;
import org.mabb.fontverter.opentype.SfntHeader;
import org.mabb.fontverter.woff.WoffFont;
import org.mabb.fontverter.woff.WoffParser;
import org.mabb.fontverter.woff.WoffTable;

import com.github.weisj.jsvg.renderer.PlatformSupport;

/** Fonts declared by the standalone SVG reftests; scoped to this suite's renderer. */
record WptFontSupport(@NotNull Map<String, Font> fonts) implements PlatformSupport {
    static @NotNull WptFontSupport load(@NotNull Path repository) throws Exception {
        Font ahem = Font.createFont(Font.TRUETYPE_FONT, repository.resolve("fonts/Ahem.ttf").toFile());
        // The reftests use FreeSans.woff, but their old relative fonts/ URLs are missing upstream.
        // WPT still bundles the font with its SVG 1.1 imports. Decode that exact font in memory.
        byte[] freeSans = decodeFreeSans(
                Files.readAllBytes(repository.resolve("svg/import/woffs/FreeSans.woff")));
        Font sans = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(freeSans));
        return new WptFontSupport(Map.of("ahem", ahem, "freesans", sans));
    }

    static byte[] decodeFreeSans(byte[] data) throws IOException {
        WoffFont woff = new WoffParser().parse(data);
        OpenTypeFont font = new OpenTypeFont();
        font.getSfntHeader().sfntFlavor = SfntHeader.CFF_FLAVOR;
        // The high-level converter rebuilds CFF horizontal metrics using incomplete glyph-name
        // lookups. Use FontVerter's reader and writer with opaque tables instead.
        for (WoffTable table : woff.getTables()) {
            if ("head".equals(table.getTag())) {
                HeadTable head = new HeadTable();
                head.readData(table.getTableData());
                font.addTable(head);
            } else {
                font.addTable(new OpenTypeTable() {
                    @Override
                    public String getTableType() {
                        return table.getTag();
                    }

                    @Override
                    protected byte[] generateUnpaddedData() {
                        return table.getTableData();
                    }
                });
            }
        }
        if (font.getHead() == null || woff.getTables().stream().noneMatch(t -> "CFF ".equals(t.getTag()))) {
            throw new IOException("Expected the bundled CFF FreeSans font with a head table");
        }
        return font.getData();
    }

    @Override
    public @Nullable ImageObserver imageObserver() {
        return null;
    }

    @Override
    public @Nullable TargetSurface targetSurface() {
        return null;
    }

    @Override
    public @Nullable Font customFont(@NotNull String family) {
        return fonts.get(family.toLowerCase(Locale.ROOT));
    }
}
