package starfish.gui.preview;

import java.io.Serializable;

/**
 * Data class for storing {@code vtkLookupTable} settings
 */
public class ColorMapConfig implements Serializable, Cloneable {

    public static class Builder {

        private ColorScheme colorScheme = ColorScheme.values()[0];
        private double min = 0, max = 1;
        private int numBuckets = 64;
        private boolean useLog = false;

        public ColorMapConfig build() {
            return new ColorMapConfig(colorScheme, min, max, numBuckets, useLog);
        }

        public Builder colorScheme(ColorScheme colorScheme) {
            this.colorScheme = colorScheme;
            return this;
        }

        public Builder min(double min) {
            this.min = min;
            return this;
        }
        public Builder max(double max) {
            this.max = max;
            return this;
        }

        public Builder tableValues(int tableValues) {
            this.numBuckets = tableValues;
            return this;
        }

        public Builder useLog(boolean useLog) {
            this.useLog = useLog;
            return this;
        }

    }

    private ColorScheme colorScheme;
    private double min, max;
    private int numBuckets;
    private boolean useLog;

    // Private because it should be initialized through the builder
    private ColorMapConfig(ColorScheme colorScheme, double min, double max, int tableValues, boolean useLog) {
        this.colorScheme = colorScheme;
        this.min = min;
        this.max = max;
        this.numBuckets = tableValues;
        this.useLog = useLog;
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }
    public void setColorScheme(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    public double getMin() {
        return min;
    }
    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }
    public void setMax(double max) {
        this.max = max;
    }

    public int getNumBuckets() {
        return numBuckets;
    }
    public void setNumBuckets(int numBuckets) {
        this.numBuckets = numBuckets;
    }

    public boolean useLog() {
        return useLog;
    }
    public void setUseLog(boolean useLog) {
        this.useLog = useLog;
    }

}
