package starfish.gui.viewer.colorconfig;

import vtk.vtkColorSeries;
import vtk.vtkLookupTable;

import java.io.Serializable;

/**
 * Data class for storing {@code vtkLookupTable} settings
 */
public class ColorConfig implements Serializable, Cloneable {

    // I used the builder pattern so this class can easily be expanded if I want to add more customization options
    public static class Builder {

        private ColorSchemePresets colorScheme = ColorSchemePresets.values()[0];
        private double min = 0, max = 1;
        private int numBuckets = 64;
        private boolean useLog = false;

        public ColorConfig build() {
            return new ColorConfig(colorScheme, min, max, numBuckets, useLog);
        }

        public Builder colorScheme(ColorSchemePresets ColorSchemePresets) {
            this.colorScheme = ColorSchemePresets;
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

    private ColorSchemePresets colorScheme;
    private double min, max;
    private int numBuckets;
    private boolean useLog;

    // Private because it should be initialized through the builder
    private ColorConfig(ColorSchemePresets colorScheme, double min, double max, int tableValues, boolean useLog) {
        this.colorScheme = colorScheme;
        this.min = min;
        this.max = max;
        this.numBuckets = tableValues;
        this.useLog = useLog;
    }

    public vtkLookupTable buildLookupTable() {
        vtkLookupTable output = colorScheme.createLookupTable(numBuckets);
        output.SetRange(min, max);
        if (useLog) {
            output.SetScaleToLog10();
        }
        return output;
    }

    public ColorSchemePresets getColorScheme() {
        return colorScheme;
    }
    public void setColorScheme(ColorSchemePresets ColorSchemePresets) {
        this.colorScheme = ColorSchemePresets;
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

    @Override
    public String toString() {
        return String.format("Scheme=%s,min=%s,max=%s,buckets=%s,log=%s", colorScheme, min, max, numBuckets, useLog);
    }

}
