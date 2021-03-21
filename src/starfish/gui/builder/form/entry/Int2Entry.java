package starfish.gui.builder.form.entry;

import java.util.function.Supplier;

class Int2Entry extends TupleEntry<IntEntry> {

    private final static Supplier<IntEntry> intEntrySupplier = () -> new IntEntry("", "", 0);

    public Int2Entry(String tagName) {
        super(tagName, intEntrySupplier, 2);
    }

}
