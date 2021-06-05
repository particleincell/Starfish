package starfish.gui.common;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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

    private static Map<String, Font> originals;
    public static void setFontScale(float scale) {

        if (originals == null) {
            originals = new HashMap<>(25);
            for (Map.Entry entry : UIManager.getDefaults().entrySet()) {
                Object key = entry.getKey();
                if (key.toString().toLowerCase().contains(".font")) {
                    Object value = entry.getValue();
                    Font font = null;
                    if (value instanceof Font) {
                        font = (Font) value;
                        originals.put(key.toString(), font);
                    }
                }
            }
        }

        for (Map.Entry<String, Font> entry : originals.entrySet()) {
            String key = entry.getKey();
            Font font = entry.getValue();

            float size = font.getSize();
            size *= scale;

            font = font.deriveFont(Font.PLAIN, size);
            UIManager.put(key, font);
        }
    }

    /**
     * Wraps string as HTML.
     * Useful because it makes JLabels text wrap.
     */
    public static String htmlWrap(String s) {
        return String.format("<html>" +
                "<body style=\"text-align: left;\">" +
                "%s" +
                "</body>" +
                "</html>", s);
    }

    public static int calculateHeightOfAllChildren(JPanel panel) {
        Component[] children = panel.getComponents();
        int sum = 0;
        for (Component c : children) {
            sum += c.getHeight();
        }
        return sum;
    }


}
