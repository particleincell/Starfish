package starfish.gui.viewer.colorconfig;

/**
 * Colors specifically to be used with ColorSchemePresets
 */
enum Colors {
    // Color Wheel
    BLACK(0, 0, 0, 1),
    WHITE(1, 1, 1, 1),
    RED(1, 0, 0, 1),
    GREEN(0, 1, 0, 1),
    BLUE(0, 0, 1, 1),
    YELLOW(1, 1, 0, 1),
    PURPLE(1, 0, 1, 1),
    LIGHT_BLUE(0, 1, 1, 1),

    // Other
    ORANGE(1, .5, 0, 1)
    ;
    private final double[] rgba;
    Colors(double r, double g, double b, double a) {
        this.rgba = new double[] {r, g, b, a};
    }
    public double[] arr() {
        return rgba;
    }
}