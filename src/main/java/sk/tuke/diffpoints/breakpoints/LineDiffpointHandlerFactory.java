package sk.tuke.diffpoints.breakpoints;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaBreakpointHandler;
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory;

public class LineDiffpointHandlerFactory implements JavaBreakpointHandlerFactory {

    @Override
    public JavaBreakpointHandler createHandler(DebugProcessImpl process) {
        return new JavaBreakpointHandler(LineDiffpointBreakpointType.class, process);
    }

}
