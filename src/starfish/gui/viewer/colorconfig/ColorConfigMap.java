package starfish.gui.viewer.colorconfig;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

/**
 * Map of var name to its color map configuration, which is backed by local storage so configs are preserved every
 * time the program is run
 */
public class ColorConfigMap extends HashMap<String, ColorConfig> {

    private final static String STORAGE_KEY = "storage";
    private final static String STORAGE_DELIMITER = "|";

    // Only one instance should be made or else multiple instances may interfere with each other
    private static ColorConfigMap instance;
    public static ColorConfigMap getInstance() {
        if (instance == null) {
            instance = new ColorConfigMap();
        }
        return instance;
    }

    private Set<String> storedVars = new TreeSet<>();
    private String storage = "";

    // Private to avoid instantiation from outside
    private ColorConfigMap() {
        super();
        loadFromStorage();
    }
    private void loadFromStorage() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        storage = prefs.get(STORAGE_KEY, "");
        String[] storedItems = storage.split("\\|");
        for (int i = 0; i + 1 < storedItems.length; i += 2) {
            String var = storedItems[i];
            storedVars.add(var);
            String encodedConfig = storedItems[i + 1];
            ColorConfig config = parse(encodedConfig);
            put(var, config);
        }
    }

    /**
     * The config of {@code var} will persist between launches
     */
    public void saveToStorage(String var) {
        ColorConfig config = get(var);
        if (config != null) {
            String encoded = encode(config);
            if (!storedVars.contains(var)) {
                if (storedVars.size() > 0) {
                    storage += STORAGE_DELIMITER;
                }
                storage += var + STORAGE_DELIMITER + encoded;
                storedVars.add(var);
            } else {
                storage = encodeAllStoredVars();
            }
            Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
            prefs.put(STORAGE_KEY, storage);
        }
    }

    private String encodeAllStoredVars() {
        StringBuilder output = new StringBuilder();
        for (String var : storedVars) {
            output.append(var);
            output.append(STORAGE_DELIMITER);
            output.append(encode(get(var)));
            output.append(STORAGE_DELIMITER);
        }
        output.deleteCharAt(output.length() - 1);
        return output.toString();
    }

    private String encode(ColorConfig config) {
        return String.format("%s,%s,%s,%s,%s", config.getColorScheme(), config.getMin(),
                config.getMax(), config.getNumBuckets(), config.useLog());
    }
    private ColorConfig parse(String s) {
        String[] parts = s.split(",");
        return new ColorConfig.Builder()
                .colorScheme(ColorSchemePresets.valueOf(parts[0].replace(' ', '_')))
                .min(Double.parseDouble(parts[1]))
                .max(Double.parseDouble(parts[2]))
                .tableValues(Integer.parseInt(parts[3]))
                .useLog(Boolean.parseBoolean(parts[4]))
                .build();

    }

    public static void deleteAllSavedPresets() {
        Preferences prefs = Preferences.userRoot().node(ColorConfigMap.getInstance().getClass().getName());
        prefs.put(STORAGE_KEY, "");
    }

}
