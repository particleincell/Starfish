package starfish.gui.viewer.colorconfig;

import vtk.vtkLookupTable;

/**
 *
 */
public enum ColorSchemePresets {

    // https://www.kennethmoreland.com/color-advice/

    COOL_TO_WARM(Colors.BLUE.arr(), Colors.WHITE.arr(), Colors.RED.arr()),
    RAINBOW(Colors.BLUE.arr(), Colors.LIGHT_BLUE.arr(), Colors.GREEN.arr(), Colors.YELLOW.arr(), Colors.RED.arr()),
    WARM(Colors.YELLOW.arr(), Colors.RED.arr()),
    COOL(Colors.LIGHT_BLUE.arr(), Colors.BLUE.arr()),
    BLACK_ORANGE_WHITE(Colors.BLACK.arr(), Colors.ORANGE.arr(), Colors.WHITE.arr()),
    PLASMA(Colors.BLACK.arr(), Colors.PURPLE.arr(), Colors.ORANGE.arr(), Colors.YELLOW.arr()),
    GRAYSCALE(Colors.BLACK.arr(), Colors.WHITE.arr()),
    KINDLMANN(Colors.BLACK.arr(), new double[]{.180,.015,.298,1}, new double[]{.247,.027,.568,1},
            new double[]{.031,.25,.63,1}, new double[]{.019,.41,.41,1}, new double[]{.027,.53,.27,1},
            new double[]{.0313,.65,.10,1}, new double[]{.32,.76,.035,1}, new double[]{.768,.807,.039,1},
            new double[]{.988,0.862,.772,1}, Colors.WHITE.arr()),
    BLACK_BLUE_WHITE(Colors.BLACK.arr(), Colors.BLUE.arr(), Colors.WHITE.arr());

    private final double[][] colors;

    ColorSchemePresets(double[]... colors) {
        this.colors = colors;
    }

    private vtkLookupTable cache;
    /**
     * @param tableValues number of tableValues in the series, positive integer.
     * @return color series of {@code tableValues} tableValues
     */
    public vtkLookupTable createLookupTable(int tableValues) {
        if (cache == null || cache.GetNumberOfColors() != tableValues) {
            cache = generateLookupTable(tableValues);
        }
        return cache;
    }
    private vtkLookupTable generateLookupTable(int tableValues) {
        double[][] arr = interpolate(tableValues, this.colors);
        vtkLookupTable output = new vtkLookupTable();
        output.SetNumberOfColors(tableValues);
        for (int i = 0; i < tableValues; i++) {
            output.SetTableValue(i, arr[i]);
        }
        return output;
    }

    /**
     * Linearly interpolate points on a line between points
     * @param points double[]s all of the same length
     * @return {@code count} double[]s of the same length as the input
     */
    private static double[][] interpolate(int count, double[]... points) {
        double[][] output = new double[count][];
        double pointRangeSize = 1.0 / points.length;
        double transitionRegionSize = 1.0 / (points.length - 1);
        for (int i = 0; i < count; i++) {
            double d = (double) i / count;
            double arrayD = d * points.length;
            int pointIndex = (int) (arrayD + .5);
            if (pointIndex < .5) {
                pointIndex += 1;
            } else if (pointIndex > points.length - .5) {
                pointIndex -= 1;
            }
            double secondTerm = (pointIndex - 1) * transitionRegionSize;
            double d2 = (d - secondTerm) * (points.length - 1);
            d2 = Math.max(0, d2);
            d2 = Math.min(1, d2);

            double[] point1 = points[pointIndex- 1];
            double[] point2 = points[pointIndex];
            double[] newEntry = new double[point1.length];
            for (int ii = 0; ii < point1.length; ii++) {
                double totalDelta = point2[ii] - point1[ii];
                newEntry[ii] = point1[ii] + totalDelta * d2;
            }
            output[i] = newEntry;
        }
        return output;
    }

    @Override
    public String toString() {
        return super.toString().replace('_', ' ');
    }

}
