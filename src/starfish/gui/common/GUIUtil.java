package starfish.gui.common;

public final class GUIUtil {

    /**
     * Truncates an array of strings so they are only {@code maxLength} chars long
     * ex: {abcdefg, abcdefghijklm} -> {abcdefg, ...jklm}
     * @return
     */
    public static String[] truncateLeftEach(String[] strings, int maxLength) {
        String[] output = new String[strings.length];
        for (int i = 0; i < strings.length; i++) {
            output[i] = truncateLeft(strings[i], maxLength);
        }
        return output;
    }
    public static String truncateLeft(String s, int maxLength) {
        return s.length() > maxLength ? "..." + s.substring(s.length() - maxLength + 3) : s;
    }


}
