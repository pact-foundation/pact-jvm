package au.com.dius.pact.consumer.dsl;

/**
 * Util class for dealing with Json format
 */
public final class QuoteUtil {

    private QuoteUtil() {
        super();
    }

    /**
     * Reads the input text with possible single quotes as delimiters
     * and returns a String correctly formatted.
     * <p>For convenience, single quotes as well as double quotes
     * are allowed to delimit strings. If single quotes are
     * used, any quotes, single or double, in the string must be
     * escaped (prepend with a '\').
     *
     * @param text the input data
     * @return String without single quotes
     */
    public static String convert(String text) {
        StringBuilder builder = new StringBuilder();
        boolean single_context = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\') {
                i = i + 1;
                if (i < text.length()) {
                    ch = text.charAt(i);
                    if (!(single_context && ch == '\'')) {
                        // unescape ' inside single quotes
                        builder.append('\\');
                    }
                }
            } else if (ch == '\'') {
                // Turn ' into ", for proper string
                ch = '"';
                single_context = ! single_context;
            }
            builder.append(ch);
        }

        return builder.toString();
    }

}
