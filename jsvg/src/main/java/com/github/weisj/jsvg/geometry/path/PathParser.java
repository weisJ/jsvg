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
package com.github.weisj.jsvg.geometry.path;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.util.ParserBase;

/**
 * A helper for parsing {@link PathCommand}s.
 *
 * @author Jannis Weis
 */
public final class PathParser extends ParserBase {
    private static final Logger LOGGER = Logger.getLogger(PathParser.class.getName());
    private char currentCommand;

    public PathParser(@NotNull String input) {
        super(input, 0);
    }

    private boolean isCommandChar(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    @Override
    public float nextFloat() throws NumberFormatException {
        float f = super.nextFloat();
        consumeWhiteSpaceOrSeparator();
        return f;
    }

    private boolean nextFlag() {
        char c = peek();
        consume();
        consumeWhiteSpaceOrSeparator();
        if (c == '1') {
            return true;
        } else if (c == '0') {
            return false;
        } else {
            throw new IllegalStateException("Invalid flag value '" + c + "' " + currentLocation());
        }
    }

    public @NotNull BezierPathCommand parseMeshCommand() {
        char peekChar = peek();
        currentCommand = 'z';
        if (isCommandChar(peekChar)) {
            consume();
            currentCommand = peekChar;
        }
        consumeWhiteSpaceOrSeparator();
        switch (currentCommand) {
            case 'l':
                return new LineToBezier(true, nextFloatOrUnspecified(), nextFloatOrUnspecified());
            case 'L':
                return new LineToBezier(false, nextFloatOrUnspecified(), nextFloatOrUnspecified());
            case 'c':
                return new CubicBezierCommand(true, nextFloat(), nextFloat(), nextFloat(), nextFloat(),
                        nextFloatOrUnspecified(), nextFloatOrUnspecified());
            case 'C':
                return new CubicBezierCommand(false, nextFloat(), nextFloat(), nextFloat(), nextFloat(),
                        nextFloatOrUnspecified(), nextFloatOrUnspecified());
            default:
                throw new IllegalStateException("Only commands c C l L allowed");
        }
    }

    public PathCommand[] parsePathCommand() {
        if ("none".equals(input)) return new PathCommand[0];
        List<PathCommand> commands = new ArrayList<>();
        try {
            parsePathCommandInto(commands);
        } catch (Exception e) {
            LOGGER.info("Error parsing path command: " + e.getMessage());
        }
        return commands.toArray(new PathCommand[0]);
    }

    private void parsePathCommandInto(List<PathCommand> commands) {
        currentCommand = 'Z';
        while (hasNext()) {
            char peekChar = peek();
            if (isCommandChar(peekChar)) {
                consume();
                currentCommand = peekChar;
            }
            consumeWhiteSpaceOrSeparator();

            if (currentCommand != 'M' && currentCommand != 'm' && commands.isEmpty()) {
                throw new IllegalArgumentException("Path must start with a move command " + currentLocation());
            }

            PathCommand cmd;
            switch (currentCommand) {
                case 'M':
                    cmd = new MoveTo(false, nextFloat(), nextFloat());
                    currentCommand = 'L';
                    break;
                case 'm':
                    cmd = new MoveTo(true, nextFloat(), nextFloat());
                    currentCommand = 'l';
                    break;
                case 'L':
                    cmd = new LineTo(false, nextFloat(), nextFloat());
                    break;
                case 'l':
                    cmd = new LineTo(true, nextFloat(), nextFloat());
                    break;
                case 'H':
                    cmd = new Horizontal(false, nextFloat());
                    break;
                case 'h':
                    cmd = new Horizontal(true, nextFloat());
                    break;
                case 'V':
                    cmd = new Vertical(false, nextFloat());
                    break;
                case 'v':
                    cmd = new Vertical(true, nextFloat());
                    break;
                case 'A':
                    cmd = new Arc(false, nextFloat(), nextFloat(), nextFloat(),
                            nextFlag(), nextFlag(), nextFloat(), nextFloat());
                    break;
                case 'a':
                    cmd = new Arc(true, nextFloat(), nextFloat(), nextFloat(),
                            nextFlag(), nextFlag(), nextFloat(), nextFloat());
                    break;
                case 'Q':
                    cmd = new Quadratic(false, nextFloat(), nextFloat(),
                            nextFloat(), nextFloat());
                    break;
                case 'q':
                    cmd = new Quadratic(true, nextFloat(), nextFloat(),
                            nextFloat(), nextFloat());
                    break;
                case 'T':
                    cmd = new QuadraticSmooth(false, nextFloat(), nextFloat());
                    break;
                case 't':
                    cmd = new QuadraticSmooth(true, nextFloat(), nextFloat());
                    break;
                case 'C':
                    cmd = new Cubic(false, nextFloat(), nextFloat(),
                            nextFloat(), nextFloat(),
                            nextFloat(), nextFloat());
                    break;
                case 'c':
                    cmd = new Cubic(true, nextFloat(), nextFloat(),
                            nextFloat(), nextFloat(),
                            nextFloat(), nextFloat());
                    break;
                case 'S':
                    cmd = new CubicSmooth(false, nextFloat(), nextFloat(),
                            nextFloat(), nextFloat());
                    break;
                case 's':
                    cmd = new CubicSmooth(true, nextFloat(), nextFloat(),
                            nextFloat(), nextFloat());
                    break;
                case 'Z':
                case 'z':
                    cmd = new Terminal();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid path element " + currentCommand + currentLocation());
            }
            commands.add(cmd);
        }
    }
}
