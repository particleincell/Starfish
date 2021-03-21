package starfish.gui.builder.form.entry;

import java.util.function.Supplier;

class Float2Entry extends TupleEntry<FloatEntry> {

    private final static Supplier<FloatEntry> floatEntrySupplier = () -> new FloatEntry("", "", 0);

    public Float2Entry(String tagName) {
        super(tagName, floatEntrySupplier, 2);
    }

}
