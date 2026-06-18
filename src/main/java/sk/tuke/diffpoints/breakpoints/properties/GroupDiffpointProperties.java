package sk.tuke.diffpoints.breakpoints.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

public class GroupDiffpointProperties extends JavaLineBreakpointProperties {

    public String diffpointName = "";
    public String diffpointId = "";
    public String groupName = "Group 1";

    public boolean shouldPause = true;

    @Override
    public @Nullable JavaLineBreakpointProperties getState() {
        return this;
    }


    @Override
    public void loadState(@NotNull JavaLineBreakpointProperties state) {
        super.loadState(state);
        if (state instanceof GroupDiffpointProperties diffpointState) {
            this.diffpointName = diffpointState.diffpointName;
            this.diffpointId = diffpointState.diffpointId;
            this.groupName = diffpointState.groupName;
            this.shouldPause = diffpointState.shouldPause;
        }
    }
}
