package starfish.gui.preview;

/**
 * vtkColorSeries is supposed to have an inner enum called "ColorSchemes", which maps names to int codes for preset
 * color schemes, but it doesn't. This is used in place of it.
 */
public enum ColorScheme {
    /*
     * via https://vtk.org/doc/nightly/html/classvtkColorSeries.html#ad55759622bbe2e26f908696d0031edf8
     * Some of the colors are commented out. I removed them because 1. They're ugly or 2. It doesn't have a gradient,
     *  so it wasn't meant for coloring a spectrum/field/map/etc
     *
     * I copied the list from the website and put it into this python code:
     *
     * import re
     * def func(text):
     *     for i, name in enumerate(re.compile(',\\w*\n\\w*').split(text)):
     *         print(f'{name}({i}),\n')
     * func("""PASTE HERE""")
     */
    //SPECTRUM(0),
    WARM(1),
    COOL(2),
    BLUES(3),
    WILD_FLOWER(4),
    CITRUS(5),
    BREWER_DIVERGING_PURPLE_ORANGE_11(6),
    BREWER_DIVERGING_PURPLE_ORANGE_10(7),
    BREWER_DIVERGING_PURPLE_ORANGE_9(8),
    BREWER_DIVERGING_PURPLE_ORANGE_8(9),
    BREWER_DIVERGING_PURPLE_ORANGE_7(10),
    BREWER_DIVERGING_PURPLE_ORANGE_6(11),
    BREWER_DIVERGING_PURPLE_ORANGE_5(12),
    BREWER_DIVERGING_PURPLE_ORANGE_4(13),
    BREWER_DIVERGING_PURPLE_ORANGE_3(14),
    BREWER_DIVERGING_SPECTRAL_11(15),
    BREWER_DIVERGING_SPECTRAL_10(16),
    BREWER_DIVERGING_SPECTRAL_9(17),
    BREWER_DIVERGING_SPECTRAL_8(18),
    BREWER_DIVERGING_SPECTRAL_7(19),
    BREWER_DIVERGING_SPECTRAL_6(20),
    BREWER_DIVERGING_SPECTRAL_5(21),
    BREWER_DIVERGING_SPECTRAL_4(22),
    BREWER_DIVERGING_SPECTRAL_3(23),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_11(24),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_10(25),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_9(26),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_8(27),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_7(28),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_6(29),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_5(30),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_4(31),
    BREWER_DIVERGING_BROWN_BLUE_GREEN_3(32),
    BREWER_SEQUENTIAL_BLUE_GREEN_9(33),
    BREWER_SEQUENTIAL_BLUE_GREEN_8(34),
    BREWER_SEQUENTIAL_BLUE_GREEN_7(35),
    BREWER_SEQUENTIAL_BLUE_GREEN_6(36),
    BREWER_SEQUENTIAL_BLUE_GREEN_5(37),
    BREWER_SEQUENTIAL_BLUE_GREEN_4(38),
    BREWER_SEQUENTIAL_BLUE_GREEN_3(39),
    BREWER_SEQUENTIAL_YELLOW_ORANGE_BROWN_9(40),
    BREWER_SEQUENTIAL_YELLOW_ORANGE_BROWN_8(41),
    BREWER_SEQUENTIAL_YELLOW_ORANGE_BROWN_7(42),
    BREWER_SEQUENTIAL_YELLOW_ORANGE_BROWN_6(43),
    BREWER_SEQUENTIAL_YELLOW_ORANGE_BROWN_5(44),
    BREWER_SEQUENTIAL_YELLOW_ORANGE_BROWN_4(45),
    BREWER_SEQUENTIAL_YELLOW_ORANGE_BROWN_3(46),
    BREWER_SEQUENTIAL_BLUE_PURPLE_9(47),
    BREWER_SEQUENTIAL_BLUE_PURPLE_8(48),
    BREWER_SEQUENTIAL_BLUE_PURPLE_7(49),
    BREWER_SEQUENTIAL_BLUE_PURPLE_6(50),
    BREWER_SEQUENTIAL_BLUE_PURPLE_5(51),
    BREWER_SEQUENTIAL_BLUE_PURPLE_4(52),
    BREWER_SEQUENTIAL_BLUE_PURPLE_3(53);
    /*
    BREWER_QUALITATIVE_ACCENT(54),
    BREWER_QUALITATIVE_DARK2(55),
    BREWER_QUALITATIVE_SET2(56),
    BREWER_QUALITATIVE_PASTEL2(57),
    BREWER_QUALITATIVE_PASTEL1(58),
    BREWER_QUALITATIVE_SET1(59),
    BREWER_QUALITATIVE_PAIRED(60),
    BREWER_QUALITATIVE_SET3(61);*/

    private final int code;

    ColorScheme(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return super.toString().replace('_', ' ');
    }
}
