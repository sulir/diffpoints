package sk.tuke.diffpoints.actions;

import sk.tuke.diffpoints.breakpoints.LineDiffpointBreakpointType;

public final class ToggleLineDiffpointAction extends AbstractToggleDiffpointAction<LineDiffpointBreakpointType> {

    public ToggleLineDiffpointAction() {
        super("Line Diffpoint", LineDiffpointBreakpointType.class);
    }
}
