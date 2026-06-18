package sk.tuke.diffpoints.breakpoints.properties;

public enum DiffpointSaveMode {
    APPEND("Save all"),
    CIRCULAR("Save last N states"),
    COMPARE_FIRST("Compare first state"),
    SELECTED_ITERATIONS("Selected iterations");

    private final String label;

    DiffpointSaveMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}

