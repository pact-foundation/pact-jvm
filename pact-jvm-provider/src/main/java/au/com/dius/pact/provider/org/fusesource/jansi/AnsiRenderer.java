/*
 * Copyright (C) 2009 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.com.dius.pact.provider.org.fusesource.jansi;

import java.util.Locale;

/**
 * Renders ANSI color escape-codes in strings by parsing out some special syntax to pick up the correct fluff to use.
 *
 * The syntax for embedded ANSI codes is:
 *
 * <pre>
 *   <tt>@|</tt><em>code</em>(<tt>,</tt><em>code</em>)* <em>text</em><tt>|@</tt>
 * </pre>
 *
 * Examples:
 *
 * <pre>
 *   <tt>@|bold Hello|@</tt>
 * </pre>
 *
 * <pre>
 *   <tt>@|bold,red Warning!|@</tt>
 * </pre>
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @since 1.1
 */
public class AnsiRenderer
{
    public static final String BEGIN_TOKEN = "@|";

    private static final int BEGIN_TOKEN_LEN = 2;

    public static final String END_TOKEN = "|@";

    private static final int END_TOKEN_LEN = 2;

    public static final String CODE_TEXT_SEPARATOR = " ";

    public static final String CODE_LIST_SEPARATOR = ",";

    private AnsiRenderer() {}

    static public String render(final String input) throws IllegalArgumentException {
        StringBuffer buff = new StringBuffer();

        int i = 0;
        int j, k;

        while (true) {
            j = input.indexOf(BEGIN_TOKEN, i);
            if (j == -1) {
                if (i == 0) {
                    return input;
                }
                else {
                    buff.append(input.substring(i, input.length()));
                    return buff.toString();
                }
            }
            else {
                buff.append(input.substring(i, j));
                k = input.indexOf(END_TOKEN, j);

                if (k == -1) {
                    return input;
                }
                else {
                    j += BEGIN_TOKEN_LEN;
                    String spec = input.substring(j, k);

                    String[] items = spec.split(CODE_TEXT_SEPARATOR, 2);
                    if (items.length == 1) {
                        return input;
                    }
                    String replacement = render(items[1], items[0].split(CODE_LIST_SEPARATOR));

                    buff.append(replacement);

                    i = k + END_TOKEN_LEN;
                }
            }
        }
    }

    static private String render(final String text, final String... codes) {
        au.com.dius.pact.provider.org.fusesource.jansi.Ansi ansi = au.com.dius.pact.provider.org.fusesource.jansi.Ansi.ansi();
        for (String name : codes) {
            Code code = Code.valueOf(name.toUpperCase(Locale.ENGLISH));

            if (code.isColor()) {
                if (code.isBackground()) {
                    ansi = ansi.bg(code.getColor());
                }
                else {
                    ansi = ansi.fg(code.getColor());
                }
            }
            else if (code.isAttribute()) {
                ansi = ansi.a(code.getAttribute());
            }
        }

        return ansi.a(text).reset().toString();
    }

    public static boolean test(final String text) {
        return text != null && text.contains(BEGIN_TOKEN);
    }

    public static enum Code
    {
        //
        // TODO: Find a better way to keep Code in sync with Color/Attribute/Erase
        //

        // Colors
        BLACK(Ansi.Color.BLACK),
        RED(Ansi.Color.RED),
        GREEN(Ansi.Color.GREEN),
        YELLOW(Ansi.Color.YELLOW),
        BLUE(Ansi.Color.BLUE),
        MAGENTA(Ansi.Color.MAGENTA),
        CYAN(Ansi.Color.CYAN),
        WHITE(Ansi.Color.WHITE),

        // Foreground Ansi.Colors
        FG_BLACK(Ansi.Color.BLACK, false),
        FG_RED(Ansi.Color.RED, false),
        FG_GREEN(Ansi.Color.GREEN, false),
        FG_YELLOW(Ansi.Color.YELLOW, false),
        FG_BLUE(Ansi.Color.BLUE, false),
        FG_MAGENTA(Ansi.Color.MAGENTA, false),
        FG_CYAN(Ansi.Color.CYAN, false),
        FG_WHITE(Ansi.Color.WHITE, false),

        // Background Ansi.Colors
        BG_BLACK(Ansi.Color.BLACK, true),
        BG_RED(Ansi.Color.RED, true),
        BG_GREEN(Ansi.Color.GREEN, true),
        BG_YELLOW(Ansi.Color.YELLOW, true),
        BG_BLUE(Ansi.Color.BLUE, true),
        BG_MAGENTA(Ansi.Color.MAGENTA, true),
        BG_CYAN(Ansi.Color.CYAN, true),
        BG_WHITE(Ansi.Color.WHITE, true),

        // Attributes
        RESET(Ansi.Attribute.RESET),
        INTENSITY_BOLD(Ansi.Attribute.INTENSITY_BOLD),
        INTENSITY_FAINT(Ansi.Attribute.INTENSITY_FAINT),
        ITALIC(Ansi.Attribute.ITALIC),
        UNDERLINE(Ansi.Attribute.UNDERLINE),
        BLINK_SLOW(Ansi.Attribute.BLINK_SLOW),
        BLINK_FAST(Ansi.Attribute.BLINK_FAST),
        BLINK_OFF(Ansi.Attribute.BLINK_OFF),
        NEGATIVE_ON(Ansi.Attribute.NEGATIVE_ON),
        NEGATIVE_OFF(Ansi.Attribute.NEGATIVE_OFF),
        CONCEAL_ON(Ansi.Attribute.CONCEAL_ON),
        CONCEAL_OFF(Ansi.Attribute.CONCEAL_OFF),
        UNDERLINE_DOUBLE(Ansi.Attribute.UNDERLINE_DOUBLE),
        UNDERLINE_OFF(Ansi.Attribute.UNDERLINE_OFF),

        // Aliases
        BOLD(Ansi.Attribute.INTENSITY_BOLD),
        FAINT(Ansi.Attribute.INTENSITY_FAINT),;

        @SuppressWarnings("unchecked")
        private final Enum n;

        private final boolean background;

        @SuppressWarnings("unchecked")
        private Code(final Enum n, boolean background) {
            this.n = n;
            this.background = background;
        }

        @SuppressWarnings("unchecked")
        private Code(final Enum n) {
            this(n, false);
        }

        public boolean isColor() {
            return n instanceof au.com.dius.pact.provider.org.fusesource.jansi.Ansi.Color;
        }

        public au.com.dius.pact.provider.org.fusesource.jansi.Ansi.Color getColor() {
            return (Ansi.Color) n;
        }

        public boolean isAttribute() {
            return n instanceof Ansi.Attribute;
        }

        public Ansi.Attribute getAttribute() {
            return (Ansi.Attribute) n;
        }

        public boolean isBackground() {
            return background;
        }
    }
}
