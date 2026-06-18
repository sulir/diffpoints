package sk.tuke.diffpoints.actions;

import sk.tuke.diffpoints.breakpoints.GroupDiffpointBreakpointType;

public final class ToggleGroupDiffpointAction extends AbstractToggleDiffpointAction<GroupDiffpointBreakpointType> {

    public ToggleGroupDiffpointAction() {
        super("Group Diffpoint", GroupDiffpointBreakpointType.class);
    }
}
