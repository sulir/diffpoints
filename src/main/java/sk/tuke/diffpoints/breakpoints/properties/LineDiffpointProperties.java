package sk.tuke.diffpoints.breakpoints.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

public class LineDiffpointProperties extends JavaLineBreakpointProperties {

    public String diffpointName = "";
    public String diffpointId = "";

    public boolean shouldPause = true;

    public DiffpointSaveMode saveMode = DiffpointSaveMode.APPEND;

    public int circularLimit = 3;
    public String selectedIterations = "";

    @Override
    public LineDiffpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull JavaLineBreakpointProperties state) {
        super.loadState(state);
        if (state instanceof LineDiffpointProperties diffpointState) {
            this.diffpointName = diffpointState.diffpointName;
            this.diffpointId = diffpointState.diffpointId;
            this.shouldPause = diffpointState.shouldPause;
            this.saveMode = diffpointState.saveMode;
            this.circularLimit = diffpointState.circularLimit;
            this.selectedIterations = diffpointState.selectedIterations;
        }
    }
}